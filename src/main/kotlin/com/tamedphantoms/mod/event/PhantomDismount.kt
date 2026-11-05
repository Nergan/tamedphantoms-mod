package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.network.PhantomDismountAllowPayload
import com.tamedphantoms.mod.platform.ModNetwork
import com.tamedphantoms.mod.platform.storedData
import com.tamedphantoms.mod.util.PhantomDismountLogic
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.RelativeMovement
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import java.util.UUID

/**
 * Выше 8 блоков воздуха под фантомом слезание — два отдельных нажатия Shift
 * за секунду. Первое только предупреждает.
 *
 * Нажатие читает клиент с самой клавиши приседа и шлёт его на сервер.
 * К моменту слезания [Player.isShiftKeyDown] уже бывает ложью, поэтому
 * считать удары по этому флагу или по каждому вызову снятия нельзя.
 * Пока сервер не подтвердил второе нажатие, пассажира из списка не
 * выкидывают — только возвращают ссылку на фантома, которую ваниль
 * обнуляет до [Entity.removePassenger]. На клиенте это держится только
 * сразу после своего Shift: чужой пакет пассажиров нельзя отменять,
 * иначе седло на сервере и на клиенте расходится, и помогает только перезаход.
 */
object PhantomDismount {

    private const val SAFE_DROP = 8
    private const val CLIENT_ALLOW_NANOS = 250_000_000L
    private const val CLIENT_HOLD_NANOS = 1_000_000_000L
    private const val TAG_RESUME = "tamedphantoms.resume_ride"
    private const val RESUME_TICKS = 100
    private const val RESUME_HORIZONTAL = 32.0

    private class Ride(val phantomId: Int, val startedAt: Long)

    private val windows = HashMap<UUID, Ride>()
    private val armed = HashSet<UUID>()
    private val resumeUntil = HashMap<UUID, Int>()
    private val carried = HashMap<UUID, TamedPhantomEntity>()
    private val seatLockUntil = HashMap<UUID, Long>()
    private val decision = ThreadLocal<Boolean?>()
    private val bypassDepth = ThreadLocal.withInitial { 0 }
    private val travelDepth = ThreadLocal.withInitial { 0 }

    /** Фантом уже проходит портал. Повторный отзыв в этот момент создаёт вторую копию с тем же UUID. */
    private val portalCarry = ThreadLocal.withInitial { HashMap<UUID, Int>() }

    private val forceRide = run {
        val type = Entity::class.java
        val three = type.methods.firstOrNull { it.name == "startRiding" && it.parameterTypes.size == 3 }
        val two = type.methods.firstOrNull { it.name == "startRiding" && it.parameterTypes.size == 2 }
        three?.isAccessible = true
        two?.isAccessible = true
        when {
            three != null -> { rider: Entity, mount: Entity -> three.invoke(rider, mount, true, false) as Boolean }
            two != null -> { rider: Entity, mount: Entity -> two.invoke(rider, mount, true) as Boolean }
            else -> { rider: Entity, mount: Entity -> rider.startRiding(mount) }
        }
    }

    @Volatile
    private var clientAllowUntilNanos: Long = 0L

    @Volatile
    private var clientHoldUntilNanos: Long = 0L

    /** Выставляет клиент. На сервере остаётся «никто», чтобы не тянуть клиентские классы. */
    var isLocalPlayer: (Player) -> Boolean = { false }

    // Боевой Fabric 1.21.1 держит промежуточное имя, NeoForge и 26.3 — имя Mojang.
    private val vehicleField = listOf("vehicle", "field_6034").firstNotNullOfOrNull { name ->
        runCatching { Entity::class.java.getDeclaredField(name).apply { isAccessible = true } }.getOrNull()
    }

    fun bypassing(): Boolean = bypassDepth.get() > 0

    @JvmStatic
    fun traveling(): Boolean = travelDepth.get() > 0

    @JvmStatic
    fun beginTravel() {
        travelDepth.set(travelDepth.get() + 1)
    }

    @JvmStatic
    fun endTravel() {
        travelDepth.set((travelDepth.get() - 1).coerceAtLeast(0))
    }

