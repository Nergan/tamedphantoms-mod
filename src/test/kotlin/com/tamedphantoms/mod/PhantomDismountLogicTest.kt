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
    @DisplayName("Первое нажатие только открывает окно")
    fun firstTapWarns() {
        val tap = PhantomDismountLogic.onTap(now = 100, windowStart = null)
        assertFalse(tap.confirm)
        assertTrue(tap.warn)
        assertEquals(100L, tap.windowStart)
    }

    @Test
    @DisplayName("Повтор в тот же тик не считается вторым нажатием")
    fun sameTickDoesNotConfirm() {
        val tap = PhantomDismountLogic.onTap(now = 100, windowStart = 100)
        assertFalse(tap.confirm)
        assertFalse(tap.warn)
        assertEquals(100L, tap.windowStart)
    }

    @Test
    @DisplayName("Второе нажатие в ту же секунду подтверждает")
    fun secondTapInsideWindowConfirms() {
        val tap = PhantomDismountLogic.onTap(now = 110, windowStart = 100)
        assertTrue(tap.confirm)
        assertFalse(tap.warn)
        assertNull(tap.windowStart)
    }

    @Test
    @DisplayName("Нажатие ровно через секунду ещё подтверждает")
    fun tapAtWindowEdgeConfirms() {
        val tap = PhantomDismountLogic.onTap(now = 120, windowStart = 100)
        assertTrue(tap.confirm)
        assertNull(tap.windowStart)
    }

    @Test
    @DisplayName("Нажатие позже секунды снова первое")
    fun lateTapStartsOver() {
        val tap = PhantomDismountLogic.onTap(now = 121, windowStart = 100)
        assertFalse(tap.confirm)
        assertTrue(tap.warn)
        assertEquals(121L, tap.windowStart)
    }
}
