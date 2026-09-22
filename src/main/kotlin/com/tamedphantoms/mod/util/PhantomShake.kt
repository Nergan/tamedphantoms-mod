package com.tamedphantoms.mod.util

import kotlin.math.sin

/** Короткое отряхивание после воды или дождя: конверт 0 → 1 → 0. */
object PhantomShake {

    const val LENGTH = 36

    fun envelope(ticksLeft: Int, partial: Float): Float {
        if (ticksLeft <= 0) return 0f
        val elapsed = (LENGTH - ticksLeft) + partial
        val x = (elapsed / LENGTH).coerceIn(0f, 1f)
        return sin(x * Math.PI).toFloat()
    }
}

/**
 * Переход «был мокрый → высох». Первый замер только запоминает состояние.
 * Дождь, который сменился водой, отряхивание не запускает: его даст выход из воды.
 */
object PhantomWetness {

    data class Memory(
        val tracked: Boolean = false,
        val inWater: Boolean = false,
        val inRain: Boolean = false,
    )

    /** Пара: новое состояние и нужно ли начать отряхивание. */
    fun step(memory: Memory, inWater: Boolean, inRain: Boolean): Pair<Memory, Boolean> {
        if (!memory.tracked) {
            return Memory(tracked = true, inWater = inWater, inRain = inRain) to false
        }
        val leftWater = memory.inWater && !inWater
        val leftRain = memory.inRain && !inRain && !inWater
        return Memory(tracked = true, inWater = inWater, inRain = inRain) to (leftWater || leftRain)
    }
}