    @JvmStatic
    fun beginPortalCarry(phantom: TamedPhantomEntity) {
        val map = portalCarry.get()
        val id = phantom.uuid
        map[id] = (map[id] ?: 0) + 1
        beginTravel()
    }

    @JvmStatic
    fun endPortalCarry(phantom: TamedPhantomEntity) {
        val map = portalCarry.get()
        val id = phantom.uuid
        val next = (map[id] ?: 1) - 1
        if (next <= 0) map.remove(id) else map[id] = next
        endTravel()
    }

    @JvmStatic
    fun carryingPortal(phantom: TamedPhantomEntity): Boolean = (portalCarry.get()[phantom.uuid] ?: 0) > 0

    /** Короткий замок после переноса: чужой пакет снятия не высаживает седока у выхода из портала. */
    @JvmStatic
    fun lockSeat(player: Player, ticks: Int = 30) {
        val until = player.tickCount.toLong() + ticks
        val previous = seatLockUntil[player.uuid] ?: Long.MIN_VALUE
        if (until > previous) seatLockUntil[player.uuid] = until
    }

    private fun seatLocked(player: Player): Boolean {
        val until = seatLockUntil[player.uuid] ?: return false
        if (player.tickCount.toLong() > until) {
            seatLockUntil.remove(player.uuid)
            return false
        }
        return true
    }

    /**
     * /tp седока переносит фантома. Координата с относительным флагом — смещение от фантома,
     * без флага — абсолютная. Седок из седла не выходит.
     */
    @JvmStatic
    fun carryCommand(
        player: ServerPlayer,
        phantom: TamedPhantomEntity,
        dest: ServerLevel,
        x: Double,
        y: Double,
        z: Double,
        relatives: Set<RelativeMovement>,
        yRot: Float,
        xRot: Float,
    ): Boolean {
        val riders = phantom.passengers.toList()
        for (rider in riders) {
            if (rider is Player) this.lockSeat(rider)
        }
        val xAbs = if (RelativeMovement.X in relatives) phantom.x + x else x
        val yAbs = if (RelativeMovement.Y in relatives) phantom.y + y else y
        val zAbs = if (RelativeMovement.Z in relatives) phantom.z + z else z
        val yaw = if (RelativeMovement.Y_ROT in relatives) phantom.yRot + yRot else yRot
        val pitch = if (RelativeMovement.X_ROT in relatives) phantom.xRot + xRot else xRot
        val moved = phantom.teleportTo(dest, xAbs, yAbs, zAbs, emptySet<RelativeMovement>(), yaw, pitch)
        val live = this.resolveCarried(player, phantom)
        if (live != null) this.reseat(riders, live)
        return moved || (live != null && live.isAlive)
    }

    /** После смены измерения ваниль иногда не успевает посадить седока обратно. */
    @JvmStatic
    fun reseat(riders: List<Entity>, phantom: TamedPhantomEntity) {
        if (!phantom.isAlive || phantom.isRemoved) return
        for (rider in riders) {
            if (!rider.isAlive || rider.isRemoved) continue
            if (rider.vehicle === phantom && phantom.passengers.contains(rider)) {
                if (rider is ServerPlayer) this.resync(rider, phantom)
                continue
            }
            this.bypass {
                if (rider.vehicle != null) rider.stopRiding()
                this.forceRide(rider, phantom)
            }
            if (rider is Player) this.lockSeat(rider)
            if (rider is ServerPlayer && rider.vehicle === phantom) this.resync(rider, phantom)
        }
    }

    fun grantClientDismount() {
        clientAllowUntilNanos = System.nanoTime() + CLIENT_ALLOW_NANOS
    }

    /** Клиент только что сам нажал Shift. Чужие снятия в эту секунду не держим. */
    fun noteLocalDismountRequest() {
        clientHoldUntilNanos = System.nanoTime() + CLIENT_HOLD_NANOS
    }

    private fun clientDismountAllowed(): Boolean = System.nanoTime() < clientAllowUntilNanos

    private fun clientHoldingDismount(): Boolean = System.nanoTime() < clientHoldUntilNanos

