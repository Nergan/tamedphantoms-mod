package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.util.MoonSickness
import net.minecraft.util.Mth

/** Туман на секунду полнолунного крика становится болезненно-жёлтым. */
object PhantomMoonLight {

    data class FogTint(val red: Float, val green: Float, val blue: Float)

    fun onTick() {
        MoonSickness.tick()
    }

    fun tint(red: Float, green: Float, blue: Float): FogTint? {
        val strength = MoonSickness.strength()
        if (strength <= 0.02f) return null
        return FogTint(
            Mth.lerp(strength, red, 0.62f),
            Mth.lerp(strength, green, 0.48f),
            Mth.lerp(strength, blue, 0.08f),
        )
    }
}
