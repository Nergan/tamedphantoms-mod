package com.tamedphantoms.mod.util

/**
 * Двойное нажатие Shift за [WINDOW_TICKS] тиков, пока фантом высоко над землёй.
 * Удержание первого нажатия вторым не считается.
 */
object PhantomDismountLogic {

    const val WINDOW_TICKS = 20L

    data class Tap(val startedAt: Long, val released: Boolean, val confirmed: Boolean = false)

    data class Step(val keepMounted: Boolean, val tap: Tap?, val warn: Boolean)

    fun onAttempt(high: Boolean, shiftDown: Boolean, rising: Boolean, tap: Tap?, now: Long): Step {
        if (!high || !shiftDown) return Step(keepMounted = false, tap = null, warn = false)
        if (tap != null && tap.confirmed) {
            return Step(keepMounted = false, tap = if (shiftDown) tap else null, warn = false)
        }
        val live = if (tap != null && tap.released && now - tap.startedAt > WINDOW_TICKS) null else tap
        if (rising && live != null && live.released && now - live.startedAt <= WINDOW_TICKS) {
            return Step(
                keepMounted = false,
                tap = Tap(live.startedAt, released = false, confirmed = true),
                warn = false,
            )
        }
        if (live == null) {
            return Step(keepMounted = true, tap = Tap(now, released = false), warn = true)
        }
        return Step(keepMounted = true, tap = live, warn = false)
    }

    /** Отпускание открывает окно второго нажатия. После окна попытка забывается. */
    fun afterShift(tap: Tap?, shiftDown: Boolean, now: Long): Tap? {
        if (tap == null) return null
        if (tap.confirmed) return if (shiftDown) tap else null
        val released = tap.released || !shiftDown
        if (released && now - tap.startedAt > WINDOW_TICKS) return null
        return if (released == tap.released) tap else tap.copy(released = released)
    }
}
