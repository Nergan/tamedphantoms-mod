package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomFlightAvoidance
import com.tamedphantoms.mod.util.PhantomFlightAvoidance.Probe
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.math.abs

class PhantomFlightAvoidanceTest {

    @Test
    @DisplayName("Чистый путь не меняет скорость")
    fun clearPathStays() {
        val steered = PhantomFlightAvoidance.steerAxes(0.0, 0.05, 0.4, preferLeft = true) { _, _, _ -> Probe.CLEAR }
        assertEquals(0.0, steered.x, 0.0001)
        assertEquals(0.05, steered.y, 0.0001)
        assertEquals(0.4, steered.z, 0.0001)
    }

    @Test
    @DisplayName("Столб впереди уводит в предпочитаемую сторону")
    fun pillarDodgesToThePreferredSide() {
        val left = PhantomFlightAvoidance.steerAxes(0.0, 0.0, 0.4, preferLeft = true, probe = ::blockedAhead)
        val right = PhantomFlightAvoidance.steerAxes(0.0, 0.0, 0.4, preferLeft = false, probe = ::blockedAhead)
        assertTrue(left.x < -0.2)
        assertTrue(abs(left.z) < 0.05)
        assertTrue(right.x > 0.2)
        assertTrue(abs(right.z) < 0.05)
    }

    @Test
    @DisplayName("Опасный блок впереди не оставляет прежний курс")
    fun hazardIsNotFlownInto() {
        val steered = PhantomFlightAvoidance.steerAxes(0.3, -0.05, 0.0, preferLeft = true) { forward, _, _ ->
            if (forward > 0.5) Probe.HAZARD else Probe.CLEAR
        }
        assertTrue(steered.x < 0.1)
        assertTrue(steered.y > -0.05)
    }

    private fun blockedAhead(forward: Double, side: Double, up: Double): Probe {
        if (abs(side) < 0.2 && up < 0.4 && forward > 0.6) return Probe.BLOCKED
        return Probe.CLEAR
    }
}
