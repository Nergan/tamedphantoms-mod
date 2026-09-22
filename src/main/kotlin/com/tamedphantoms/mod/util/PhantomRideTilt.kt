package com.tamedphantoms.mod.util

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.world.entity.player.Player

/**
 * Насколько нос уже задран относительно прицела. По этому числу
 * ускоряются взмахи. Камеру и модель игрока оно не двигает.
 */
object PhantomRideTilt {

    fun noseUpDegrees(phantom: TamedPhantomEntity, partial: Float): Float {
        val pilot = phantom.controllingPassenger as? Player ?: return 0f
        return PhantomHover.noseUpDegrees(
            pilot.getViewXRot(partial),
            phantom.getViewXRot(partial),
        )
    }

    fun blend(phantom: TamedPhantomEntity, partial: Float): Float =
        noseUpDegrees(phantom, partial) / PhantomHover.PITCH_DEGREES
}
