package com.tamedphantoms.mod.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Куда вести дикого фантома к игроку с приманкой.
 *
 * Ванильный `PhantomMoveControl` умножает горизонталь на `1 − 0.7·|dy| / horiz`.
 * Если точка почти под фантомом, множитель уходит в минус: фантом разворачивается
 * от игрока и садится к нему задом, задрав тело. Поэтому спуск не круче [MAX_SLOPE],
 * а вблизи игрока фантом кружит по кольцу [ORBIT], спускаясь по спирали носом вниз.
 */
object PhantomTemptPath {

    /** |dy| / horiz. При 0.9 ванильный множитель не меньше 0.37. */
    const val MAX_SLOPE = 0.9

    /** Радиус кольца вокруг игрока, блоки. */
    const val ORBIT = 3.5

    /** Насколько точка на кольце уходит вперёд по кругу от фантома, радианы. */
    private const val LEAD = 0.9

    data class Point(val x: Double, val y: Double, val z: Double)

    fun aim(fromX: Double, fromY: Double, fromZ: Double, toX: Double, toY: Double, toZ: Double): Point {
        var targetX = toX
        var targetZ = toZ
        if (hypot(toX - fromX, toZ - fromZ) < ORBIT) {
            val angle = atan2(fromZ - toZ, fromX - toX) + LEAD
            targetX = toX + cos(angle) * ORBIT
            targetZ = toZ + sin(angle) * ORBIT
        }
        val reach = hypot(targetX - fromX, targetZ - fromZ).coerceAtLeast(0.5)
        val limit = reach * MAX_SLOPE
        val targetY = fromY + (toY - fromY).coerceIn(-limit, limit)
        return Point(targetX, targetY, targetZ)
    }
}
