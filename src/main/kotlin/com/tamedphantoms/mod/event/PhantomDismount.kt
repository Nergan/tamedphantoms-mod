package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.network.PhantomDismountAllowPayload
import com.tamedphantoms.mod.util.PhantomDismountLogic
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import net.neoforged.neoforge.network.PacketDistributor
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
 * обнуляет до [Entity.removePassenger].
 */
object PhantomDismount {

    private const val SAFE_DROP = 8
    private const val CLIENT_ALLOW_NANOS = 250_000_000L

    private class Ride(val phantomId: Int, val startedAt: Long)

    private val windows = HashMap<UUID, Ride>()
    private val armed = HashSet<UUID>()
    private val decision = ThreadLocal<Boolean?>()
    private val bypassDepth = ThreadLocal.withInitial { 0 }

    @Volatile
    private var clientAllowUntilNanos: Long = 0L

    /** Выставляет клиент. На сервере остаётся «никто», чтобы не тянуть клиентские классы. */
    var isLocalPlayer: (Player) -> Boolean = { false }

    private val vehicleField = try {
        Entity::class.java.getDeclaredField("vehicle").apply { isAccessible = true }
    } catch (_: Throwable) {
        null
    }

    fun bypassing(): Boolean = bypassDepth.get() > 0

    fun grantClientDismount() {
        clientAllowUntilNanos = System.nanoTime() + CLIENT_ALLOW_NANOS
    }

    private fun clientDismountAllowed(): Boolean = System.nanoTime() < clientAllowUntilNanos

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
        PacketDistributor.sendToPlayer(player, PhantomDismountAllowPayload)
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

    @SubscribeEvent
    fun onTick(event: PlayerTickEvent.Post) {
        val player = event.entity as? ServerPlayer ?: return
        val ride = windows[player.uuid] ?: return
        val now = player.level().gameTime
        if (now - ride.startedAt > PhantomDismountLogic.WINDOW_TICKS) {
            windows.remove(player.uuid)
            armed.remove(player.uuid)
            return
        }
        if (player.uuid in armed) return
        val phantom = player.serverLevel().getEntity(ride.phantomId) as? TamedPhantomEntity ?: return
        if (!phantom.isAlive || phantom.isOrderedToSit() || this.airBelow(phantom) <= SAFE_DROP) {
            windows.remove(player.uuid)
            return
        }
        if (player.vehicle === phantom && phantom.passengers.contains(player)) return
        if (phantom.passengers.contains(player)) {
            this.restore(player, phantom)
            return
        }
        if (player.vehicle === phantom) {
            this.unlink(player)
        }
        if (player.vehicle == null) {
            player.startRiding(phantom, true)
        }
    }

    @SubscribeEvent
    fun onLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        val id = event.entity.uuid
        windows.remove(id)
        armed.remove(id)
    }

    private fun compute(player: Player, phantom: TamedPhantomEntity): Boolean {
        if (!player.isAlive || !phantom.isAlive || player.isRemoved || phantom.isRemoved) return false
        if (phantom.isOrderedToSit()) return false
        if (player.level().isClientSide) {
            if (!isLocalPlayer(player)) return false
            if (this.clientDismountAllowed()) return false
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
