package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.PhantomDismountLogic
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import java.util.UUID

/**
 * Выше 8 блоков воздуха под фантомом слезание — это два нажатия Shift
 * в течение секунды. Первое только предупреждает. Удержание не считается
 * вторым нажатием, и окно не тянется дольше секунды от первого нажатия.
 */
object PhantomDismount {

    private const val SAFE_DROP = 8

    private data class Key(val id: UUID, val client: Boolean)

    private class Ride(val phantomId: Int, var tap: PhantomDismountLogic.Tap)

    private val pending = HashMap<Key, Ride>()
    private val shiftDown = HashMap<Key, Boolean>()
    private val restoring = HashSet<Key>()

    /**
     * @return true, если это слезание нужно отменить и сразу посадить игрока обратно.
     */
    fun shouldKeepMounted(player: Player, phantom: TamedPhantomEntity): Boolean {
        val id = key(player)
        if (id in restoring || !player.isAlive || !phantom.isAlive) return false
        val now = player.level().gameTime
        val down = player.isShiftKeyDown()
        val rising = down && shiftDown[id] != true
        if (down) shiftDown[id] = true
        val step = PhantomDismountLogic.onAttempt(
            high = airBelow(phantom) > SAFE_DROP,
            shiftDown = down,
            rising = rising,
            tap = pending[id]?.tap,
            now = now,
        )
        if (step.tap == null) {
            pending.remove(id)
        } else {
            pending[id] = Ride(phantom.id, step.tap)
        }
        if (step.warn && player is ServerPlayer) {
            player.displayClientMessage(
                Component.translatable("tamedphantoms.dismount.high").withStyle(ChatFormatting.YELLOW),
                true,
            )
        }
        return step.keepMounted
    }

    /** Снова сажает игрока. Повторный заход из [shouldKeepMounted] не зацикливается. */
    fun restore(player: Player, phantom: TamedPhantomEntity) {
        val id = key(player)
        if (!restoring.add(id)) return
        try {
            if (player.vehicle != null) {
                player.stopRiding()
            }
            if (player.vehicle !== phantom) {
                player.startRiding(phantom, true)
            }
        } finally {
            restoring.remove(id)
        }
    }

    @SubscribeEvent
    fun onTick(event: PlayerTickEvent.Post) {
        val player = event.entity
        val id = key(player)
        val down = player.isShiftKeyDown()
        if (!down) shiftDown[id] = false
        val ride = pending[id] ?: return
        val now = player.level().gameTime
        val next = PhantomDismountLogic.afterShift(ride.tap, down, now)
        if (next == null) {
            pending.remove(id)
            return
        }
        ride.tap = next
        if (next.confirmed || player.vehicle != null) return
        val level = player.level() as? ServerLevel ?: return
        val phantom = level.getEntity(ride.phantomId) as? TamedPhantomEntity ?: return
        if (!phantom.isAlive || airBelow(phantom) <= SAFE_DROP) return
        player.startRiding(phantom, true)
    }

    @SubscribeEvent
    fun onLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        val id = event.entity.uuid
        pending.keys.removeIf { it.id == id }
        shiftDown.keys.removeIf { it.id == id }
        restoring.removeIf { it.id == id }
    }

    private fun key(player: Player) = Key(player.uuid, player.level().isClientSide)

    /** Сколько пустых блоков под ногами, пока не встретится опора. Больше [SAFE_DROP] — высоко. */
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
