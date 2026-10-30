package com.tamedphantoms.mod.util

import kotlin.math.min

/**
 * Несколько криков в один миг. Два крика — слепота вдвое дольше.
 * Девять — потолок: 8 секунд слепоты и 10 урона.
 */
object PhantomScreamStack {
    const val CAP = 9
    const val BLIND_CAP_SECONDS = 8
    private const val DAMAGE_START = 3
    private const val DAMAGE_CAP = 10.0

    fun counted(screams: Int): Int = screams.coerceIn(0, CAP)

    /** 1 крик — 1 секунда, 2 — 2, дальше не больше 8. */
    fun blindnessTicks(screams: Int): Int {
        val n = counted(screams)
        if (n <= 0) return 0
        return min(n, BLIND_CAP_SECONDS) * 20
    }

    /**
     * С трёх криков игроку наносится урон.
     * [closeness] 1 — вплотную к ближайшему фантому, 0 — на краю радиуса.
     * При трёх криках это 1–4, при девяти — всегда 10.
     */
    fun playerDamage(screams: Int, closeness: Double): Double {
        val n = counted(screams)
        if (n < DAMAGE_START) return 0.0
        val near = closeness.coerceIn(0.0, 1.0)
        val atThree = 1.0 + 3.0 * near
        val t = (n - DAMAGE_START).toDouble() / (CAP - DAMAGE_START).toDouble()
        return atThree + t * (DAMAGE_CAP - atThree)
    }

    fun fleeBlocks(screams: Int): Double {
        val n = counted(screams).coerceAtLeast(1)
        return 10.0 * min(n, BLIND_CAP_SECONDS)
    }

    fun closeness(distance: Double, radius: Double): Double {
        if (radius <= 0.0) return 1.0
        return (1.0 - distance / radius).coerceIn(0.0, 1.0)
    }
}
