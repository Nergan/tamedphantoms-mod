package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomQuat
import com.tamedphantoms.mod.util.PhantomRiderNeck
import com.tamedphantoms.mod.util.PhantomRiderSight
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

class PhantomRiderSightTest {

    private fun degrees(radians: Double) = Math.toDegrees(radians)

    private fun between(a: PhantomQuat, b: PhantomQuat) = degrees((a.conjugate() * b).angle())

    private fun camera(bodyYaw: Float, bodyPitch: Float, bank: Float, yaw: Float, pitch: Float) =
        PhantomRiderSight.worldTilt(bodyYaw, PhantomRiderSight.bodyTilt(bodyPitch, bank)) * PhantomRiderSight.look(yaw, pitch)

    @Test
    @DisplayName("Нос вверх уводит глаза назад по дуге вокруг центра фантома")
    fun noseUpMovesEyesBack() {
        // Курс 0 — нос на +Z.
        val eye = PhantomRiderSight.worldTilt(0f, PhantomRiderSight.bodyTilt(30f, 0f)).transform(0.0, 2.0, 0.0)
        assertEquals(0.0, eye[0], 1e-9)
        assertEquals(2.0 * cos(Math.toRadians(30.0)), eye[1], 1e-9)
        assertEquals(-2.0 * sin(Math.toRadians(30.0)), eye[2], 1e-9)
    }

    @Test
    @DisplayName("Ось наклона поворачивается вместе с курсом")
    fun tiltFollowsYaw() {
        // Курс 90 — нос на −X, назад — это +X.
        val eye = PhantomRiderSight.worldTilt(90f, PhantomRiderSight.bodyTilt(30f, 0f)).transform(0.0, 2.0, 0.0)
        assertEquals(2.0 * sin(Math.toRadians(30.0)), eye[0], 1e-9)
        assertEquals(0.0, eye[2], 1e-9)
    }

    @Test
    @DisplayName("Ровный корпус даёт ванильные углы камеры")
    fun levelBodyIsVanilla() {
        val angles = PhantomRiderSight.angles(camera(125f, 0f, 0f, 400f, -35f), 400f)
        assertEquals(400f, angles[0], 1e-3f)
        assertEquals(-35f, angles[1], 1e-3f)
        assertEquals(0f, angles[2], 1e-3f)
        // Ванильный взгляд: −sin(курс)·cos(тангаж), −sin(тангаж), cos(курс)·cos(тангаж).
        val forward = PhantomRiderSight.forward(camera(0f, 0f, 0f, 30f, 20f))
        assertEquals(-sin(Math.toRadians(30.0)) * cos(Math.toRadians(20.0)), forward[0], 1e-9)
        assertEquals(-sin(Math.toRadians(20.0)), forward[1], 1e-9)
        assertEquals(cos(Math.toRadians(30.0)) * cos(Math.toRadians(20.0)), forward[2], 1e-9)
    }

    @Test
    @DisplayName("Взгляд вдоль носа поднимается вместе с ним")
    fun viewPitchesWithNose() {
        val angles = PhantomRiderSight.angles(camera(0f, 30f, 0f, 0f, 0f), 0f)
        assertEquals(0f, angles[0], 1e-3f)
        assertEquals(-30f, angles[1], 1e-3f)
        assertEquals(0f, angles[2], 1e-3f)
    }

    @Test
    @DisplayName("Крен корпуса становится креном камеры, курс и тангаж остаются")
    fun bankRollsView() {
        val angles = PhantomRiderSight.angles(camera(40f, 0f, 25f, 40f, 0f), 40f)
        assertEquals(40f, angles[0], 1e-3f)
        assertEquals(0f, angles[1], 1e-3f)
        assertEquals(25f, angles[2], 1e-3f)
    }

