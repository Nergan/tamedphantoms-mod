package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomAcrobatics
import com.tamedphantoms.mod.util.PhantomCrawl
import com.tamedphantoms.mod.util.PhantomFlightAttitude
import com.tamedphantoms.mod.util.PhantomFlightPace
import com.tamedphantoms.mod.util.PhantomHeadLook
import com.tamedphantoms.mod.util.PhantomHover
import com.tamedphantoms.mod.util.PhantomShake
import com.tamedphantoms.mod.util.PhantomSitFlutter
import com.tamedphantoms.mod.util.PhantomTailBend
import com.tamedphantoms.mod.util.PhantomWetness
import com.tamedphantoms.mod.util.PhantomWingbeat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PhantomFlightPaceTest {

    @Test
    @DisplayName("Множитель 2.0 сохраняет прежнюю скорость, 1.25 берёт 0.625, под водой ещё ×0.5")
    fun paceScalesFromDoubleBaseline() {
        assertEquals(1f, PhantomFlightPace.paceOf(2.0), 0.0001f)
        assertEquals(0.625f, PhantomFlightPace.paceOf(1.25), 0.0001f)
        assertEquals(0.5f, PhantomFlightPace.paceOf(1.0), 0.0001f)
        assertEquals(0.5, PhantomFlightPace.waterScale(true), 0.0001)
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
    @DisplayName("Зависание набирается примерно за секунду и даёт 22 градуса и взмах ×1.75")
    fun blendPitchAndFlap() {
        var blend = 0f
        repeat(20) { blend = PhantomHover.step(blend, moving = false) }
        assertTrue(blend > 0.85f)

        blend = PhantomHover.step(1f, moving = true)
        assertTrue(blend < 1f)

        assertEquals(22f, PhantomHover.pitchOffset(1f), 0.001f)
        assertEquals(0f, PhantomHover.pitchOffset(0f), 0.001f)
        assertEquals(1.75f, PhantomHover.flapRate(1f), 0.001f)
        assertEquals(1f, PhantomHover.flapRate(0f), 0.001f)
        assertEquals(0.75, PhantomHover.REVERSE_SPEED, 0.0001)
    }

    @Test
    @DisplayName("Клевок корпуса — это разница между наклоном тела и прицелом пилота")
    fun noseUpComesFromBodyPitch() {
        assertEquals(0f, PhantomHover.noseUpDegrees(40f, 20f), 0.001f)
        assertEquals(22f, PhantomHover.noseUpDegrees(0f, 22f), 0.001f)
        assertEquals(0f, PhantomHover.blendFromPitches(40f, 20f), 0.001f)
        assertEquals(1f, PhantomHover.blendFromPitches(0f, 22f), 0.001f)
    }
}

class PhantomPoseTest {

    @Test
    @DisplayName("Хвост гнётся против поворота")
    fun tailBendsOppositeTheTurn() {
        assertTrue(PhantomTailBend.targetRadians(12f) < 0f)
        assertTrue(PhantomTailBend.targetRadians(-12f) > 0f)
        assertEquals(0f, PhantomTailBend.targetRadians(0f), 0.0001f)
    }

    @Test
    @DisplayName("Взгляд в цель на одной высоте не включает поклон в 11 градусов")
    fun trackingLooksAtEyes() {
        val tracking = PhantomHeadLook.headPitchRadians(0f, upsideDown = false, tracking = true)
        val idle = PhantomHeadLook.headPitchRadians(0f, upsideDown = false, tracking = false)
        assertEquals(0f, tracking, 0.0001f)
        assertEquals(PhantomHeadLook.REST_PITCH, idle, 0.0001f)
    }

    @Test
    @DisplayName("Лёжа, крылья по очереди поднимаются редкой фразой")
    fun sittingWingsAlternate() {
        assertTrue(PhantomSitFlutter.lift(18f, 0) > 0.1f)
        assertEquals(0f, PhantomSitFlutter.lift(18f, 1), 0.0001f)
        assertTrue(PhantomSitFlutter.lift(40f, 1) > 0.1f)
        assertEquals(0f, PhantomSitFlutter.lift(40f, 0), 0.0001f)
        assertEquals(0f, PhantomSitFlutter.lift(200f, 0), 0.0001f)
        assertEquals(0f, PhantomSitFlutter.lift(200f, 1), 0.0001f)
    }

    @Test
    @DisplayName("Отряхивание начинается и кончается спокойно")
    fun shakeEnvelope() {
        assertTrue(PhantomShake.envelope(PhantomShake.LENGTH, 0f) < 0.15f)
        assertTrue(PhantomShake.envelope(PhantomShake.LENGTH / 2, 0f) > 0.9f)
        assertEquals(0f, PhantomShake.envelope(0, 0f), 0.0001f)
    }
}

class PhantomWingbeatTest {

    @Test
    @DisplayName("Крейсер реже, набор глубже, планирование почти без взмахов, зависание остаётся ×1.75")
    fun regimes() {
        val cruise = PhantomWingbeat.pose(0.0, 0.6, 0f, 0f, 0f, 0f)
        assertEquals(PhantomWingbeat.CRUISE_RATE, cruise.rate, 0.001f)
        assertTrue(cruise.amplitude < 1f)

        val climb = PhantomWingbeat.pose(0.3, 0.6, 0f, 0f, 0f, 0f)
        assertEquals(PhantomWingbeat.CLIMB_RATE, climb.rate, 0.001f)
        assertTrue(climb.amplitude > 1.2f)

        val glide = PhantomWingbeat.pose(0.0, 0.6, 0f, 0f, 0f, 1f)
        assertEquals(PhantomWingbeat.GLIDE_RATE, glide.rate, 0.001f)
        assertTrue(glide.amplitude < 0.15f)
        assertEquals(0f, glide.droop, 0.001f)

        val hover = PhantomWingbeat.pose(0.0, 0.0, 1f, 0f, 0f, 1f)
        assertEquals(PhantomHover.flapRate(1f), hover.rate, 0.001f)
    }

    @Test
    @DisplayName("У земли крыло опущено, отрыв убирает опускание и усиливает взмах")
    fun groundAndTakeoff() {
        val skim = PhantomWingbeat.pose(0.0, 0.2, 0f, 1f, 0f, 0f)
        assertEquals(PhantomWingbeat.GROUND_DROOP, skim.droop, 0.001f)
        assertEquals(1f, skim.tipHang, 0.001f)
        assertTrue(skim.amplitude < 0.05f)

        val launch = PhantomWingbeat.pose(0.2, 0.2, 0f, 1f, 1f, 0f)
        assertEquals(0f, launch.droop, 0.001f)
        assertEquals(PhantomWingbeat.TAKEOFF_RATE, launch.rate, 0.001f)
        assertEquals(1f, PhantomWingbeat.stepTakeoff(0.2f, launch = true), 0.001f)

        val swim = PhantomWingbeat.pose(0.3, 0.6, 1f, 0f, 0f, 0f, swim = 1f)
        assertTrue(swim.rate < 0.25f)
        assertTrue(swim.amplitude < 0.1f)
    }
}

class PhantomFlightAttitudeTest {

    @Test
    @DisplayName("Чистое снижение и задний ход держат нос в зависании, ход вперёд при снижении опускает его")
    fun pitchFollowsFlightNotLook() {
        assertTrue(PhantomFlightAttitude.wantsHover(0.0, 0.0))
        assertTrue(PhantomFlightAttitude.wantsHover(-0.3, 0.2))
        assertTrue(!PhantomFlightAttitude.wantsHover(0.3, 0.0))
        assertTrue(!PhantomFlightAttitude.wantsHover(0.0, 0.3))

        val hover = PhantomFlightAttitude.pose(0.0, 0.0, -0.4, hoverBlend = 1f)
        assertEquals(PhantomHover.PITCH_DEGREES, hover.pitch, 0.001f)
        assertEquals(0f, hover.bank, 0.001f)

        val dive = PhantomFlightAttitude.pose(0.4, 0.0, -0.4, hoverBlend = 0f)
        assertTrue(dive.pitch < 0f)
        assertTrue(dive.pitch > -PhantomFlightAttitude.MOVE_PITCH - 0.01f)

        val climb = PhantomFlightAttitude.pose(0.4, 0.0, 0.4, hoverBlend = 0f)
        assertTrue(climb.pitch > 0f)
        assertTrue(climb.pitch < PhantomHover.PITCH_DEGREES)

        val bank = PhantomFlightAttitude.pose(0.4, 0.4, 0.0, hoverBlend = 0f)
        assertEquals(-PhantomFlightAttitude.BANK, bank.bank, 0.001f)
        assertEquals(-PhantomFlightAttitude.HEAD_YAW, bank.headYaw, 0.001f)
        assertTrue(kotlin.math.abs(bank.headYaw) <= PhantomFlightAttitude.HEAD_YAW)

        assertTrue(PhantomFlightAttitude.tailPitchDegrees(0.3) < -8f)
        assertTrue(PhantomFlightAttitude.tailPitchDegrees(-0.3) > 8f)
        assertEquals(0f, PhantomFlightAttitude.tailPitchDegrees(0.0), 0.001f)
    }
}

class PhantomAcrobaticsTest {

    @Test
    @DisplayName("В прямом полёте наклон копится до полной петли, в зависании и назад — нет")
    fun loopOnlyWhileFlyingForward() {
        var pitch = 0f
        var roll = 0f
        var loop = 0f
        var done = false
        repeat(59) {
            val step = PhantomAcrobatics.step(pitch, roll, loop, acrobatic = true, climb = 1f, strafe = 0f)
            pitch = step.pitch
            roll = step.roll
            loop = step.loop
            done = step.completedLoop
        }
        assertTrue(!done)
        assertTrue(pitch > PhantomFlightAttitude.MOVE_PITCH)

        val finished = PhantomAcrobatics.step(pitch, roll, loop, acrobatic = true, climb = 1f, strafe = 0f)
        assertTrue(finished.completedLoop)

        val hover = PhantomAcrobatics.step(40f, 30f, 40f, acrobatic = false, climb = 1f, strafe = 1f)
        assertTrue(!hover.completedLoop)
        assertTrue(hover.pitch < 40f)
        assertTrue(hover.roll < 30f)
        assertTrue(!PhantomAcrobatics.allows(0.0, 0.0, crawling = false, sitting = false, ridden = true))
        assertTrue(!PhantomAcrobatics.allows(-0.4, 0.0, crawling = false, sitting = false, ridden = true))
        assertTrue(PhantomAcrobatics.allows(0.4, 0.0, crawling = false, sitting = false, ridden = true))

        var barrelRoll = 0f
        repeat(5) {
            barrelRoll = PhantomAcrobatics.step(0f, barrelRoll, 0f, acrobatic = true, climb = 0f, strafe = 1f).roll
        }
        assertTrue(barrelRoll < -PhantomFlightAttitude.BANK)
    }
}

class PhantomCrawlTest {

    @Test
    @DisplayName("Шаг лап чаще при большей скорости из настроек и стоит на месте")
    fun stepFollowsConfiguredSpeed() {
        val slow = PhantomCrawl.advance(0.4, 0.5)
        val normal = PhantomCrawl.advance(0.4, 1.25)
        val fast = PhantomCrawl.advance(0.4, 2.5)
        assertTrue(normal > slow)
        assertTrue(fast > normal)
        assertEquals(0f, PhantomCrawl.advance(0.0, 2.5), 0.0001f)
    }
}

class PhantomWetnessTest {

    @Test
    @DisplayName("Отряхивание после дождя и воды, но не в первый тик и не когда дождь сменился водой")
    fun rainAndWater() {
        val (seen, first) = PhantomWetness.step(PhantomWetness.Memory(), inWater = false, inRain = true)
        assertTrue(!first)

        val (dry, shook) = PhantomWetness.step(seen, inWater = false, inRain = false)
        assertTrue(shook)
        assertTrue(!dry.inRain)

        val (swimming, intoWater) = PhantomWetness.step(seen, inWater = true, inRain = false)
        assertTrue(!intoWater)

        val (ashore, leftWater) = PhantomWetness.step(swimming, inWater = false, inRain = false)
        assertTrue(leftWater)
        assertTrue(!ashore.inWater)
    }
}