    /**
     * @return true, если это слезание нужно отменить и оставить игрока в седле.
     */
    fun shouldKeepMounted(player: Player, phantom: TamedPhantomEntity): Boolean {
        if (bypassDepth.get() > 0) return false
        decision.get()?.let { return it }
        val block = this.compute(player, phantom)
        decision.set(block)
        return block
    }

    fun finish() {
        decision.remove()
    }

    /** Фронт клавиши Shift, присланный клиентом. */
    fun onTap(player: ServerPlayer) {
        if (player.isSpectator) return
        val phantom = this.riddenPhantom(player) ?: return
        if (!phantom.isAlive) return
        val now = player.level().gameTime
        if (this.airBelow(phantom) <= SAFE_DROP) {
            windows.remove(player.uuid)
            return
        }
        val existing = windows[player.uuid]
        val samePhantom = existing != null && existing.phantomId == phantom.id
        val step = PhantomDismountLogic.onTap(now, if (samePhantom) existing?.startedAt else null)
        if (step.confirm) {
            windows.remove(player.uuid)
            armed.add(player.uuid)
            if (player.vehicle !== phantom && phantom.passengers.contains(player)) {
                this.link(player, phantom)
            }
            if (player.vehicle === phantom) {
                player.stopRiding()
            }
            return
        }
        val start = step.windowStart
        if (start != null) {
            windows[player.uuid] = Ride(phantom.id, start)
        }
        if (step.warn) {
            player.displayClientMessage(
                Component.translatable("tamedphantoms.dismount.high").withStyle(ChatFormatting.YELLOW),
                true,
            )
        }
    }

    /**
     * /tp и портал сначала снимают седока. Высоко в воздухе это снятие
     * отменялось, игрок уезжал один, а клиент оставался пассажиром пропавшего
     * фантома и падал сквозь блоки. Здесь снятие настоящее, а после телепорта
     * фантом оказывается в той же точке и седло возвращается.
     */
    @JvmStatic
    fun dismountForTeleport(player: ServerPlayer) {
        val phantom = player.vehicle as? TamedPhantomEntity
        if (phantom != null && phantom.isAlive && !phantom.isRemoved) {
            carried[player.uuid] = phantom
            this.allowClient(player)
        }
        this.bypass {
            player.stopRiding()
        }
    }

    @JvmStatic
    fun finishTeleport(player: ServerPlayer) {
        val level = player.level() as? ServerLevel ?: return
        this.finishTeleportAt(player, level, player.x, player.y, player.z)
    }

    /**
     * После телепорта седока фантом оказывается в той же точке.
     * Смена измерения подменяет сущность, поэтому седло возвращается на живую копию с тем же UUID.
     */
    @JvmStatic
    fun finishTeleportAt(player: ServerPlayer, dest: ServerLevel, x: Double, y: Double, z: Double) {
        val captured = carried.remove(player.uuid) ?: return
        var phantom = this.resolveCarried(player, captured) ?: return
        if (!player.isAlive || player.isRemoved) return
        val original = phantom
        val moved = phantom.teleportTo(dest, x, y, z, emptySet<RelativeMovement>(), phantom.yRot, phantom.xRot)
        if (!phantom.isAlive || phantom.isRemoved) {
            phantom = this.resolveCarried(player, captured) ?: return
        }
        if (!moved && phantom === original) return
        phantom.fallDistance = 0.0f
        player.fallDistance = 0.0f
        phantom.deltaMovement = Vec3.ZERO
        phantom.setPortalCooldown()
        player.setPortalCooldown()
        this.bypass {
            if (player.vehicle != null && player.vehicle !== phantom) player.stopRiding()
            if (player.vehicle !== phantom) player.startRiding(phantom, true)
        }
        this.resync(player, phantom)
    }

    /** Портал иногда подменяет сущность. Та же UUID в любом измерении — всё ещё этот фантом. */
    private fun resolveCarried(player: ServerPlayer, captured: TamedPhantomEntity): TamedPhantomEntity? {
        if (captured.isAlive && !captured.isRemoved) return captured
        val owner = player
        val server = owner.server ?: return null
        for (level in server.allLevels) {
            val found = level.getEntity(captured.uuid) as? TamedPhantomEntity ?: continue
            if (found.isAlive && !found.isRemoved) return found
        }
        return null
    }