    @Test
    @DisplayName("Углы камеры собирают тот же поворот, что и разложили")
    fun anglesRebuildRotation() {
        val random = Random(7)
        val rad = Math.PI / 180.0
        repeat(500) {
            val q = camera(
                random.nextFloat() * 720f - 360f,
                random.nextFloat() * 720f - 360f,
                random.nextFloat() * 720f - 360f,
                random.nextFloat() * 720f - 360f,
                random.nextFloat() * 180f - 90f,
            )
            val angles = PhantomRiderSight.angles(q, 0f)
            val rebuilt = PhantomQuat.y(Math.PI - angles[0] * rad) * PhantomQuat.x(-angles[1] * rad) * PhantomQuat.z(-angles[2] * rad)
            assertTrue(between(q, rebuilt) < 0.05, "angles ${angles.toList()}")
        }
    }

    @Test
    @DisplayName("В пределах шеи взгляд держит горизонт, за порогом корпус тянет его")
    fun neckHoldsThenFollows() {
        val neck = PhantomRiderNeck()
        repeat(60) { neck.step(PhantomRiderSight.bodyTilt(22f, 0f), 0.05, 30.0, 0.0) }
        assertEquals(0.0, degrees(neck.shown.angle()), 1e-6)
        repeat(60) { neck.step(PhantomRiderSight.bodyTilt(0f, 50f), 0.05, 30.0, 0.0) }
        assertEquals(20.0, degrees(neck.shown.angle()), 1e-3)
        repeat(60) { neck.step(PhantomRiderSight.bodyTilt(0f, 0f), 0.05, 30.0, 0.0) }
        assertEquals(0.0, degrees(neck.shown.angle()), 1e-3)
    }

    @Test
    @DisplayName("Порог 0 крепит взгляд к корпусу")
    fun zeroNeckIsRigid() {
        val neck = PhantomRiderNeck()
        val body = PhantomRiderSight.bodyTilt(35f, -20f)
        neck.step(body, 0.05, 0.0, 0.0)
        assertEquals(0.0, between(body, neck.shown), 1e-6)
    }

    @Test
    @DisplayName("Петля и бочка ведут взгляд без рывков, потом он выравнивается")
    fun figuresHaveNoJumps() {
        val frame = 1.0 / 60.0
        for (smoothing in listOf(0.0, 0.1, 0.4)) {
            for (rolling in listOf(false, true)) {
                val neck = PhantomRiderNeck()
                var previous = PhantomQuat.IDENTITY
                var worst = 0.0
                // Петля за полторы секунды, бочка за 0.8 с: 4 и 7.5 градуса за кадр.
                val frames = if (rolling) 48 else 90
                for (i in 0..frames + 180) {
                    val turn = if (i <= frames) 360f * i / frames else 0f
                    val body = if (rolling) PhantomRiderSight.bodyTilt(0f, turn) else PhantomRiderSight.bodyTilt(turn, 0f)
                    neck.step(body, frame, 30.0, smoothing)
                    worst = maxOf(worst, between(previous, neck.shown))
                    previous = neck.shown
                }
                assertTrue(worst < 15.0, "smoothing=$smoothing rolling=$rolling worst=$worst")
                assertEquals(0.0, degrees(neck.shown.angle()), 0.5, "smoothing=$smoothing rolling=$rolling")
            }
        }
    }

    @Test
    @DisplayName("Сильное сглаживание не отстаёт от корпуса больше чем на треть оборота")
    fun smoothingLagIsCapped() {
        val neck = PhantomRiderNeck()
        var worstLag = 0.0
        for (i in 0..60) {
            val body = PhantomRiderSight.bodyTilt(0f, 30f * i)
            neck.step(body, 1.0 / 60.0, 0.0, 0.4)
            worstLag = maxOf(worstLag, between(body, neck.shown))
        }
        assertTrue(worstLag <= 120.0 + 1e-6, "lag $worstLag")
        assertTrue(worstLag > 90.0, "lag $worstLag")
    }
}
