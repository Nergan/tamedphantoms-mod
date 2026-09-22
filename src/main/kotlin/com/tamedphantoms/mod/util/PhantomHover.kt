package com.tamedphantoms.mod.util

/**
 * Зависание и задний ход верхом.
 * Наклон корпуса — в сторону, противоположную прошлой: прежний знак на модели
 * выглядел перевёрнутым. На полном смешении это 28°.
 * Взмахи в этом положении в 1.75 раза чаще обычных.
 */
object PhantomHover {

    const val PITCH_DEGREES = 28f
    const val FLAP_BOOST = 0.75f
    const val BLEND_PER_TICK = 0.10f
    const val REVERSE_SPEED = 0.75

    fun step(current: Float, moving: Boolean): Float {
        val target = if (moving) 0f else 1f
        return current + (target - current) * BLEND_PER_TICK
    }

    /** Добавка к xRot тела. */
    fun pitchOffset(blend: Float): Float = PITCH_DEGREES * blend.coerceIn(0f, 1f)

    fun flapRate(blend: Float): Float = 1f + FLAP_BOOST * blend.coerceIn(0f, 1f)

    /** Насколько корпус уже ушёл от прицела пилота в сторону зависания, градусы. */
    fun noseUpDegrees(pilotPitch: Float, bodyPitch: Float): Float =
        (bodyPitch - pilotPitch * 0.5f).coerceIn(0f, PITCH_DEGREES)

    fun blendFromPitches(pilotPitch: Float, bodyPitch: Float): Float =
        noseUpDegrees(pilotPitch, bodyPitch) / PITCH_DEGREES
}
