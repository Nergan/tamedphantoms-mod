package com.tamedphantoms.mod.util

import com.tamedphantoms.mod.config.ServerConfig
import kotlin.math.abs

/**
 * Петля и бочка только в полёте вперёд. Набор копится медленно.
 * Зависание, стрейф на месте, задний ход, ползание и сидение набор сбрасывают.
 */
object PhantomAcrobatics {

    const val DEFAULT_STEP = 0.3f
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
        step: Float = DEFAULT_STEP,
    ): State {
        val rate = step.coerceIn(0.05f, 12f)
        if (!acrobatic) {
            return State(ease(pitch, rate), ease(roll, rate), ease(loop, rate), completedLoop = false)
        }
        val climbDir = when {
            climb > 0.15f -> 1f
            climb < -0.15f -> -1f
            else -> 0f
        }
        val nextPitch = if (climbDir != 0f) pitch + climbDir * rate else ease(pitch, rate)
        val nextLoop = if (climbDir != 0f) loop + climbDir * rate else ease(loop, rate)
        val turn = when {
            strafe > 0.15f -> -1f
            strafe < -0.15f -> 1f
            else -> 0f
        }
        val nextRoll = if (turn != 0f) roll + turn * rate else ease(roll, rate)
        val done = abs(nextLoop) >= FULL_TURN
        return State(nextPitch, nextRoll, if (done) 0f else nextLoop, done)
    }

    fun configuredStep(): Float =
        ServerConfig.CONFIG.acrobaticsStep.get().toFloat().coerceIn(0.05f, 12f)

    private fun ease(value: Float, step: Float): Float =
        PhantomHeadLook.approachDegrees(value, 0f, step * 1.2f)
}