    /** Перед настоящим снятием: клиент не должен тут же посадить игрока обратно. */
    fun allowClient(player: ServerPlayer) {
        this.grantClientDismount()
        ModNetwork.sendToPlayer(player, PhantomDismountAllowPayload)
    }

    /** Вернуть ссылку на фантома, не выбрасывая игрока из списка пассажиров. */
    fun restore(player: Player, phantom: TamedPhantomEntity) {
        if (player.vehicle === phantom && phantom.passengers.contains(player)) return
        if (phantom.passengers.contains(player) && this.link(player, phantom)) return
        bypassDepth.set(bypassDepth.get() + 1)
        try {
            if (phantom.passengers.contains(player)) {
                phantom.detachPassengerNow(player)
            } else if (player.vehicle === phantom) {
                this.unlink(player)
            }
            if (player.vehicle !== phantom) {
                player.startRiding(phantom, true)
            }
        } finally {
            bypassDepth.set(bypassDepth.get() - 1)
        }
    }

    fun onPlayerTick(player: Player) {
        this.heal(player)
        val serverPlayer = player as? ServerPlayer ?: return
        val until = resumeUntil[serverPlayer.uuid]
        if (until != null) {
            val done = this.resumeCarried(serverPlayer)
            if (done || serverPlayer.tickCount > until) {
                resumeUntil.remove(serverPlayer.uuid)
            }
        }
        val ride = windows[serverPlayer.uuid] ?: return
        val now = serverPlayer.level().gameTime
        if (now - ride.startedAt > PhantomDismountLogic.WINDOW_TICKS) {
            windows.remove(serverPlayer.uuid)
            armed.remove(serverPlayer.uuid)
            return
        }
        if (serverPlayer.uuid in armed) return
        val phantom = serverPlayer.serverLevel().getEntity(ride.phantomId) as? TamedPhantomEntity ?: return
        if (!phantom.isAlive || phantom.isOrderedToSit() || this.airBelow(phantom) <= SAFE_DROP) {
            windows.remove(serverPlayer.uuid)
            return
        }
        if (serverPlayer.vehicle === phantom && phantom.passengers.contains(serverPlayer)) return
        if (phantom.passengers.contains(serverPlayer)) {
            this.restore(serverPlayer, phantom)
            return
        }
        if (serverPlayer.vehicle === phantom) {
            this.unlink(serverPlayer)
        }
        if (serverPlayer.vehicle == null) {
            serverPlayer.startRiding(phantom, true)
        }
    }

    @JvmStatic
    fun onLogout(player: Player) {
        val ridden = player.vehicle as? TamedPhantomEntity
        if (ridden != null && ridden.isOwnedBy(player)) {
            this.protectRiders(ridden)
            // Ваниль забирает ездовое с игроком, только если на нём остался один игрок.
            // Пассажира снимаем заранее, иначе фантом останется в мире вместе с ним.
            this.releaseOtherRiders(ridden, player)
        }
        val id = player.uuid
        val task = Runnable {
            windows.remove(id)
            armed.remove(id)
            resumeUntil.remove(id)
            carried.remove(id)
            seatLockUntil.remove(id)
        }
        val server = (player as? ServerPlayer)?.serverLevel()?.getServer()
        if (server == null || server.isSameThread) task.run() else server.execute(task)
    }

    /**
     * Седока нельзя снимать до записи игрока. Пока он в седле, ваниль прячет
     * фантома в данные игрока и убирает его из мира. На входе оба появляются
     * снова, игрок сидит на том же фантоме. Снятие до записи оставляет фантома
     * в небе, а игрока кладёт на землю под ним.
     */
    fun resumeAfterLogin(player: Player) {
        val serverPlayer = player as? ServerPlayer ?: return
        resumeUntil[serverPlayer.uuid] = serverPlayer.tickCount + RESUME_TICKS
        this.resumeCarried(serverPlayer)
        val phantom = serverPlayer.vehicle as? TamedPhantomEntity ?: return
        this.resync(serverPlayer, phantom)
    }

