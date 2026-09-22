package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.PhantomRideTilt
import net.minecraft.client.Minecraft
import net.neoforged.neoforge.client.event.ViewportEvent
import net.neoforged.neoforge.common.NeoForge

/**
 * Камера наездника и пассажира клевает вверх вместе с фантомом.
 * Мышиный взгляд не переписывается — это только сдвиг картинки.
 */
object PhantomRideCamera {

    fun init() {
        NeoForge.EVENT_BUS.addListener(::onCamera)
    }

    private fun onCamera(event: ViewportEvent.ComputeCameraAngles) {
        val player = Minecraft.getInstance().player ?: return
        val phantom = player.vehicle as? TamedPhantomEntity ?: return
        val noseUp = PhantomRideTilt.noseUpDegrees(phantom, event.partialTick.toFloat())
        if (noseUp < 0.05f) return
        event.pitch -= noseUp
    }
}
