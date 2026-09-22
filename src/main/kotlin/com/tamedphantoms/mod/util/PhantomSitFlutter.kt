package com.tamedphantoms.mod.util

import kotlin.math.sin

/**
 * Редкая фраза, пока фантом лежит: левое крыло, пауза, правое, и так четыре раза,
 * потом долгая тишина. Амплитуда маленькая.
 * Знак подъёма в модели — противоположный прошлому: тот уходил в землю.
 */
object PhantomSitFlutter {

    const val CYCLE = 260f
    private const val PULSE = 16f
    private const val AMPLITUDE = 0.16f

    private val beatStart = floatArrayOf(10f, 32f, 54f, 76f)
    private val beatWing = intArrayOf(0, 1, 0, 1)

    fun lift(ageInTicks: Float, wing: Int): Float {
        val time = ((ageInTicks % CYCLE) + CYCLE) % CYCLE
        var best = 0f
        for (i in beatStart.indices) {
            if (beatWing[i] != wing) continue
            val local = time - beatStart[i]
            if (local < 0f || local > PULSE) continue
            val s = sin(local / PULSE * Math.PI).toFloat()
            val lift = s * s * AMPLITUDE
            if (lift > best) best = lift
        }
        return best
    }
}
