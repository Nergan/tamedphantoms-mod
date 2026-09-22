package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.ViewportEvent

/** Крен камеры только от бокового полёта. Тангаж взгляда не трогает. */
object PhantomRideBank {

    @SubscribeEvent
    fun onCamera(event: ViewportEvent.ComputeCameraAngles) {
        val rider = event.camera.entity as? Player ?: return
        val phantom = rider.vehicle as? TamedPhantomEntity ?: return
        if (phantom.isOrderedToSit()) return
        val partial = event.partialTick.toFloat()
        event.roll = event.roll - phantom.bankVisual(partial)
    }
}
