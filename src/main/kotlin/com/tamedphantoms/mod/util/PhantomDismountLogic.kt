package com.tamedphantoms.mod.util

/**
 * Два отдельных нажатия Shift за [WINDOW_TICKS] тиков.
 * Удержание — это повтор попытки каждый тик; новым нажатием она становится
 * только после паузы хотя бы в [NEW_PRESS_GAP] тиков.
 */
object PhantomDismountLogic {

    const val WINDOW_TICKS = 20L
    const val NEW_PRESS_GAP = 2L

    data class Tap(val confirm: Boolean, val warn: Boolean, val windowStart: Long?)

    /** [windowStart] — тик первого нажатия. Повтор в ту же игровую секунду подтверждает слезание. */
    fun onTap(now: Long, windowStart: Long?): Tap {
        if (windowStart == null) {
            return Tap(confirm = false, warn = true, windowStart = now)
        }
        if (now == windowStart) {
            return Tap(confirm = false, warn = false, windowStart = windowStart)
        }
        if (now - windowStart <= WINDOW_TICKS) {
            return Tap(confirm = true, warn = false, windowStart = null)
        }
        return Tap(confirm = false, warn = true, windowStart = now)
    }

    fun isNewPress(now: Long, previous: Long?): Boolean {
        if (previous == null) return true
        return now - previous >= NEW_PRESS_GAP
    }
}
