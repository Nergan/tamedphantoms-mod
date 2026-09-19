package com.tamedphantoms.mod.entity.ai

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.world.entity.ai.goal.Goal
import java.util.EnumSet
import kotlin.math.cos
import kotlin.math.sin

/**
 * Медленное кружение рядом с тем, кто держит поводок.
 */
class TamedPhantomLeashWanderGoal(private val phantom: TamedPhantomEntity) : Goal() {

    private var orbit = 0.0
    private var radius = 1.6
    private var height = 1.0
    private var recalc = 0

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
        orbit += 0.055 + (phantom.random.nextDouble() - 0.5) * 0.02
        val targetX = holder.x + cos(orbit) * radius
        val targetZ = holder.z + sin(orbit) * radius
        val targetY = holder.y + height
        phantom.moveControl.setWantedPosition(targetX, targetY, targetZ, 0.20)
    }
}
