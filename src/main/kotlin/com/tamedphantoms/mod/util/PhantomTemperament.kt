package com.tamedphantoms.mod.util

/**
 * Характер фантома. Выводится из его UUID, поэтому у одного фантома он один и тот же
 * после перезапуска и у всех игроков. Все значения 0..1.
 */
data class PhantomTemperament(
    /** Как часто подлетает к игрокам посмотреть. */
    val curiosity: Float,
    /** Как охотно крутит фигуры в воздухе. */
    val playfulness: Float,
    /** На какой высоте любит летать: 0 — низко, 1 — высоко. */
    val altitude: Float,
    /** Как часто садится отдохнуть. */
    val calm: Float,
) {

    /** Желаемая высота над землёй, блоки. Ночью выше: фантомы ночные охотники. */
    fun cruiseHeight(night: Boolean, raining: Boolean): Double {
        var height = 7.0 + altitude * 18.0
        if (night) height *= 1.35
        if (raining) height *= 0.6
        return height
    }

    /** Шанс за тик начать фигуру: ночью и в полнолуние выше, в новолуние ниже. */
    fun maneuverOdds(night: Boolean, fullMoon: Boolean, newMoon: Boolean): Int {
        var odds = 2200f - playfulness * 1500f
        if (night) odds *= 0.6f
        if (fullMoon) odds *= 0.35f
        if (newMoon) odds *= 2f
        return odds.toInt().coerceAtLeast(60)
    }

    /** Шанс за тик сесть отдохнуть. Днём фантом отдыхает чаще. */
    fun restOdds(night: Boolean, raining: Boolean): Int {
        var odds = 420f - calm * 260f
        if (!night) odds *= 0.5f
        if (raining) odds *= 0.7f
        return odds.toInt().coerceAtLeast(40)
    }

    companion object {
        fun of(seed: Long): PhantomTemperament {
            var s = seed xor 0x5DEECE66DL
            fun next(): Float {
                s = s * 6364136223846793005L + 1442695040888963407L
                return ((s ushr 40) and 0xFFFFFF).toFloat() / 0xFFFFFF.toFloat()
            }
            return PhantomTemperament(next(), next(), next(), next())
        }
    }
}