    /**
     * Старые выходы успели записать метку и оставить фантома в мире.
     * Пока метка есть, сажаем на того же, если он рядом по горизонтали.
     * Новые выходы метку не пишут: фантома забирает ваниль вместе с игроком.
     */
    private fun resumeCarried(player: ServerPlayer): Boolean {
        val data = player.storedData
        if (!data.contains(TAG_RESUME)) return true
        val rideId = data.getString(TAG_RESUME)
        val id = runCatching { UUID.fromString(rideId) }.getOrNull()
        if (id == null) {
            data.remove(TAG_RESUME)
            return true
        }
        val riding = player.vehicle as? TamedPhantomEntity
        if (riding != null && riding.uuid == id) {
            data.remove(TAG_RESUME)
            this.resync(player, riding)
            return true
        }
        val phantom = this.findLoaded(player, id) ?: return false
        if (!phantom.isAlive || !phantom.tamed || !phantom.isSaddled || phantom.isOrderedToSit() || !phantom.isOwnedBy(player)) {
            return false
        }
        val dx = player.x - phantom.x
        val dz = player.z - phantom.z
        if (dx * dx + dz * dz > RESUME_HORIZONTAL * RESUME_HORIZONTAL) return false
        this.releaseStale(phantom)
        if (player.vehicle !== phantom) {
            player.startRiding(phantom, true)
        }
        if (player.vehicle !== phantom) return false
        data.remove(TAG_RESUME)
        player.fallDistance = 0.0f
        phantom.fallDistance = 0.0f
        this.resync(player, phantom)
        return true
    }

    /** Повторно шлёт список седоков. Клиент, который сидит «не до конца», садится заново. */
    fun resync(player: ServerPlayer, phantom: TamedPhantomEntity) {
        this.releaseStale(phantom)
        player.connection.send(ClientboundSetPassengersPacket(phantom))
    }

    /** Убирает из седла снятых и мёртвых, чтобы место не оставалось занятым навсегда. */
    fun releaseStale(phantom: TamedPhantomEntity) {
        val keep = HashMap<UUID, Entity>()
        val stale = ArrayList<Entity>()
        for (entity in phantom.passengers) {
            if (entity.isRemoved || !entity.isAlive || entity.vehicle !== phantom) {
                stale.add(entity)
                continue
            }
            val previous = keep.put(entity.uuid, entity)
            if (previous != null) {
                val dropPrevious = previous !is Player || !isLocalPlayer(previous)
                if (dropPrevious) {
                    stale.add(previous)
                } else {
                    keep[entity.uuid] = previous
                    stale.add(entity)
                }
            }
        }
        if (stale.isEmpty()) return
        this.bypass {
            for (entity in stale) {
                phantom.detachPassengerNow(entity)
            }
        }
    }

    /**
     * Седок есть только с одной стороны ссылки: клиент не шлёт управление,
     * а сервер считает место занятым. Собирает ссылку обратно.
     */
    fun heal(player: Player) {
        if (!player.isAlive || player.isRemoved) return
        val phantom = player.vehicle as? TamedPhantomEntity ?: return
        if (!phantom.isAlive || phantom.isRemoved) {
            this.forceDismount(player, phantom)
            return
        }
        this.releaseStale(phantom)
        if (phantom.passengers.contains(player)) {
            val pilotOk = phantom.controllingPassenger === player || !phantom.isOwnedBy(player)
            if (!player.level().isClientSide || pilotOk) return
        }
        if (player.level().isClientSide) {
            this.bypass {
                this.unlink(player)
                player.startRiding(phantom, true)
            }
            return
        }
        this.forceRemount(player, phantom)
    }

    private fun findLoaded(player: ServerPlayer, id: UUID): TamedPhantomEntity? {
        val server = player.serverLevel().getServer() ?: return null
        for (level in server.allLevels) {
            val found = level.getEntity(id) as? TamedPhantomEntity
            if (found != null) return found
        }
        return null
    }

    private fun forceDismount(player: Player, phantom: TamedPhantomEntity) {
        this.bypass {
            if (player.vehicle === phantom || phantom.passengers.contains(player)) {
                player.stopRiding()
            }
        }
    }

