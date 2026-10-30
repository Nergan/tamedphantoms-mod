package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

/**
 * Крен и тангаж камеры повторяют модель в полёте вперёд. Знаки противоположны повороту модели: так горизонт совпадает с ней.
 * У земли наклон гаснет вместе с позой ползания, а не обрывается.
 */
object PhantomRideBank {

    data class Angles(val pitch: Float, val roll: Float)

    fun adjust(entity: Entity?, partialTick: Float, pitch: Float, roll: Float): Angles? {
        val rider = entity as? Player ?: return null
        val phantom = rider.vehicle as? TamedPhantomEntity ?: return null
        if (phantom.isOrderedToSit()) return null
        val fade = 1f - phantom.crawlVisual(partialTick)
        if (fade <= 0f) return null
        val ridePitch = Mth.lerp(partialTick, phantom.cameraPitchO, phantom.cameraPitch) * fade
        val rideRoll = Mth.lerp(partialTick, phantom.cameraRollO, phantom.cameraRoll) * fade
        return Angles(pitch - ridePitch, roll + rideRoll)
    }
}
