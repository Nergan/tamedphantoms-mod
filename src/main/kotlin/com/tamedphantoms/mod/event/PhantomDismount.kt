package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.EntityMountEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent
import java.util.UUID

/**
 * С высоты больше 8 блоков до поверхности слезание требует второго Shift
 * в течение секунды. Первое нажатие только предупреждает.
 */
object PhantomDismount {

    private const val CONFIRM_TICKS = 20
    private const val SAFE_DROP = 8

    private data class Key(val id: UUID, val client: Boolean)

    private class Attempt(val armedAt: Long, var released: Boolean)

    private val pending = HashMap<Key, Attempt>()

    @SubscribeEvent
    fun onMount(event: EntityMountEvent) {
        if (event.isMounting) return
        val player = event.entityMounting as? Player ?: return
        if (event.entityBeingMounted !is TamedPhantomEntity) return
        if (!player.isAlive || !player.isShiftKeyDown()) return
        if (!higherThanSurface(player, SAFE_DROP)) {
            pending.remove(key(player))
            return
        }
        val now = player.level().gameTime
        val state = pending[key(player)]
        if (state != null && state.released && now - state.armedAt <= CONFIRM_TICKS) {
            pending.remove(key(player))
            return
        }
        if (state == null || state.released) {
            pending[key(player)] = Attempt(now, released = false)
            if (player is ServerPlayer) {
                player.displayClientMessage(
                    Component.translatable("tamedphantoms.dismount.high").withStyle(ChatFormatting.YELLOW),
                    true,
                )
            }
        }
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onTick(event: PlayerTickEvent.Post) {
        val player = event.entity
        val state = pending[key(player)] ?: return
        val now = player.level().gameTime
        if (player.isShiftKeyDown()) return
        if (now - state.armedAt > CONFIRM_TICKS) {
            pending.remove(key(player))
        } else {
            state.released = true
        }
    }

    @SubscribeEvent
    fun onLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        val id = event.entity.uuid
        pending.keys.removeIf { it.id == id }
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
