package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomFlightModel
import com.tamedphantoms.mod.util.PhantomFlightModel.Input
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.math.abs

class PhantomFlightModelTest {

    private val vc = 0.525

    private fun cruise(model: PhantomFlightModel, ticks: Int) {
        repeat(ticks) { model.step(Input(forward = 1f, strafe = 0f, climb = 0f, cruiseSpeed = vc)) }
    }

    @Test
    @DisplayName("Взмахи разгоняют из зависания почти до крейсерской примерно за секунду")
    fun acceleratesFromHover() {
        val model = PhantomFlightModel()
        cruise(model, 24)
        assertTrue(model.speed > vc * 0.88, "speed ${model.speed}")
        assertTrue(model.speed <= vc + 1.0E-9)
    }

    @Test
    @DisplayName("Набор высоты со взмахами держит скорость и поднимает фантома")
    fun climbKeepsSpeed() {
        val model = PhantomFlightModel()
        cruise(model, 40)
        var height = 0.0
        repeat(60) { height += model.step(Input(1f, 0f, 1f, vc)).vertical }
        assertTrue(model.speed > vc * 0.8, "speed ${model.speed}")
        assertTrue(height > vc * 20, "height $height")
        assertEquals(PhantomFlightModel.CLIMB_ANGLE, model.pathPitch, 0.01f)
    }

    @Test
    @DisplayName("Пике без взмахов складывает крылья и разгоняет выше крейсерской")
    fun stoopBuildsSpeed() {
        val model = PhantomFlightModel()
        cruise(model, 40)
        repeat(80) { model.step(Input(0f, 0f, -1f, vc)) }
        assertTrue(model.tuck > 0.95f, "tuck ${model.tuck}")
        assertTrue(model.speed > vc * 1.6, "speed ${model.speed}")
        assertTrue(model.speed <= vc * PhantomFlightModel.MAX_SPEED + 1.0E-9)
    }

    @Test
    @DisplayName("Скорость из пике держится несколько секунд после выхода в горизонт")
    fun stoopEnergyLasts() {
        val model = PhantomFlightModel()
        cruise(model, 40)
        repeat(80) { model.step(Input(0f, 0f, -1f, vc)) }
        cruise(model, 50)
        assertTrue(model.speed > vc * 1.15, "speed ${model.speed}")
        cruise(model, 200)
        assertTrue(model.speed < vc * 1.05, "speed ${model.speed}")
    }

    @Test
    @DisplayName("Без взмахов фантом планирует и переходит в зависание")
    fun glidesThenHovers() {
        val model = PhantomFlightModel()
        cruise(model, 40)
        var distance = 0.0
        var drop = 0.0
        repeat(80) {
            val out = model.step(Input(0f, 0f, 0f, vc))
            distance += out.forward
            drop += out.vertical
        }
        assertTrue(distance > 4.0, "distance $distance")
        assertTrue(drop < 0.0, "drop $drop")
        assertTrue(model.hover > 0.5f, "hover ${model.hover}")
        assertTrue(abs(model.speed) < vc * 0.2, "speed ${model.speed}")
    }

    @Test
    @DisplayName("Двойное «вверх» на скорости даёт петлю, она замыкается и тангаж возвращается к горизонту")
    fun loopCompletes() {
        val model = PhantomFlightModel()
        cruise(model, 40)
        var completed = false
        var maxPitch = 0f
        var height = 0.0
        model.step(Input(1f, 0f, 1f, vc, loopRequest = true))
        repeat(90) {
            height += model.step(Input(1f, 0f, 1f, vc)).vertical
            maxPitch = maxOf(maxPitch, model.pathPitch)
            if (model.completedLoop) completed = true
        }
        assertTrue(completed)
        assertTrue(maxPitch > 300f, "max pitch $maxPitch")
        assertFalse(model.looping)
        assertTrue(abs(PhantomFlightModel.wrap(model.pathPitch)) <= PhantomFlightModel.CLIMB_ANGLE + 1f)
    }

    @Test
    @DisplayName("Петля без скорости срывается в сваливание и не засчитывается")
    fun slowLoopStalls() {
        val model = PhantomFlightModel()
        repeat(11) { model.step(Input(1f, 0f, 0f, vc)) }
        assertTrue(model.speed >= vc * PhantomFlightModel.LOOP_ENTRY, "speed ${model.speed}")
        model.step(Input(1f, 0f, 0f, vc, loopRequest = true))
        assertTrue(model.looping)
        var completed = false
        repeat(120) {
            model.step(Input(0f, 0f, 0f, vc))
            if (model.completedLoop) completed = true
        }
        assertFalse(completed)
        assertFalse(model.looping)
    }

    @Test
    @DisplayName("Двойное «влево» крутит полную бочку с рывком влево")
    fun rollJukes() {
        val model = PhantomFlightModel()
        cruise(model, 40)
        var lateral = 0.0
        var deepest = 0f
        model.step(Input(1f, 0f, 0f, vc, rollRequest = 1)).also { lateral += it.lateral }
        repeat(PhantomFlightModel.ROLL_TICKS) {
            lateral += model.step(Input(1f, 0f, 0f, vc)).lateral
            deepest = minOf(deepest, model.roll)
        }
        assertTrue(deepest < -300f, "roll $deepest")
        assertTrue(lateral > 1.0, "lateral $lateral")
        assertTrue(abs(model.roll) < 30f, "roll after ${model.roll}")
    }

    @Test
    @DisplayName("Задний ход тормозит и уводит назад")
    fun reverseBrakes() {
        val model = PhantomFlightModel()
        cruise(model, 40)
        repeat(30) { model.step(Input(-1f, 0f, 0f, vc)) }
        assertTrue(model.speed < 0.0, "speed ${model.speed}")
        assertTrue(model.hover > 0.9f)
    }

    @Test
    @DisplayName("На земле фантом идёт заметно медленнее полёта")
    fun groundIsSlow() {
        val model = PhantomFlightModel()
        repeat(40) { model.step(Input(1f, 0f, 0f, vc, medium = PhantomFlightModel.Medium.GROUND)) }
        assertEquals(vc * PhantomFlightModel.GROUND_SPEED, model.speed, vc * 0.02)
    }
}
