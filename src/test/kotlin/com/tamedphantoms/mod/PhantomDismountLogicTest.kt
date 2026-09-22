package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomDismountLogic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PhantomDismountLogicTest {

    @Test
    @DisplayName("Первая попытка высоко над землёй только предупреждает")
    fun firstAttemptWarns() {
        val step = PhantomDismountLogic.onAttempt(high = true, now = 100, window = null)
        assertTrue(step.keepMounted)
        assertTrue(step.warn)
        assertEquals(100L, step.window!!.startedAt)
    }

    @Test
    @DisplayName("Повтор на следующем тике — то же удержание, не слезает")
    fun holdingDoesNotConfirm() {
        val first = PhantomDismountLogic.onAttempt(true, 100, null).window
        val next = PhantomDismountLogic.onAttempt(true, 101, first)
        assertTrue(next.keepMounted)
        assertFalse(next.warn)
        assertEquals(100L, next.window!!.startedAt)
    }

    @Test
    @DisplayName("Вторая попытка в ту же секунду после паузы слезает")
    fun secondAttemptInsideWindowDismounts() {
        val first = PhantomDismountLogic.onAttempt(true, 100, null).window
        val second = PhantomDismountLogic.onAttempt(true, 110, first)
        assertFalse(second.keepMounted)
        assertNull(second.window)
        assertFalse(second.warn)
    }

    @Test
    @DisplayName("Вторая попытка позже секунды снова только предупреждает")
    fun secondAttemptAfterWindowDoesNotDismount() {
        val first = PhantomDismountLogic.onAttempt(true, 100, null).window
        val late = PhantomDismountLogic.onAttempt(true, 130, first)
        assertTrue(late.keepMounted)
        assertTrue(late.warn)
        assertEquals(130L, late.window!!.startedAt)
    }

    @Test
    @DisplayName("Долгое удержание не продлевает окно и само не слезает")
    fun longHoldDoesNotBecomeSecondPress() {
        var window = PhantomDismountLogic.onAttempt(true, 0, null).window
        for (tick in 1L..40L) {
            val step = PhantomDismountLogic.onAttempt(true, tick, window)
            assertTrue(step.keepMounted)
            window = step.window
        }
        assertEquals(0L, window!!.startedAt)
        val after = PhantomDismountLogic.onAttempt(true, 50, window)
        assertTrue(after.keepMounted)
        assertTrue(after.warn)
    }

    @Test
    @DisplayName("Ниже порога высоты попытка слезает сразу")
    fun lowAltitudeDismountsImmediately() {
        val step = PhantomDismountLogic.onAttempt(high = false, now = 50, window = null)
        assertFalse(step.keepMounted)
        assertNull(step.window)
    }
}
