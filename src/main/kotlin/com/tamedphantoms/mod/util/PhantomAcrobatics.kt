package com.tamedphantoms.mod.util

import kotlin.math.abs

/**
 * Пока фантом летит вперёд или вбок, набор, снижение и поворот не останавливаются
 * на фиксированном угле: наклон копится и может пройти полный оборот.
 * Зависание, задний ход, ползание и сидение этот набор сбрасывают.
 */
object PhantomAcrobatics {

    const val STEP = 6f
    const val FULL_TURN = 360f

    data class State(
        val pitch: Float,
        val roll: Float,
        val loop: Float,
        val completedLoop: Boolean,
    )

    fun allows(
        forward: Double,
        strafe: Double,
        crawling: Boolean,
        sitting: Boolean,
        ridden: Boolean,
    ): Boolean {
        if (!ridden || sitting || crawling) return false
        return !PhantomFlightAttitude.wantsHover(forward, strafe)
    }

    fun step(
        pitch: Float,
        roll: Float,
        loop: Float,
        acrobatic: Boolean,
        climb: Float,
        strafe: Float,
    ): State {
        if (!acrobatic) {
            return State(ease(pitch), ease(roll), ease(loop), completedLoop = false)
        }
        val climbDir = when {
            climb > 0.15f -> 1f
            climb < -0.15f -> -1f
            else -> 0f
        }
        val nextPitch = if (climbDir != 0f) pitch + climbDir * STEP else ease(pitch)
        val nextLoop = if (climbDir != 0f) loop + climbDir * STEP else ease(loop)
        val turn = when {
            strafe > 0.15f -> -1f
            strafe < -0.15f -> 1f
            else -> 0f
        }
        val nextRoll = if (turn != 0f) roll + turn * STEP else ease(roll)
        val done = abs(nextLoop) >= FULL_TURN
        return State(nextPitch, nextRoll, if (done) 0f else nextLoop, done)
    }

    private fun ease(value: Float): Float = PhantomHeadLook.approachDegrees(value, 0f, STEP)
}
