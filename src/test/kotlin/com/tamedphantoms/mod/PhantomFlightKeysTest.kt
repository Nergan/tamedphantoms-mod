package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomFlightKeys
import com.tamedphantoms.mod.util.PhantomFlightKeys.CruiseChange
import com.tamedphantoms.mod.util.PhantomFlightKeys.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PhantomFlightKeysTest {

    private fun tapForwardTwice(keys: PhantomFlightKeys, start: Int = 0): List<CruiseChange> = listOf(
        keys.tick(start, Keys(forward = true), true).cruise,
        keys.tick(start + 1, Keys(), true).cruise,
        keys.tick(start + 3, Keys(forward = true), true).cruise,
    )

    @Test
    @DisplayName("Двойное «вперёд» включает круиз")
    fun doubleForwardStartsCruise() {
        val keys = PhantomFlightKeys()
        assertEquals(listOf(CruiseChange.NONE, CruiseChange.NONE, CruiseChange.STARTED), tapForwardTwice(keys))
        assertTrue(keys.cruising)
    }

    @Test
    @DisplayName("Медленное двойное нажатие круиз не включает")
    fun slowTapsDoNothing() {
        val keys = PhantomFlightKeys()
        keys.tick(0, Keys(forward = true), true)
        keys.tick(1, Keys(), true)
        assertEquals(CruiseChange.NONE, keys.tick(20, Keys(forward = true), true).cruise)
        assertFalse(keys.cruising)
    }

    @Test
    @DisplayName("Удержание второго нажатия круиз не гасит, новое нажатие гасит")
    fun newPressStopsCruise() {
        val keys = PhantomFlightKeys()
        tapForwardTwice(keys)
        assertEquals(CruiseChange.NONE, keys.tick(4, Keys(forward = true), true).cruise)
        assertEquals(CruiseChange.NONE, keys.tick(5, Keys(), true).cruise)
        assertTrue(keys.cruising)
        assertEquals(CruiseChange.STOPPED, keys.tick(40, Keys(forward = true), true).cruise)
        assertFalse(keys.cruising)
    }

    @Test
    @DisplayName("Любая другая клавиша движения возвращает управление")
    fun otherKeysStopCruise() {
        for (other in listOf(Keys(back = true), Keys(left = true), Keys(right = true), Keys(up = true), Keys(down = true), Keys(sneak = true))) {
            val keys = PhantomFlightKeys()
            tapForwardTwice(keys)
            keys.tick(10, Keys(), true)
            assertEquals(CruiseChange.STOPPED, keys.tick(30, other, true).cruise, other.toString())
        }
    }

    @Test
    @DisplayName("Посадка или вода гасят круиз")
    fun landingStopsCruise() {
        val keys = PhantomFlightKeys()
        tapForwardTwice(keys)
        assertEquals(CruiseChange.STOPPED, keys.tick(10, Keys(), false).cruise)
    }

    @Test
    @DisplayName("Двойные «вверх», «влево» и «вправо» дают петлю и бочки")
    fun maneuverTaps() {
        val keys = PhantomFlightKeys()
        keys.tick(0, Keys(up = true), true)
        keys.tick(1, Keys(), true)
        assertTrue(keys.tick(2, Keys(up = true), true).loop)
        keys.tick(10, Keys(left = true), true)
        keys.tick(11, Keys(), true)
        assertEquals(1, keys.tick(12, Keys(left = true), true).roll)
        keys.tick(20, Keys(right = true), true)
        keys.tick(21, Keys(), true)
        assertEquals(-1, keys.tick(22, Keys(right = true), true).roll)
    }

    @Test
    @DisplayName("Своя клавиша петли не следует за взлётом, а своя бочка — за шагом")
    fun dedicatedManeuverKeys() {
        val keys = PhantomFlightKeys()
        keys.tick(0, Keys(up = true, loop = false), true)
        keys.tick(1, Keys(loop = false), true)
        assertFalse(keys.tick(2, Keys(up = true, loop = false), true).loop)
        keys.tick(10, Keys(loop = true, up = false), true)
        keys.tick(11, Keys(), true)
        assertTrue(keys.tick(12, Keys(loop = true, up = false), true).loop)

        keys.tick(20, Keys(left = true, rollLeft = false), true)
        keys.tick(21, Keys(rollLeft = false), true)
        assertEquals(0, keys.tick(22, Keys(left = true, rollLeft = false), true).roll)
        keys.tick(30, Keys(rollRight = true, right = false), true)
        keys.tick(31, Keys(), true)
        assertEquals(-1, keys.tick(32, Keys(rollRight = true, right = false), true).roll)
    }

    @Test
    @DisplayName("Нажатие отдельной клавиши петли возвращает управление из круиза")
    fun dedicatedLoopStopsCruise() {
        val keys = PhantomFlightKeys()
        tapForwardTwice(keys)
        keys.tick(10, Keys(), true)
        assertEquals(CruiseChange.STOPPED, keys.tick(30, Keys(loop = true), true).cruise)
        assertFalse(keys.cruising)
    }
}
