package com.tamedphantoms.mod.entity.ai

import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.player.Player
import java.util.EnumSet
import kotlin.math.cos
import kotlin.math.sin

/**
 * Плавное блуждание освобождённого фантома. Если игрок держит предмет
 * приручения — мягко летит к нему.
 */
class TamedPhantomWanderGoal(private val phantom: TamedPhantomEntity) : Goal() {

    companion object {
        private const val TEMPT_RANGE = 16.0
    }

    private var heading = 0.0
    private var climb = 0.0
    private var recalcCooldown = 0

    init {
        flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse(): Boolean = !phantom.tamed && !phantom.isVehicle()

    override fun canContinueToUse(): Boolean = !phantom.tamed && !phantom.isVehicle()

    override fun requiresUpdateEveryTick(): Boolean = true

    override fun start() {
        heading = phantom.yRot * (Math.PI / 180.0)
        climb = 0.0
        recalcCooldown = 0
    }

    override fun tick() {
        val tempting = findTemptingPlayer()
        if (tempting != null) {
            phantom.moveControl.setWantedPosition(tempting.x, tempting.y + 1.4, tempting.z, 0.85)
            return
        }

        if (this.recalcCooldown-- <= 0) {
            this.recalcCooldown = 70 + phantom.random.nextInt(70)
            heading += (phantom.random.nextDouble() - 0.5) * 1.1
            climb = (phantom.random.nextDouble() - 0.42) * 0.28
        }

        heading += 0.016 + (phantom.random.nextDouble() - 0.5) * 0.012
        val ahead = 7.0
        val targetX = phantom.x + cos(heading) * ahead
        val targetZ = phantom.z + sin(heading) * ahead
        val targetY = phantom.y + climb * 5.0

        phantom.moveControl.setWantedPosition(targetX, targetY, targetZ, 0.48)
    }

    private fun findTemptingPlayer(): Player? {
        val tameItem = ServerConfig.CONFIG.resolveTameItem()
        return phantom.level().getNearestPlayer(phantom, TEMPT_RANGE)?.takeIf { player ->
            player.isAlive && (player.mainHandItem.`is`(tameItem) || player.offhandItem.`is`(tameItem))
        }
    }
}