    private fun forceRemount(player: Player, phantom: TamedPhantomEntity) {
        this.bypass {
            if (player.vehicle != null) player.stopRiding()
            if (phantom.passengers.contains(player)) phantom.detachPassengerNow(player)
            player.startRiding(phantom, true)
        }
    }

    /** Пять минут медленного падения и огнестойкости пассажиру, если он есть. Хозяин эффекты не получает. */
    fun protectRiders(phantom: TamedPhantomEntity) {
        val ticks = 5 * 60 * 20
        for (passenger in phantom.passengers.toList()) {
            val living = passenger as? LivingEntity ?: continue
            if (!living.isAlive || living.isRemoved) continue
            if (phantom.isOwnedBy(living)) continue
            living.addEffect(MobEffectInstance(MobEffects.SLOW_FALLING, ticks, 0, false, true, true))
            living.addEffect(MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks, 0, false, true, true))
        }
    }

    private fun releaseOtherRiders(phantom: TamedPhantomEntity, owner: Player) {
        val others = phantom.passengers.filter { it !== owner }
        if (others.isEmpty()) return
        this.bypass {
            for (rider in others) {
                rider.fallDistance = 0.0f
                if (rider.vehicle === phantom || phantom.passengers.contains(rider)) {
                    rider.stopRiding()
                }
            }
        }
    }

    internal fun bypassNow(block: () -> Unit) = bypass(block)

    private inline fun bypass(block: () -> Unit) {
        bypassDepth.set(bypassDepth.get() + 1)
        try {
            block()
        } finally {
            bypassDepth.set((bypassDepth.get() - 1).coerceAtLeast(0))
        }
    }

    private fun compute(player: Player, phantom: TamedPhantomEntity): Boolean {
        if (!player.isAlive || !phantom.isAlive || player.isRemoved || phantom.isRemoved) return false
        if (player is ServerPlayer && player.hasDisconnected()) return false
        if (this.seatLocked(player)) return true
        if (phantom.isOrderedToSit()) return false
        if (player.level().isClientSide) {
            if (!isLocalPlayer(player)) return false
            if (this.clientDismountAllowed()) return false
            if (!this.clientHoldingDismount()) return false
            return this.airBelow(phantom) > SAFE_DROP
        }
        if (this.airBelow(phantom) <= SAFE_DROP) {
            windows.remove(player.uuid)
            return false
        }
        if (armed.remove(player.uuid)) {
            windows.remove(player.uuid)
            return false
        }
        return true
    }

    private fun riddenPhantom(player: ServerPlayer): TamedPhantomEntity? {
        val direct = player.vehicle as? TamedPhantomEntity
        if (direct != null) return direct
        val known = windows[player.uuid]?.phantomId ?: return null
        val phantom = player.serverLevel().getEntity(known) as? TamedPhantomEntity ?: return null
        if (phantom.passengers.contains(player)) return phantom
        return null
    }

    private fun link(player: Player, phantom: TamedPhantomEntity): Boolean {
        val field = vehicleField ?: return false
        return try {
            field.set(player, phantom)
            player.vehicle === phantom
        } catch (_: Throwable) {
            false
        }
    }

    private fun unlink(player: Player) {
        val field = vehicleField ?: return
        try {
            field.set(player, null)
        } catch (_: Throwable) {
        }
    }

    /** Сколько пустых блоков под фантомом, пока не встретится опора. Больше [SAFE_DROP] — высоко. */
    private fun airBelow(phantom: TamedPhantomEntity): Int {
        val level = phantom.level()
        val x = Mth.floor(phantom.x)
        val z = Mth.floor(phantom.z)
        var y = Mth.floor(phantom.y) - 1
        var air = 0
        repeat(SAFE_DROP + 1) {
            if (y < level.minBuildHeight) return air
            val pos = BlockPos(x, y, z)
            val state = level.getBlockState(pos)
            val ground = !state.getCollisionShape(level, pos).isEmpty || !state.fluidState.isEmpty
            if (ground) return air
            air++
            y--
        }
        return air
    }
}
