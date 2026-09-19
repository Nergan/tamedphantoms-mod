package com.tamedphantoms.mod.entity.ai

import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.entity.PhantomMoveTargetAccess
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import java.util.EnumSet

/**
 * Пока игрок держит предмет приручения, дикий фантом летит к нему
 * и не атакует. Приоритет 0 и флаг MOVE глушат ванильное кружение.
 */
class WildPhantomTemptGoal(private val phantom: Phantom) : Goal() {

    companion object {
        private const val RANGE = 16.0
    }

    private var player: Player? = null

    init {
        flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
    }

    override fun canUse(): Boolean {
        if (phantom is TamedPhantomEntity) return false
        val found = findHoldingPlayer() ?: return false
        player = found
        return true
    }

    override fun canContinueToUse(): Boolean = canUse()

    override fun requiresUpdateEveryTick(): Boolean = true

    override fun tick() {
        val target = player ?: return
        phantom.target = null
        PhantomMoveTargetAccess.set(phantom, Vec3(target.x, target.y + 1.4, target.z))
    }

    override fun stop() {
        player = null
    }

    private fun findHoldingPlayer(): Player? {
        val tameItem = ServerConfig.CONFIG.resolveTameItem()
        return phantom.level().getNearestPlayer(phantom, RANGE)?.takeIf { player ->
            player.isAlive && (player.mainHandItem.`is`(tameItem) || player.offhandItem.`is`(tameItem))
        }
    }
}
