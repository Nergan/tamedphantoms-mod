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

    /** Меньше этого смещения за тик фантом стоит: шаг не идёт. */
    const val STILL = 0.004

    /**
     * Фаза шага за тик. Длина шага постоянная, поэтому фаза идёт пропорционально пути:
     * медленный ход — медленный шаг, остаточный дрейф после посадки шаг не крутит.
     */
    fun advance(horizontal: Double, speedMultiple: Double): Float {
        if (horizontal < STILL) return 0f
        val config = (speedMultiple / 1.25).coerceIn(0.25, 4.0)
        val move = (horizontal / 0.08).coerceAtMost(1.25)
        return (0.62f * config * move).toFloat()
    }

    /** Размах шага: 1 — идёт, 0 — стоит, крылья возвращаются в покой. */
    fun stride(current: Float, horizontal: Double): Float {
        val target = if (horizontal < STILL) 0f else (horizontal / 0.05).toFloat().coerceIn(0.35f, 1f)
        val rate = if (target > current) 0.25f else 0.12f
        return current + (target - current) * rate
    }
}
