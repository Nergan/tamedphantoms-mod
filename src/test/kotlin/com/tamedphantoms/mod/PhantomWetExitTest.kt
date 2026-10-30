package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomWetness
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PhantomWetExitTest {

    private fun tracked(inWater: Boolean, inRain: Boolean) =
        PhantomWetness.Memory(tracked = true, inWater = inWater, inRain = inRain)

    @Test
    @DisplayName("Из воды на сухой воздух — отряхивается. Снег и пустыня для игры не дождь")
    fun leavingWaterIntoDryAirShakes() {
        assertTrue(PhantomWetness.step(tracked(inWater = true, inRain = false), inWater = false, inRain = false).second)
    }

    @Test
    @DisplayName("Из воды прямо под дождь — не отряхивается, но отряхнётся, выйдя из-под дождя")
    fun leavingWaterIntoRainWaits() {
        val (memory, shake) = PhantomWetness.step(tracked(inWater = true, inRain = false), inWater = false, inRain = true)
        assertFalse(shake)
        assertTrue(PhantomWetness.step(memory, inWater = false, inRain = false).second)
    }

    @Test
    @DisplayName("Первый замер ничего не запускает")
    fun firstSampleIsSilent() {
        assertFalse(PhantomWetness.step(PhantomWetness.Memory(), inWater = false, inRain = false).second)
    }

    @Test
    @DisplayName("Из дождя в воду — не отряхивается")
    fun rainIntoWaterWaits() {
        assertFalse(PhantomWetness.step(tracked(inWater = false, inRain = true), inWater = true, inRain = false).second)
    }
}
