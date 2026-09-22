package com.tamedphantoms.mod.util

/**
 * Ползание у земли: крылья чуть приподняты, прямые, и по очереди
 * ходят вперёд-назад. Частота шага растёт со скоростью из настроек.
 */
object PhantomCrawl {

    /** Небольшой подъём всего крыла, радианы. Кончик не загибается отдельно. */
    const val LIFT = 0.08f

    /** Размах шага вперёд-назад, радианы. */
    const val SWEEP = 0.42f

    fun advance(horizontal: Double, speedMultiple: Double): Float {
        val config = (speedMultiple / 1.25).coerceIn(0.25, 4.0)
        val move = (horizontal / 0.05).coerceIn(0.0, 1.5)
        return (0.22 * config * move).toFloat()
    }
}
