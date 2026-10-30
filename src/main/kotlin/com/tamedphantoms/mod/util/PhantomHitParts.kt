package com.tamedphantoms.mod.util

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Попал ли клик в седло. Луч упирается в коробку столкновений, а она
 * повёрнута не вместе с фантомом, поэтому точку переводим в оси корпуса.
 * Седло — середина верхней грани вдоль спины. Перевёрнутый сидящий фантом
 * лежит седлом вниз, туда клик не попадает.
 */
object PhantomHitParts {

    /** Доля высоты коробки, выше которой точка считается верхом спины. */
    const val TOP_SHARE = 0.6

    /**
     * @param dx смещение точки клика от позиции фантома
     * @param scale во сколько раз фантом больше ванильного (ширина коробки / 0.9)
     */
    fun onSaddle(
        dx: Double,
        dy: Double,
        dz: Double,
        yawDegrees: Float,
        boxHeight: Double,
        scale: Double,
        saddled: Boolean,
        upsideDown: Boolean,
    ): Boolean {
        if (!saddled || upsideDown) return false
        if (dy < boxHeight * TOP_SHARE) return false
        val yaw = Math.toRadians(yawDegrees.toDouble())
        val along = dx * -sin(yaw) + dz * cos(yaw)
        val side = dx * cos(yaw) + dz * sin(yaw)
        return abs(side) <= 0.32 * scale && along in (-0.8 * scale)..(0.22 * scale)
    }
}
