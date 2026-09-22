package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import java.util.UUID

/**
 * С высоты больше 8 блоков до поверхности слезание требует второго Shift
 * в течение секунды. Первое нажатие только предупреждает.
 *
 * Отмена [net.neoforged.neoforge.event.entity.EntityMountEvent] не удерживает
 * игрока: ванильный спешивающий код уже обнуляет ссылку на фантома.
 * Поэтому первое нажатие сразу сажает игрока обратно.
 */
object PhantomDismount {

    private const val CONFIRM_TICKS = 20
    private const val SAFE_DROP = 8

    private data class Key(val id: UUID, val client: Boolean)

    private class Attempt(val armedAt: Long, var released: Boolean)

    private val pending = HashMap<Key, Attempt>()
    private val allowedTick = HashMap<Key, Long>()
    private val restoring = HashSet<Key>()

    /**
     * @return true, если это слезание нужно отменить и сразу посадить игрока обратно.
     */
    fun shouldKeepMounted(player: Player, phantom: TamedPhantomEntity): Boolean {
        if (key(player) in restoring) return false
        if (!player.isAlive || !phantom.isAlive || !player.isShiftKeyDown()) return false
        val id = key(player)
        val now = player.level().gameTime
        if (allowedTick[id] == now) return false
        if (!higherThanSurface(player, SAFE_DROP)) {
            pending.remove(id)
            return false
        }
        val state = pending[id]
        if (state != null && state.released && now - state.armedAt <= CONFIRM_TICKS) {
            allowedTick[id] = now
            pending.remove(id)
            return false
        }
        if (state == null || state.released) {
            pending[id] = Attempt(now, released = false)
            if (player is ServerPlayer) {
                player.displayClientMessage(
                    Component.translatable("tamedphantoms.dismount.high").withStyle(ChatFormatting.YELLOW),
                    true,
                )
            }
        }
        return true
    }

    /** Снова сажает игрока. Повторный заход из [shouldKeepMounted] не зацикливается. */
    fun restore(player: Player, phantom: TamedPhantomEntity) {
        val id = key(player)
        if (!restoring.add(id)) return
        try {
            if (player.vehicle != null) {
                player.stopRiding()
            }
            player.startRiding(phantom, true)
        } finally {
            restoring.remove(id)
        }
    }

    @SubscribeEvent
    fun onTick(event: PlayerTickEvent.Post) {
        val player = event.entity
        if (player.isShiftKeyDown()) return
        val id = key(player)
        val state = pending[id] ?: return
        val now = player.level().gameTime
        if (now - state.armedAt > CONFIRM_TICKS) {
            pending.remove(id)
        } else {
            state.released = true
        }
    }

    @SubscribeEvent
    fun onLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        val id = event.entity.uuid
        pending.keys.removeIf { it.id == id }
        allowedTick.keys.removeIf { it.id == id }
        restoring.removeIf { it.id == id }
    }

    private fun key(player: Player) = Key(player.uuid, player.level().isClientSide)

    /** Под ногами на SAFE_DROP+1 блоков нет опоры: до поверхности больше SAFE_DROP. */
    private fun higherThanSurface(player: Player, blocks: Int): Boolean {
        val level = player.level()
        val x = Mth.floor(player.x)
        val z = Mth.floor(player.z)
        val feet = Mth.floor(player.y)
        for (drop in 1..(blocks + 1)) {
            val pos = BlockPos(x, feet - drop, z)
            if (pos.y < level.minBuildHeight) return true
            val state = level.getBlockState(pos)
            if (!state.getCollisionShape(level, pos).isEmpty || !state.fluidState.isEmpty) {
                return false
            }
        }
        return true
    }
}
