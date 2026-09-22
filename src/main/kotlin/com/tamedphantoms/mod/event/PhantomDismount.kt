package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.PhantomDismountLogic
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.EntityMountEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import java.util.UUID

/**
 * Выше 8 блоков воздуха под фантомом слезание — два отдельных нажатия Shift
 * в течение секунды. Первое только предупреждает.
 *
 * Ваниль зовёт снятие каждый тик, пока Shift зажат, и к этому моменту
 * [Player.isShiftKeyDown] уже бывает ложью. Поэтому второе нажатие — это
 * новая попытка после паузы, а не «клавиша сейчас нажата».
 */
object PhantomDismount {

    private const val SAFE_DROP = 8

    private data class Key(val id: UUID, val client: Boolean)

    private class Decision(val tick: Long, val block: Boolean)

    private class Ride(val phantomId: Int, var window: PhantomDismountLogic.Window)

    private val pending = HashMap<Key, Ride>()
    private val decided = HashMap<Key, Decision>()
    private val restoring = HashSet<Key>()

    private val vehicleField = try {
        Entity::class.java.getDeclaredField("vehicle").apply { isAccessible = true }
    } catch (_: Throwable) {
        null
    }

    /**
     * @return true, если это слезание нужно отменить и оставить игрока в седле.
     */
    fun shouldKeepMounted(player: Player, phantom: TamedPhantomEntity): Boolean {
        val id = key(player)
        if (id in restoring || !player.isAlive || !phantom.isAlive) return false
        val now = player.level().gameTime
        val previous = decided[id]
        if (previous != null && previous.tick == now) return previous.block
        val step = PhantomDismountLogic.onAttempt(
            high = airBelow(phantom) > SAFE_DROP,
            now = now,
            window = pending[id]?.window,
        )
        if (step.window == null) {
            pending.remove(id)
        } else {
            pending[id] = Ride(phantom.id, step.window)
        }
        if (step.warn && player is ServerPlayer) {
            player.displayClientMessage(
                Component.translatable("tamedphantoms.dismount.high").withStyle(ChatFormatting.YELLOW),
                true,
            )
        }
        decided[id] = Decision(now, step.keepMounted)
        return step.keepMounted
    }

    /** Вернуть ссылку на фантома, если снятие уже успело её оборвать. */
    fun restore(player: Player, phantom: TamedPhantomEntity) {
        if (player.vehicle === phantom && phantom.passengers.contains(player)) return
        if (phantom.passengers.contains(player)) {
            val field = vehicleField
            if (field != null) {
                field.set(player, phantom)
                if (player.vehicle === phantom) return
            }
        }
        val id = key(player)
        if (!restoring.add(id)) return
        try {
            if (player.vehicle != null) player.stopRiding()
            if (player.vehicle !== phantom) player.startRiding(phantom, true)
        } finally {
            restoring.remove(id)
        }
    }

    @SubscribeEvent
    fun onMount(event: EntityMountEvent) {
        if (event.isMounting) return
        val player = event.entityMounting as? Player ?: return
        val phantom = event.entityBeingMounted as? TamedPhantomEntity ?: return
        if (!shouldKeepMounted(player, phantom)) return
        event.isCanceled = true
        restore(player, phantom)
    }

    @SubscribeEvent
    fun onTick(event: PlayerTickEvent.Post) {
        val player = event.entity
        val id = key(player)
        val ride = pending[id] ?: return
        val now = player.level().gameTime
        if (now - ride.window.startedAt > PhantomDismountLogic.WINDOW_TICKS &&
            now - ride.window.lastAttempt >= PhantomDismountLogic.GAP_TICKS
        ) {
            pending.remove(id)
            return
        }
        if (player.vehicle != null || now - ride.window.startedAt > PhantomDismountLogic.WINDOW_TICKS) return
        val level = player.level() as? ServerLevel ?: return
        val phantom = level.getEntity(ride.phantomId) as? TamedPhantomEntity ?: return
        if (!phantom.isAlive || airBelow(phantom) <= SAFE_DROP) return
        player.startRiding(phantom, true)
    }

    @SubscribeEvent
    fun onLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        val id = event.entity.uuid
        pending.keys.removeIf { it.id == id }
        decided.keys.removeIf { it.id == id }
        restoring.removeIf { it.id == id }
    }

    private fun key(player: Player) = Key(player.uuid, player.level().isClientSide)

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
