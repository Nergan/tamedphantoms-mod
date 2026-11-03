package com.tamedphantoms.mod.util

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Поворот кватернионом двойной точности. Порядок умножения как у JOML и `PoseStack`:
 * `a * b` сначала поворачивает на `b`, потом на `a`. Без зависимостей, чтобы считалось и в тестах.
 */
class PhantomQuat(val x: Double, val y: Double, val z: Double, val w: Double) {

    operator fun times(o: PhantomQuat) = PhantomQuat(
        w * o.x + x * o.w + y * o.z - z * o.y,
        w * o.y - x * o.z + y * o.w + z * o.x,
        w * o.z + x * o.y - y * o.x + z * o.w,
        w * o.w - x * o.x - y * o.y - z * o.z,
    )

    fun conjugate() = PhantomQuat(-x, -y, -z, w)

    fun normalized(): PhantomQuat {
        val length = sqrt(x * x + y * y + z * z + w * w)
        return if (length <= 0.0) IDENTITY else PhantomQuat(x / length, y / length, z / length, w / length)
    }

    /** Угол поворота, радианы, от 0 до π. Через atan2: у acos возле нуля теряется точность. */
    fun angle(): Double = 2.0 * atan2(sqrt(x * x + y * y + z * z), abs(w))

    /** Повёрнутый вектор (vx, vy, vz). */
    fun transform(vx: Double, vy: Double, vz: Double): DoubleArray {
        val tx = 2.0 * (y * vz - z * vy)
        val ty = 2.0 * (z * vx - x * vz)
        val tz = 2.0 * (x * vy - y * vx)
        return doubleArrayOf(
            vx + w * tx + (y * tz - z * ty),
            vy + w * ty + (z * tx - x * tz),
            vz + w * tz + (x * ty - y * tx),
        )
    }

    /** Часть пути [t] от этого поворота к [target] по короткой дуге. */
    fun slerp(target: PhantomQuat, t: Double): PhantomQuat {
        var dot = x * target.x + y * target.y + z * target.z + w * target.w
        var tx = target.x
        var ty = target.y
        var tz = target.z
        var tw = target.w
        if (dot < 0.0) {
            dot = -dot
            tx = -tx
            ty = -ty
            tz = -tz
            tw = -tw
        }
        if (dot > 0.9995) {
            return PhantomQuat(x + (tx - x) * t, y + (ty - y) * t, z + (tz - z) * t, w + (tw - w) * t).normalized()
        }
        val theta = acos(dot)
        val s = sin(theta)
        val a = sin((1.0 - t) * theta) / s
        val b = sin(t * theta) / s
        return PhantomQuat(a * x + b * tx, a * y + b * ty, a * z + b * tz, a * w + b * tw)
    }

    /** Углы (x, y, z) для порядка Y·X·Z: `y(b) * x(a) * z(c)` даёт этот поворот. Радианы. */
    fun eulerYXZ(): DoubleArray = doubleArrayOf(
        asin((-2.0 * (y * z - w * x)).coerceIn(-1.0, 1.0)),
        atan2(x * z + y * w, 0.5 - y * y - x * x),
        atan2(y * x + w * z, 0.5 - x * x - z * z),
    )

    companion object {
        val IDENTITY = PhantomQuat(0.0, 0.0, 0.0, 1.0)

        fun x(angle: Double) = PhantomQuat(sin(angle / 2.0), 0.0, 0.0, cos(angle / 2.0))
        fun y(angle: Double) = PhantomQuat(0.0, sin(angle / 2.0), 0.0, cos(angle / 2.0))
        fun z(angle: Double) = PhantomQuat(0.0, 0.0, sin(angle / 2.0), cos(angle / 2.0))
    }
}
