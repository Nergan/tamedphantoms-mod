package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.ViewportEvent

/** Крен и тангаж камеры повторяют модель в полёте вперёд. Знаки противоположны повороту модели: так горизонт совпадает с ней. */
object PhantomRideBank {

    @SubscribeEvent
    fun onCamera(event: ViewportEvent.ComputeCameraAngles) {
        val rider = event.camera.entity as? Player ?: return
        val phantom = rider.vehicle as? TamedPhantomEntity ?: return
        if (phantom.isOrderedToSit()) return
        val partial = event.partialTick.toFloat()
        if (phantom.crawlVisual(partial) > 0.45f) return
        val pitch = Mth.lerp(partial, phantom.cameraPitchO, phantom.cameraPitch)
        val roll = Mth.lerp(partial, phantom.cameraRollO, phantom.cameraRoll)
        event.pitch = event.pitch - pitch
        event.roll = event.roll + roll
    }
}
