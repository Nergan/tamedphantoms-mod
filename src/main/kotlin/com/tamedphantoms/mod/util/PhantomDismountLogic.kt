package com.tamedphantoms.mod.util

/**
 * Два отдельных нажатия Shift за [WINDOW_TICKS] тиков.
 * Повтор вызова в том же удержании (каждый тик, пока клавиша зажата) вторым нажатием не считается:
 * между попытками должна быть пауза хотя бы в [GAP_TICKS] тиков.
 * Состояние клавиши здесь не используется: в момент снятия с ездового оно уже бывает ложным.
 */
object PhantomDismountLogic {

    const val WINDOW_TICKS = 20L
    const val GAP_TICKS = 4L

    data class Window(val startedAt: Long, val lastAttempt: Long)

    data class Step(val keepMounted: Boolean, val window: Window?, val warn: Boolean)

    fun onAttempt(high: Boolean, now: Long, window: Window?): Step {
        if (!high) return Step(keepMounted = false, window = null, warn = false)
        if (window == null) {
            return Step(keepMounted = true, window = Window(now, now), warn = true)
        }
        val age = now - window.startedAt
        val gap = now - window.lastAttempt
        if (gap >= GAP_TICKS) {
            if (age <= WINDOW_TICKS) {
                return Step(keepMounted = false, window = null, warn = false)
            }
            return Step(keepMounted = true, window = Window(now, now), warn = true)
        }
        return Step(keepMounted = true, window = window.copy(lastAttempt = now), warn = false)
    }
}
