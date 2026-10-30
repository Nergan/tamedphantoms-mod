package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomEffectSpeed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PhantomEffectSpeedTest {
    @Test
    fun speedSlownessAndInvertedStick() {
        assertEquals(1.0, PhantomEffectSpeed.movementScale(null, null), 0.001)
        assertEquals(1.2, PhantomEffectSpeed.movementScale(0, null), 0.001)
        assertEquals(1.4, PhantomEffectSpeed.movementScale(1, null), 0.001)
        assertEquals(0.85, PhantomEffectSpeed.movementScale(null, 0), 0.001)
        assertEquals(1.02, PhantomEffectSpeed.movementScale(0, 0), 0.001)
        assertEquals(-1f, PhantomEffectSpeed.axis(1f, true))
        assertEquals(1.0, PhantomEffectSpeed.axis(1.0, false), 0.001)
    }
}
