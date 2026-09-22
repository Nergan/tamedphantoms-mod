package com.tamedphantoms.mod.util

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.world.entity.player.Player

/**
 * Видимый клевок носом. Берётся из уже синхронизированного наклона тела,
 * поэтому и пилот, и пассажир, и чужие клиенты видят один и тот же угол.
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
