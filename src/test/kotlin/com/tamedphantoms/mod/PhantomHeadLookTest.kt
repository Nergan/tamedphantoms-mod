package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomHeadLook
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.math.abs

class PhantomHeadLookTest {

    @Test
    @DisplayName("Взгляд строго вперёд по +Z — нулевое рыскание")
    fun yawStraightAheadIsZero() {
        val yaw = PhantomHeadLook.yawDegrees(0.0, 0.0, 0.0, 5.0)
        assertEquals(0f, yaw, 0.01f)
    }

    @Test
    @DisplayName("Цель выше глаз — отрицательный тангаж, как у ванили (взгляд вверх)")
    fun pitchAboveEyesLooksUp() {
        val pitch = PhantomHeadLook.pitchDegrees(0.0, 1.0, 0.0, 0.0, 3.0, 4.0)
        assertTrue(pitch < 0f)
    }

    @Test
    @DisplayName("Перевёрнутая модель меняет знак и рыскания, и тангажа")
    fun upsideDownFlipsBothAxes() {
        assertEquals(-20f, PhantomHeadLook.modelYawDegrees(20f, upsideDown = true))
        assertEquals(15f, PhantomHeadLook.modelPitchDegrees(-15f, upsideDown = true))
        assertEquals(20f, PhantomHeadLook.modelYawDegrees(20f, upsideDown = false))
        assertEquals(-15f, PhantomHeadLook.modelPitchDegrees(-15f, upsideDown = false))
    }

    @Test
    @DisplayName("Поворот головы не перескакивает цель за один шаг")
    fun approachDoesNotOvershoot() {
        assertEquals(10f, PhantomHeadLook.approachDegrees(0f, 30f, 10f))
        assertEquals(-4f, PhantomHeadLook.approachDegrees(0f, -4f, 10f))
    }

    @Test
    @DisplayName("Короткий путь через 180°, а не разворот на 340°")
    fun approachTakesShortArc() {
        val next = PhantomHeadLook.approachDegrees(170f, -170f, 10f)
        assertEquals(180f, abs(PhantomHeadLook.wrapDegrees(next)), 0.01f)
    }

    @Test
    @DisplayName("Голова не уходит дальше лимита от тела")
    fun clampKeepsHeadNearBody() {
        val clamped = PhantomHeadLook.clampRelative(0f, 120f, 75f)
        assertEquals(75f, clamped, 0.01f)
        assertTrue(abs(PhantomHeadLook.wrapDegrees(clamped)) <= 75f)
    }
}
