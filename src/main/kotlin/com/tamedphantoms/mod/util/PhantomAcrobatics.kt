package com.tamedphantoms.mod.util

/**
 * Когда камера всадника повторяет наклон фантома: только в полёте вперёд.
 * Зависание, стрейф на месте, задний ход, ползание и сидение держат камеру ровно.
 * Сами петля и бочка — траектория в [PhantomFlightModel].
 */
object PhantomAcrobatics {

    private const val FORWARD_EPS = 0.045

    fun allows(
        forward: Double,
        strafe: Double,
        crawling: Boolean,
        sitting: Boolean,
        ridden: Boolean,
    ): Boolean {
        if (!ridden || sitting || crawling) return false
        return forward > FORWARD_EPS
    }
}
