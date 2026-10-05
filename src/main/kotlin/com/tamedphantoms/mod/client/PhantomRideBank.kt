package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

/** Крен и тангаж камеры повторяют модель в полёте вперёд. Знаки противоположны повороту модели: так горизонт совпадает с ней. */
object PhantomRideBank {

    data class Angles(val pitch: Float, val roll: Float)

    fun adjust(entity: Entity?, partialTick: Float, pitch: Float, roll: Float): Angles? {
        val rider = entity as? Player ?: return null
        val phantom = rider.vehicle as? TamedPhantomEntity ?: return null
        if (phantom.isOrderedToSit()) return null
        if (phantom.crawlVisual(partialTick) > 0.45f) return null
        val ridePitch = Mth.lerp(partialTick, phantom.cameraPitchO, phantom.cameraPitch)
        val rideRoll = Mth.lerp(partialTick, phantom.cameraRollO, phantom.cameraRoll)
        return Angles(pitch - ridePitch, roll + rideRoll)
    }
}
