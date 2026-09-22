package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.util.MoonSickness
import net.minecraft.util.Mth
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.ViewportEvent

/** Туман на секунду полнолунного крика становится болезненно-жёлтым. */
object PhantomMoonLight {

    @SubscribeEvent
    fun onTick(event: ClientTickEvent.Post) {
        MoonSickness.tick()
    }

    @SubscribeEvent
    fun onFog(event: ViewportEvent.ComputeFogColor) {
        val strength = MoonSickness.strength()
        if (strength <= 0.02f) return
        event.red = Mth.lerp(strength, event.red, 0.62f)
        event.green = Mth.lerp(strength, event.green, 0.48f)
        event.blue = Mth.lerp(strength, event.blue, 0.08f)
    }
}
