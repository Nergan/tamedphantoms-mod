package com.tamedphantoms.mod.util

import java.util.UUID

/**
 * Стая заступается за своего, когда его злит игрок. За освобождённого встают освобождённые
 * поблизости, за ручного — все фантомы его хозяина, даже если обидчик сам хозяин.
 */
object PhantomPack {

    /** Блоки, на которые освобождённые слышат своего. */
    const val RELEASED_RADIUS = 32.0

    /**
     * Встаёт ли фантом-сосед на защиту вместе с обиженным.
     * @param distanceSqr квадрат расстояния между ними
     */
    fun joins(
        victimTamed: Boolean,
        victimOwner: UUID?,
        mateTamed: Boolean,
        mateOwner: UUID?,
        distanceSqr: Double,
    ): Boolean = if (victimTamed) {
        victimOwner != null && mateTamed && mateOwner == victimOwner
    } else {
        !mateTamed && distanceSqr <= RELEASED_RADIUS * RELEASED_RADIUS
    }
}
