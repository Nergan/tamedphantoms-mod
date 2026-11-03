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
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
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
    private val decision = ThreadLocal<Boolean?>()
    private val bypassDepth = ThreadLocal.withInitial { 0 }

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

    fun onLogout(player: Player) {
        val id = player.uuid
        val task = Runnable {
            windows.remove(id)
            armed.remove(id)
            resumeUntil.remove(id)
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
