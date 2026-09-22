package com.tamedphantoms.mod.util

import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Углы взгляда головы. Те же формулы, что у ванильного LookControl:
 * рыскание — куда смотреть по горизонтали, тангаж — на уровень глаз.
 *
 * Переворот модели на 180° вокруг Z (фантом лежит кверху брюхом) меняет
 * знак локальных поворотов головы: без этого она кивает и поворачивается
 * от игрока.
 */
object PhantomHeadLook {

    const val MAX_YAW_DEGREES = 75f
    const val MAX_PITCH_DEGREES = 60f
    const val YAW_SPEED_DEGREES = 10f
    const val PITCH_SPEED_DEGREES = 40f

    fun yawDegrees(fromX: Double, fromZ: Double, toX: Double, toZ: Double): Float {
        val dx = toX - fromX
        val dz = toZ - fromZ
        return (atan2(dz, dx) * (180.0 / Math.PI)).toFloat() - 90f
    }

    fun pitchDegrees(
        fromX: Double,
        fromY: Double,
        fromZ: Double,
        toX: Double,
        toY: Double,
        toZ: Double,
    ): Float {
        val dx = toX - fromX
        val dy = toY - fromY
        val dz = toZ - fromZ
        val horizontal = sqrt(dx * dx + dz * dz)
        return (-(atan2(dy, horizontal) * (180.0 / Math.PI))).toFloat()
    }

    /** Локальные градусы кости головы. [upsideDown] — модель перевёрнута вокруг Z. */
    fun modelYawDegrees(relativeYaw: Float, upsideDown: Boolean): Float {
        val wrapped = wrapDegrees(relativeYaw)
        return if (upsideDown) -wrapped else wrapped
    }

    fun modelPitchDegrees(relativePitch: Float, upsideDown: Boolean): Float =
        if (upsideDown) -relativePitch else relativePitch

    /**
     * Покой головы в ванильной сетке смотрит примерно на 11° вниз.
     * Если оставить этот запас, пока фантом целится в игрока на одной высоте,
     * взгляд садится в грудь, а не в глаза.
     */
    const val REST_PITCH = 0.2f

    fun headPitchRadians(relativePitch: Float, upsideDown: Boolean, tracking: Boolean): Float {
        val rest = if (tracking && !upsideDown) 0f else REST_PITCH
        return rest + modelPitchDegrees(relativePitch, upsideDown) * (Math.PI.toFloat() / 180f)
    }

    fun approachDegrees(current: Float, target: Float, maxStep: Float): Float {
        val delta = wrapDegrees(target - current).coerceIn(-maxStep, maxStep)
        return current + delta
    }

    /** Держит [angle] в пределах ±[limit] от [origin], по кратчайшей дуге. */
    fun clampRelative(origin: Float, angle: Float, limit: Float): Float {
        val delta = wrapDegrees(angle - origin).coerceIn(-limit, limit)
        return origin + delta
    }

    fun wrapDegrees(degrees: Float): Float {
        val wrapped = (degrees % 360f + 360f) % 360f
        return if (wrapped >= 180f) wrapped - 360f else wrapped
    }
}
