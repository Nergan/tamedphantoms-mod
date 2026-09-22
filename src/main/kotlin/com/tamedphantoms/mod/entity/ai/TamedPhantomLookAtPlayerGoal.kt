package com.tamedphantoms.mod.entity.ai

import com.tamedphantoms.mod.config.ModConfig
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.player.Player
import java.util.EnumSet

/**
 * Периодический взгляд на ближайшего игрока, как у кошки, собаки и свиньи.
 *
 * Шанс 0.02 за тик и удержание 2–4 секунды — те же числа, что у
 * ванильного LookAtPlayerGoal. Работает и в полёте, и когда фантом
 * лежит перевёрнутым. Верхом взгляд не перебивает управление.
 */
class TamedPhantomLookAtPlayerGoal(private val phantom: TamedPhantomEntity) : Goal() {

    private var lookAt: Player? = null
    private var lookTime = 0

    init {
        flags = EnumSet.of(Flag.LOOK)
    }

    override fun canUse(): Boolean {
        if (phantom.isVehicle || phantom.isDefending()) return false
        if (phantom.random.nextFloat() >= CHANCE) return false
        lookAt = phantom.level().getNearestPlayer(phantom, ModConfig.LOOK_AT_PLAYER_RANGE)
        return lookAt != null
    }

    override fun canContinueToUse(): Boolean {
        val target = lookAt ?: return false
        if (phantom.isVehicle || phantom.isDefending() || !target.isAlive) return false
        if (phantom.distanceToSqr(target) > RANGE_SQR) return false
        return lookTime > 0
    }

    override fun start() {
        lookTime = adjustedTickDelay(40 + phantom.random.nextInt(40))
    }

    override fun stop() {
        if (phantom.glanceTarget === lookAt) {
            phantom.glanceTarget = null
        }
        lookAt = null
    }

    override fun tick() {
        phantom.glanceTarget = lookAt
        lookTime--
    }

    override fun requiresUpdateEveryTick(): Boolean = true

    companion object {
        private const val CHANCE = 0.02f
        private val RANGE_SQR = ModConfig.LOOK_AT_PLAYER_RANGE * ModConfig.LOOK_AT_PLAYER_RANGE
    }
}
