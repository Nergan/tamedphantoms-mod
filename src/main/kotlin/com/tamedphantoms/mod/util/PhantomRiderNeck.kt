package com.tamedphantoms.mod.util

import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Шея седока в первом лице. Пока корпус наклонён от взгляда не дальше порога, голова держит
 * горизонт, а глаза едут вместе со спиной. Дальше корпус тянет голову за собой, поэтому
 * в петле и бочке взгляд идёт с фантомом. После фигуры голова плавно выравнивается.
 * Наклоны заданы в осях курса фантома: курс ведёт мышь, шея его не трогает.
 */
class PhantomRiderNeck {

    /** Куда шея ставит голову. */
    private var held = PhantomQuat.IDENTITY

    /** Наклон головы в кадре: [held] после сглаживания. */
    var shown = PhantomQuat.IDENTITY
        private set

    fun reset() {
        held = PhantomQuat.IDENTITY
        shown = PhantomQuat.IDENTITY
    }

    /**
     * @param body наклон корпуса, см. [PhantomRiderSight.bodyTilt]
     * @param seconds время с прошлого кадра
     * @param neck угол корпуса, до которого голова держит горизонт, градусы. 0 — взгляд всегда с корпусом
     * @param smoothing время сглаживания взгляда, секунды. 0 — без сглаживания
     */
    fun step(body: PhantomQuat, seconds: Double, neck: Double, smoothing: Double) {
        level(body, seconds, smoothing)
        held = keepWithin(held, body, neck * RAD)
        if (smoothing <= 0.0) {
            shown = held
        } else if (seconds > 0.0) {
            shown = shown.slerp(held, 1.0 - exp(-seconds / smoothing)).normalized()
        }
        shown = keepWithin(shown, held, MAX_LAG)
    }

    /**
     * Голова тянется к горизонту, пока корпус к нему близко. У вершины петли сторона
     * «ближе к горизонту» меняется, поэтому там голову только тянет корпус, а выравнивание
     * возвращается постепенно и не быстрее [LEVEL_SPEED].
     */
    private fun level(body: PhantomQuat, seconds: Double, smoothing: Double) {
        if (seconds <= 0.0) return
        val near = ((LEVEL_LIMIT - body.angle()) / LEVEL_FADE).coerceIn(0.0, 1.0)
        val angle = held.angle()
        if (near <= 0.0 || angle <= 0.0) return
        val turn = min(angle * (1.0 - exp(-seconds / (LEVEL_SECONDS + smoothing))), LEVEL_SPEED * seconds) * near
        held = held.slerp(PhantomQuat.IDENTITY, turn / angle).normalized()
    }

    companion object {
        private const val RAD = Math.PI / 180.0
        private const val LEVEL_SECONDS = 0.15
        private const val LEVEL_LIMIT = 90.0 * RAD
        private const val LEVEL_FADE = 30.0 * RAD
        private const val LEVEL_SPEED = 240.0 * RAD
        // При отставании больше полуоборота сглаживание повернуло бы взгляд в обратную сторону.
        private const val MAX_LAG = 120.0 * RAD

        /** [q], сдвинутый к [around] по той же дуге так, чтобы между ними было не больше [limit] радиан. */
        fun keepWithin(q: PhantomQuat, around: PhantomQuat, limit: Double): PhantomQuat {
            var relative = around.conjugate() * q
            if (relative.w < 0.0) relative = PhantomQuat(-relative.x, -relative.y, -relative.z, -relative.w)
            val angle = relative.angle()
            if (angle <= limit) return q
            if (limit <= 0.0) return around
            val scale = sin(limit / 2.0) / sin(angle / 2.0)
            return (around * PhantomQuat(relative.x * scale, relative.y * scale, relative.z * scale, cos(limit / 2.0))).normalized()
        }
    }
}
