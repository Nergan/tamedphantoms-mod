package com.tamedphantoms.mod.entity.ai

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.world.entity.ai.goal.Goal
import java.util.EnumSet
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Медленное кружение рядом с тем, кто держит поводок.
 */
class TamedPhantomLeashWanderGoal(private val phantom: TamedPhantomEntity) : Goal() {

    private var orbit = 0.0
    private var radius = 1.6
    private var height = 1.0
    private var recalc = 0
    private var stuckTicks = 0
    private var stuckX = Double.NaN
    private var stuckZ = Double.NaN

    init {
        flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse(): Boolean {
        return phantom.tamed &&
            phantom.isLeashed &&
            !phantom.isOrderedToSit() &&
            !phantom.isVehicle() &&
            phantom.leashHolder != null
    }

    override fun canContinueToUse(): Boolean = canUse()

    override fun requiresUpdateEveryTick(): Boolean = true

    override fun start() {
        orbit = phantom.random.nextDouble() * Math.PI * 2.0
        recalc = 0
    }

    override fun tick() {
        val holder = phantom.leashHolder ?: return
        if (recalc-- <= 0) {
            radius = 1.1 + phantom.random.nextDouble() * 1.5
            height = 0.7 + phantom.random.nextDouble() * 1.2
            recalc = 40 + phantom.random.nextInt(45)
        }
        val moved = if (stuckX.isNaN()) 1.0 else hypot(phantom.x - stuckX, phantom.z - stuckZ)
        stuckX = phantom.x
        stuckZ = phantom.z
        if (phantom.horizontalCollision || moved < 0.03) stuckTicks++ else stuckTicks = 0
        if (stuckTicks >= 12) {
            orbit += if (phantom.random.nextBoolean()) 2.1 else -2.1
            stuckTicks = 0
        }
        orbit += 0.055 + (phantom.random.nextDouble() - 0.5) * 0.02
        val targetX = holder.x + cos(orbit) * radius
        val targetZ = holder.z + sin(orbit) * radius
        val targetY = holder.y + height
        phantom.moveControl.setWantedPosition(targetX, targetY, targetZ, 0.20)
    }
}
