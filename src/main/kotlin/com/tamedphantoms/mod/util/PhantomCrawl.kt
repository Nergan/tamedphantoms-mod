package com.tamedphantoms.mod.util

/**
 * Ползание у земли: крылья чуть опущены и по очереди переступают, как лапы.
 * Чем выше скорость из настроек и чем быстрее фантом едет, тем чаще шаг.
 */
object PhantomCrawl {

    const val DROOP = 0.2f
    const val STEP = 0.12f
    const val TIP = 1.2f

    fun advance(horizontal: Double, speedMultiple: Double): Float {
        val config = (speedMultiple / 1.25).coerceIn(0.25, 4.0)
        val move = (horizontal / 0.05).coerceIn(0.0, 1.5)
        return (0.26 * config * move).toFloat()
    }
}
