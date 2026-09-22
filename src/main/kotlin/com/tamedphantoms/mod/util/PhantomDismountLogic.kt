package com.tamedphantoms.mod.util

/**
 * Два отдельных нажатия Shift за [WINDOW_TICKS] тиков.
 * Сюда попадает только фронт клавиши: пока Shift зажат, повторных вызовов нет.
 */
object PhantomDismountLogic {

    const val WINDOW_TICKS = 20L

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
}
