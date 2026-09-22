package com.tamedphantoms.mod.entity.ai

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.PhantomHeldLook
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.player.Player
import java.util.EnumSet

/**
 * Пока игрок показывает еду прирученному или предмет приручения освобождённому,
 * голова смотрит на него всё время, а не короткими взглядами.
 */
class TamedPhantomHeldItemLookGoal(private val phantom: TamedPhantomEntity) : Goal() {

    private var lookAt: Player? = null

    init {
        flags = EnumSet.of(Flag.LOOK)
    }

    override fun canUse(): Boolean {
        if (phantom.isVehicle || phantom.isDefending()) return false
        lookAt = find()
        return lookAt != null
    }

    override fun canContinueToUse(): Boolean {
        if (phantom.isVehicle || phantom.isDefending()) return false
        lookAt = find()
        return lookAt != null
    }

    override fun start() {}

    override fun stop() {
        if (phantom.glanceTarget === lookAt) {
            phantom.glanceTarget = null
        }
        lookAt = null
    }

    override fun tick() {
        phantom.glanceTarget = lookAt
    }

    override fun requiresUpdateEveryTick(): Boolean = true

    private fun find(): Player? = PhantomHeldLook.nearest(phantom) { player ->
        PhantomHeldLook.shouldStare(
            phantom.tamed,
            PhantomHeldLook.holdsFood(player),
            PhantomHeldLook.holdsTameItem(player),
        )
    }
}
