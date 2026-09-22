package com.tamedphantoms.mod.util

/**
 * Ползание у земли: крылья чуть приподняты, прямые, и по очереди
 * ходят вперёд-назад. Частота шага растёт со скоростью из настроек.
 */
object PhantomCrawl {

    /** Небольшой подъём всего крыла, радианы. Кончик не загибается отдельно. */
    const val LIFT = 0.08f

    /** Размах основания: короткий ход кончика вперёд-назад, без складывания к телу. */
    const val SWEEP = 0.14f

    /** Дополнительный ход кончика по той же дуге. */
    const val TIP_SWEEP = 0.1f

    fun advance(horizontal: Double, speedMultiple: Double): Float {
        if (horizontal <= 0.0) return 0f
        val config = (speedMultiple / 1.25).coerceIn(0.25, 4.0)
        val move = (horizontal / 0.08).coerceIn(0.7, 1.25)
        return (0.62f * config * move).toFloat()
    }
}
