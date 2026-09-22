package com.tamedphantoms.mod.util

/**
 * Схема взмахов по режиму полёта. Вся логика живёт здесь:
 * чтобы откатить, удалите этот файл и верните прежний расчёт
 * фазы в [com.tamedphantoms.mod.entity.TamedPhantomEntity]
 * и позу крыльев в модели.
 *
 * Набор и взлёт — чаще и глубже, быстрый прямой полёт — чуть реже,
 * долгое снижение — крылья почти неподвижны и раскрыты,
 * у земли взмах почти пропадает, крыло висит и кончик опущен ещё ниже.
 */
object PhantomWingbeat {

    const val CLIMB_RATE = 1.28f
    const val CLIMB_AMPLITUDE = 1.32f
    const val CRUISE_RATE = 0.82f
    const val CRUISE_AMPLITUDE = 0.92f
    const val GLIDE_RATE = 0.12f
    const val GLIDE_AMPLITUDE = 0.08f
    const val HOVER_AMPLITUDE = 1.18f
    const val TAKEOFF_RATE = 1.42f
    const val TAKEOFF_AMPLITUDE = 1.48f
    const val TAKEOFF_TICKS = 64f
    const val GROUND_RATE = 0.12f
    const val GROUND_AMPLITUDE = 0.02f
    const val SWIM_RATE = 0.16f
    const val SWIM_AMPLITUDE = 0.05f

    /** Основание крыла вниз, радианы. Положительное число — вниз. */
    const val GROUND_DROOP = 0.72f

    /** Доля опускания, которую кончик отыгрывает обратно к горизонту. */
    const val TIP_CANCEL = 0.72f

    /** Насколько кончик опущен сильнее основания, когда крыло волочится. */
    const val TIP_DRAG = 1.4f

    private const val CLIMB_START = 0.06
    private const val CLIMB_FULL = 0.22
    private const val CRUISE_START = 0.22
    private const val CRUISE_FULL = 0.45
    private const val DESCEND_START = -0.05

    data class Pose(
        val rate: Float,
        val amplitude: Float,
        val droop: Float,
        /** 0 — кончик ближе к горизонту, 1 — кончик висит ниже основания. */
        val tipHang: Float,
    )

    fun stepGround(current: Float, onGround: Boolean): Float =
        if (onGround) (current + 0.22f).coerceAtMost(1f) else (current - 0.14f).coerceAtLeast(0f)

    fun stepTakeoff(current: Float, launch: Boolean): Float =
        if (launch) 1f else (current - 1f / TAKEOFF_TICKS).coerceAtLeast(0f)

    fun stepGlide(current: Float, descending: Boolean): Float {
        val target = if (descending) 1f else 0f
        val gain = if (descending) 0.045f else 0.14f
        return current + (target - current) * gain
    }

    fun descending(vertical: Double, hover: Float, ground: Float): Boolean =
        vertical < DESCEND_START && hover < 0.2f && ground < 0.25f

    fun pose(
        vertical: Double,
        horizontal: Double,
        hover: Float,
        ground: Float,
        takeoff: Float,
        glide: Float,
        swim: Float = 0f,
    ): Pose {
        val hoverW = hover.coerceIn(0f, 1f)
        val groundW = ground.coerceIn(0f, 1f)
        val takeoffW = takeoff.coerceIn(0f, 1f)
        val swimW = swim.coerceIn(0f, 1f)
        val glideW = glide.coerceIn(0f, 1f) * (1f - hoverW) * (1f - groundW) * (1f - swimW)
        val climbW = ramp(vertical, CLIMB_START, CLIMB_FULL) * (1f - groundW) * (1f - hoverW)
        val cruiseW = ramp(horizontal, CRUISE_START, CRUISE_FULL) *
            (1f - groundW) * (1f - hoverW) * (1f - climbW) * (1f - glideW)

        var rate = 1f
        var amplitude = 1f
        rate = mix(rate, CRUISE_RATE, cruiseW)
        amplitude = mix(amplitude, CRUISE_AMPLITUDE, cruiseW)
        rate = mix(rate, CLIMB_RATE, climbW)
        amplitude = mix(amplitude, CLIMB_AMPLITUDE, climbW)
        rate = mix(rate, GLIDE_RATE, glideW)
        amplitude = mix(amplitude, GLIDE_AMPLITUDE, glideW)
        rate = mix(rate, PhantomHover.flapRate(1f), hoverW)
        amplitude = mix(amplitude, HOVER_AMPLITUDE, hoverW)
        rate = mix(rate, GROUND_RATE, groundW)
        amplitude = mix(amplitude, GROUND_AMPLITUDE, groundW)
        rate = mix(rate, TAKEOFF_RATE, takeoffW)
        amplitude = mix(amplitude, TAKEOFF_AMPLITUDE, takeoffW)
        rate = mix(rate, SWIM_RATE, swimW)
        amplitude = mix(amplitude, SWIM_AMPLITUDE, swimW)
        val droop = GROUND_DROOP * groundW * (1f - takeoffW) * (1f - swimW)
        val tipHang = groundW * (1f - takeoffW) * (1f - swimW)
        return Pose(rate, amplitude, droop, tipHang)
    }

    private fun ramp(value: Double, start: Double, full: Double): Float {
        if (full <= start) return if (value >= full) 1f else 0f
        return ((value - start) / (full - start)).toFloat().coerceIn(0f, 1f)
    }

    private fun mix(from: Float, to: Float, weight: Float): Float = from + (to - from) * weight
}
