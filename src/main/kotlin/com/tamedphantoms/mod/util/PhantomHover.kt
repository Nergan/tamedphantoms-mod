package com.tamedphantoms.mod.util

/**
 * Зависание верхом, когда наездник отпустил клавиши.
 * Нос задирается на 20°, взмахи становятся в полтора раза чаще.
 * Положительный градус здесь — нос вверх; в xRot Майнкрафта это минус.
 */
object PhantomHover {

    const val PITCH_DEGREES = 20f
    const val FLAP_BOOST = 0.50f
    const val BLEND_PER_TICK = 0.10f

    fun step(current: Float, moving: Boolean): Float {
        val target = if (moving) 0f else 1f
        return current + (target - current) * BLEND_PER_TICK
    }

    /** Добавка к xRot тела: отрицательная, нос вверх. */
    fun pitchOffset(blend: Float): Float = -PITCH_DEGREES * blend.coerceIn(0f, 1f)

    fun flapRate(blend: Float): Float = 1f + FLAP_BOOST * blend.coerceIn(0f, 1f)

    /** Насколько нос задран относительно прицела пилота, градусы 0..[PITCH_DEGREES]. */
    fun noseUpDegrees(pilotPitch: Float, bodyPitch: Float): Float =
        (pilotPitch * 0.5f - bodyPitch).coerceIn(0f, PITCH_DEGREES)

    fun blendFromPitches(pilotPitch: Float, bodyPitch: Float): Float =
        noseUpDegrees(pilotPitch, bodyPitch) / PITCH_DEGREES
}
