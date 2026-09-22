package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomDismountLogic
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PhantomDismountLogicTest {

    @Test
    @DisplayName("Первое нажатие высоко над землёй только предупреждает")
    fun firstPressWarns() {
        val step = PhantomDismountLogic.onAttempt(
            high = true,
            shiftDown = true,
            rising = true,
            tap = null,
            now = 100,
        )
        assertTrue(step.keepMounted)
        assertTrue(step.warn)
        assertFalse(step.tap!!.released)
    }

    @Test
    @DisplayName("Удержание первого Shift не слезает и не считается вторым нажатием")
    fun holdingFirstPressStaysMounted() {
        val first = PhantomDismountLogic.onAttempt(true, true, true, null, 100).tap
        val held = PhantomDismountLogic.onAttempt(true, true, rising = false, first, 110)
        assertTrue(held.keepMounted)
        assertFalse(held.warn)
        assertTrue(held.tap!!.startedAt == 100L)
    }

    @Test
    @DisplayName("Второе нажатие в ту же секунду слезает")
    fun secondPressInsideWindowDismounts() {
        var tap = PhantomDismountLogic.onAttempt(true, true, true, null, 100).tap
        tap = PhantomDismountLogic.afterShift(tap, shiftDown = false, now = 105)
        val second = PhantomDismountLogic.onAttempt(true, true, rising = true, tap, 115)
        assertFalse(second.keepMounted)
        assertTrue(second.tap!!.confirmed)
        val stillHeld = PhantomDismountLogic.onAttempt(true, true, rising = false, second.tap, 116)
        assertFalse(stillHeld.keepMounted)
    }

    @Test
    @DisplayName("Второе нажатие позже секунды снова только предупреждает")
    fun secondPressAfterWindowDoesNotDismount() {
        var tap = PhantomDismountLogic.onAttempt(true, true, true, null, 100).tap
        tap = PhantomDismountLogic.afterShift(tap, shiftDown = false, now = 105)
        assertNull(PhantomDismountLogic.afterShift(tap, shiftDown = false, now = 121))
        val late = PhantomDismountLogic.onAttempt(true, true, rising = true, tap = null, now = 200)
        assertTrue(late.keepMounted)
        assertTrue(late.warn)
    }

    @Test
    @DisplayName("Просроченное окно не подтверждается, даже если тик отпускания его не стёр")
    fun expiredTapIsNotAConfirm() {
        val stale = PhantomDismountLogic.Tap(startedAt = 100, released = true)
        val late = PhantomDismountLogic.onAttempt(true, true, rising = true, stale, now = 200)
        assertTrue(late.keepMounted)
        assertTrue(late.warn)
        assertFalse(late.tap!!.confirmed)
    }

    @Test
    @DisplayName("Ниже порога высоты Shift слезает сразу")
    fun lowAltitudeDismountsImmediately() {
        val step = PhantomDismountLogic.onAttempt(high = false, shiftDown = true, rising = true, tap = null, now = 50)
        assertFalse(step.keepMounted)
        assertNull(step.tap)
    }
}
