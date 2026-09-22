package com.tamedphantoms.mod.util

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Наклон корпуса от самого полёта, а не от прицела игрока.
 * Зависание, задний ход и чистое снижение держат нос в позе зависания.
 * Вперёд или вбок при наборе — слегка в ту же сторону, при снижении — в противоположную.
 * Боковая скорость кренит тело и чуть поворачивает голову, не дальше [HEAD_YAW].
 *
 * Положительный крен — влево (левое крыло ниже). Положительный тангаж — поза зависания.
 */
object PhantomFlightAttitude {

    const val MOVE_PITCH = 11f
    const val BANK = 22f
    const val HEAD_YAW = 14f

    private const val MOVE_EPS = 0.045
    private const val VERTICAL_FULL = 0.28
    private const val STRAFE_FULL = 0.2

    data class Pose(
        val pitch: Float,
        val bank: Float,
        val headYaw: Float,
    )

    /** Назад, зависание и стрейф без хода вперёд держат позу зависания. */
    fun wantsHover(forwardSpeed: Double, strafeSpeed: Double): Boolean = forwardSpeed <= MOVE_EPS

    fun pose(forwardSpeed: Double, strafeSpeed: Double, vertical: Double, hoverBlend: Float): Pose {
        val climb = (vertical / VERTICAL_FULL).coerceIn(-1.0, 1.0).toFloat()
        val movePitch = climb * MOVE_PITCH
        val blend = hoverBlend.coerceIn(0f, 1f)
        val pitch = movePitch + (PhantomHover.pitchOffset(1f) - movePitch) * blend
        val side = (strafeSpeed / STRAFE_FULL).coerceIn(-1.0, 1.0).toFloat()
        return Pose(
            pitch = pitch,
            bank = -side * BANK,
            headYaw = -side * HEAD_YAW,
        )
    }

    /** Градусы кости хвоста. Положительная скорость вверх опускает хвост на модели со знаком минус. */
    fun tailPitchDegrees(vertical: Double): Float {
        if (abs(vertical) < 0.03) return 0f
        return (-vertical * 64.0).coerceIn(-26.0, 26.0).toFloat()
    }

    /** Продольная скорость вперёд и боковая влево, блоки/тик. */
    fun split(yawDegrees: Float, dx: Double, dz: Double): Pair<Double, Double> {
        val yaw = Math.toRadians(yawDegrees.toDouble())
        val sinYaw = sin(yaw)
        val cosYaw = cos(yaw)
        val forward = dx * -sinYaw + dz * cosYaw
        val strafe = dx * cosYaw + dz * sinYaw
        return forward to strafe
    }
}
