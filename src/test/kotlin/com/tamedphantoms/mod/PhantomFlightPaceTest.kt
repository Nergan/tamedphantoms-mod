package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomFlightPace
import com.tamedphantoms.mod.util.PhantomHover
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PhantomFlightPaceTest {

    @Test
    @DisplayName("Множитель 2.0 сохраняет прежнюю скорость, 1.25 берёт 0.625, под водой ещё ×0.8")
    fun paceScalesFromDoubleBaseline() {
        assertEquals(1f, PhantomFlightPace.paceOf(2.0), 0.0001f)
        assertEquals(0.625f, PhantomFlightPace.paceOf(1.25), 0.0001f)
        assertEquals(0.5f, PhantomFlightPace.paceOf(1.0), 0.0001f)
        assertEquals(0.8, PhantomFlightPace.waterScale(true), 0.0001)
        assertEquals(1.0, PhantomFlightPace.waterScale(false), 0.0001)
    }

    @Test
    @DisplayName("Свои фантомы не быстрее сервера, освобождённые сервер не убавляют")
    fun ownerPreferenceCannotExceedServer() {
        assertEquals(1.0, PhantomFlightPace.multiple(true, 1.5, 1.0), 0.0001)
        assertEquals(1.5, PhantomFlightPace.multiple(true, 1.5, 3.0), 0.0001)
        assertEquals(1.5, PhantomFlightPace.multiple(true, 1.5, null), 0.0001)
        assertEquals(1.5, PhantomFlightPace.multiple(false, 1.5, 0.5), 0.0001)
    }
}

class PhantomHoverTest {

    @Test
    @DisplayName("Зависание набирается примерно за секунду и даёт 20 градусов и взмах ×1.5")
    fun blendPitchAndFlap() {
        var blend = 0f
        repeat(20) { blend = PhantomHover.step(blend, moving = false) }
        assertTrue(blend > 0.85f)

        blend = PhantomHover.step(1f, moving = true)
        assertTrue(blend < 1f)

        assertEquals(-20f, PhantomHover.pitchOffset(1f), 0.001f)
        assertEquals(0f, PhantomHover.pitchOffset(0f), 0.001f)
        assertEquals(1.5f, PhantomHover.flapRate(1f), 0.001f)
        assertEquals(1f, PhantomHover.flapRate(0f), 0.001f)
    }

    @Test
    @DisplayName("Клевок носом — это разница между прицелом пилота и наклоном тела")
    fun noseUpComesFromBodyPitch() {
        assertEquals(0f, PhantomHover.noseUpDegrees(40f, 20f), 0.001f)
        assertEquals(20f, PhantomHover.noseUpDegrees(40f, 0f), 0.001f)
        assertEquals(0f, PhantomHover.blendFromPitches(40f, 20f), 0.001f)
        assertEquals(1f, PhantomHover.blendFromPitches(40f, 0f), 0.001f)
    }
}
