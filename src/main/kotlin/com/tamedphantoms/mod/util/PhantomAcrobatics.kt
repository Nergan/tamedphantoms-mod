package com.tamedphantoms.mod.util

import kotlin.math.abs

/**
 * Петля и бочка только в полёте вперёд. Набор копится медленно.
 * Зависание, стрейф на месте, задний ход, ползание и сидение набор сбрасывают.
 */
object PhantomAcrobatics {

    const val STEP = 2f
    const val EASE = 2.4f
    const val FULL_TURN = 360f
    private const val FORWARD_EPS = 0.045

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
        return forward > FORWARD_EPS
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

    private fun ease(value: Float): Float = PhantomHeadLook.approachDegrees(value, 0f, EASE)
}
