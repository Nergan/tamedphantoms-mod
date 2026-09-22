package com.tamedphantoms.mod.util

import kotlin.math.sin

/**
 * Лёжа кверху брюхом, фантом по очереди чуть поднимает крылья.
 * Подъём на экране — это опускание крыла в координатах модели:
 * рендер переворачивает её на 180° вокруг Z.
 */
object PhantomSitFlutter {

    const val PERIOD = 80f
    private const val PULSE = 18f
    private const val AMPLITUDE = 0.5f

    fun lift(ageInTicks: Float, wing: Int): Float {
        val local = (ageInTicks + wing * PERIOD * 0.5f) % PERIOD
        if (local > PULSE) return 0f
        val s = sin(local / PULSE * Math.PI).toFloat()
        return s * s * AMPLITUDE
    }
}
