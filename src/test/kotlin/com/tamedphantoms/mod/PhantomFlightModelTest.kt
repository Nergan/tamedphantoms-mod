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

    /** Петля до окна рывка. Возвращает, сколько набрано высоты к этому моменту. */
    private fun loopUntilZoomWindow(model: PhantomFlightModel): Double {
        cruise(model, 40)
        var height = model.step(Input(1f, 0f, 1f, vc, loopRequest = true)).vertical
        var guard = 0
        while (!model.zoomReady) {
            assertTrue(model.looping, "петля кончилась раньше окна рывка")
            assertTrue(++guard < 120)
            height += model.step(Input(1f, 0f, 1f, vc)).vertical
        }
        return height
    }

    @Test
    @DisplayName("Рывок не начинается, пока нос в петле не посмотрит вниз")
    fun zoomWindowOpensLate() {
        val model = PhantomFlightModel()
        cruise(model, 40)
        model.step(Input(1f, 0f, 1f, vc, loopRequest = true))
        var startedFrom = Float.NaN
        repeat(90) {
            if (model.zooming) return@repeat
            val before = model.pathPitch
            model.step(Input(1f, 0f, 1f, vc, zoomRequest = true))
            if (model.zooming) startedFrom = before
        }
        assertTrue(model.zooming)
        assertTrue(startedFrom >= PhantomFlightModel.ZOOM_WINDOW - 5f, "рывок начался с угла $startedFrom")
    }

    @Test
    @DisplayName("Рывок из петли уносит вверх со сложенными крыльями, засчитывает петлю и затухает")
    fun zoomShootsUp() {
        val model = PhantomFlightModel()
        loopUntilZoomWindow(model)
        val kicked = model.step(Input(1f, 0f, 0f, vc, zoomRequest = true, zoomBoost = 2.0))
        assertTrue(model.zooming)
        var completed = 0
        var climb = if (kicked.vertical > 0.0) kicked.vertical else 0.0
        var maxPitch = PhantomFlightModel.wrap(model.pathPitch)
        var maxSpeed = model.speed
        var maxTuck = model.tuck
        var ticks = 1
        while (model.zooming && ticks < 200) {
            val out = model.step(Input(1f, 0f, 0f, vc))
            if (out.vertical > 0.0) climb += out.vertical
            if (model.completedLoop) completed++
            maxPitch = maxOf(maxPitch, PhantomFlightModel.wrap(model.pathPitch))
            maxSpeed = maxOf(maxSpeed, model.speed)
            maxTuck = maxOf(maxTuck, model.tuck)
            ticks++
        }
        assertEquals(1, completed, "петля засчитывается один раз")
        assertTrue(maxPitch > PhantomFlightModel.ZOOM_ANGLE - 1f, "нос $maxPitch")
        assertTrue(maxSpeed > vc * 2.1, "скорость $maxSpeed")
        assertEquals(PhantomFlightModel.ZOOM_TICKS, ticks, "окно рывка $ticks")
        assertTrue(maxTuck > 0.95f, "крылья $maxTuck")
        assertTrue(climb > vc * 40, "подъём $climb")
        assertFalse(model.zooming, "рывок затух за $ticks тиков")
        repeat(40) { model.step(Input(1f, 0f, 0f, vc)) }
        assertTrue(abs(PhantomFlightModel.wrap(model.pathPitch)) < 5f, "после рывка нос ${model.pathPitch}")
        assertTrue(model.tuck < 0.05f, "после рывка крылья ${model.tuck}")
    }

    @Test
    @DisplayName("Сила с сервера: выше толчок и тот же срок, 64 не срезается, 0 — рывка нет")
    fun zoomBoostScales() {
        data class Zoom(val ticks: Int, val climb: Double, val peak: Double, val end: Double)

        fun measure(boost: Double): Zoom {
            val model = PhantomFlightModel()
            loopUntilZoomWindow(model)
            val first = model.step(Input(1f, 0f, 0f, vc, zoomRequest = true, zoomBoost = boost))
            if (!model.zooming) return Zoom(0, 0.0, model.speed, model.speed)
            var climb = if (first.vertical > 0.0) first.vertical else 0.0
            var peak = model.speed
            var ticks = 1
            while (model.zooming && ticks < 400) {
                val out = model.step(Input(1f, 0f, 0f, vc, zoomBoost = boost))
                if (out.vertical > 0.0) climb += out.vertical
                peak = maxOf(peak, model.speed)
                ticks++
            }
            return Zoom(ticks, climb, peak, model.speed)
        }

        val off = measure(0.0)
        val usual = measure(8.0)
        val hard = measure(16.0)
        val hardest = measure(64.0)
        assertEquals(0, off.ticks)
        assertEquals(0.0, off.climb)
        assertEquals(PhantomFlightModel.ZOOM_TICKS, usual.ticks, "срок на 8: ${usual.ticks}")
        assertEquals(usual.ticks, hard.ticks, "срок на 16: ${hard.ticks}, на 8: ${usual.ticks}")
        assertEquals(usual.ticks, hardest.ticks, "срок на 64: ${hardest.ticks}, на 8: ${usual.ticks}")
        assertTrue(usual.peak > vc * 5.0, "толчок на 8: ${usual.peak}")
        assertTrue(hard.peak > usual.peak, "толчок на 16: ${hard.peak}, на 8: ${usual.peak}")
        assertTrue(hardest.peak > hard.peak * 2.0, "толчок на 64: ${hardest.peak}, на 16: ${hard.peak}")
        assertTrue(hard.climb > usual.climb, "подъём на 16: ${hard.climb}, на 8: ${usual.climb}")
        assertTrue(usual.end < usual.peak * 0.6, "скорость на 8 не спадает: ${usual.end} из ${usual.peak}")
        assertTrue(hard.end < hard.peak * 0.2, "скорость на 16 не спадает: ${hard.end} из ${hard.peak}")
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

    @Test
    @DisplayName("У земли без подъёма фантом садится, с подъёмом — взлетает")
    fun groundSettlesUnlessClimbing() {
        val settle = PhantomFlightModel().step(Input(0f, 0f, 0f, vc, medium = PhantomFlightModel.Medium.GROUND))
        assertTrue(settle.vertical < 0.0, "vertical ${settle.vertical}")
        val climb = PhantomFlightModel().step(Input(0f, 0f, 1f, vc, medium = PhantomFlightModel.Medium.GROUND))
        assertTrue(climb.vertical > 0.0, "vertical ${climb.vertical}")
    }

    @Test
    @DisplayName("Под водой петля и бочка идут, обычное плавание их не включает само")
    fun figuresSwim() {
        val water = PhantomFlightModel.Medium.WATER
        val swimming = PhantomFlightModel()
        repeat(30) { swimming.step(Input(1f, 0f, 0f, vc, medium = water)) }
        assertFalse(swimming.looping)
        assertFalse(swimming.rolling)
        assertTrue(swimming.speed >= vc * PhantomFlightModel.LOOP_ENTRY, "speed ${swimming.speed}")

        val loop = PhantomFlightModel()
        repeat(30) { loop.step(Input(1f, 0f, 0f, vc, medium = water)) }
        loop.step(Input(1f, 0f, 0f, vc, medium = water, loopRequest = true))
        assertTrue(loop.looping)
        repeat(5) { loop.step(Input(1f, 0f, 0f, vc, medium = water)) }
        assertTrue(loop.looping, "вода оборвала петлю")

        val roll = PhantomFlightModel()
        repeat(30) { roll.step(Input(1f, 0f, 0f, vc, medium = water)) }
        roll.step(Input(1f, 0f, 0f, vc, medium = water, rollRequest = 1))
        assertTrue(roll.rolling)
        repeat(4) { roll.step(Input(1f, 0f, 0f, vc, medium = water)) }
        assertTrue(roll.rolling, "вода оборвала бочку")
    }

    @Test
    @DisplayName("Один оборот с одним R даёт 0.25, крылья прижимаются по доле бонуса")
    fun rollBoostBuildsAndTucks() {
        val model = PhantomFlightModel()
        cruise(model, 40)
        model.step(Input(1f, 0f, 0f, vc, rollRequest = 1, rollBoostRequest = true, rollBoostLimit = 2.0, rollBoostStep = 0.25))
        repeat(PhantomFlightModel.ROLL_TICKS + 8) {
            model.step(Input(1f, 0f, 0f, vc, rollBoostLimit = 2.0, rollBoostStep = 0.25))
        }
        assertEquals(0.25, model.rollBoost, 0.05)
        assertTrue(model.tuck > 0.04f, "tuck ${model.tuck}")
        assertTrue(model.tuck < 0.4f, "tuck ${model.tuck}")
    }

    @Test
    @DisplayName("Набор нажатий доходит до потолка, а одного нажатия на втором обороте мало")
    fun rollBoostStopsAtLimitAndSecondPress() {
        val capped = PhantomFlightModel()
        cruise(capped, 40)
        mash(capped, 2.0)
        assertFalse(capped.rolling)
        assertEquals(2.0, capped.rollBoost, 0.01)
        repeat(50) { capped.step(Input(1f, 0f, 0f, vc, rollBoostLimit = 2.0, rollBoostStep = 0.25)) }
        assertTrue(capped.tuck > 0.85f, "tuck ${capped.tuck}")
        assertTrue(capped.speed > vc + 1.4, "speed ${capped.speed}")

        val early = PhantomFlightModel()
        cruise(early, 40)
        early.step(Input(1f, 0f, 0f, vc, rollRequest = 1, rollBoostRequest = true, rollBoostLimit = 2.0, rollBoostStep = 0.25))
        repeat(PhantomFlightModel.ROLL_TICKS + 40) {
            early.step(Input(1f, 0f, 0f, vc, rollBoostLimit = 2.0, rollBoostStep = 0.25))
        }
        assertFalse(early.rolling)
        assertTrue(early.rollBoost < 0.40, "second press must not add, boost ${early.rollBoost}")
        assertTrue(early.rollBoost > 0.08, "first step should remain, boost ${early.rollBoost}")
    }

    @Test
    @DisplayName("Набор высоты съедает бонус, петля и зависание ведут себя по своим правилам")
    fun rollBoostClimbDrainAndClears() {
        val climbing = charged(1.0)
        val before = climbing.rollBoost
        repeat(50) { climbing.step(Input(1f, 0f, 1f, vc, rollBoostLimit = 2.0)) }
        assertTrue(climbing.rollBoost < before - 0.15, "boost ${climbing.rollBoost}")
        assertTrue(climbing.rollBoost > 0.4, "boost ${climbing.rollBoost}")

        val loop = charged(0.6)
        val kept = loop.rollBoost
        loop.step(Input(1f, 0f, 0f, vc, loopRequest = true, rollBoostLimit = 2.0))
        var ticks = 0
        while (loop.looping && ticks++ < 90) {
            loop.step(Input(1f, 0f, 1f, vc, rollBoostLimit = 2.0))
        }
        assertEquals(kept, loop.rollBoost, 0.02)

        val hovering = charged(0.4)
        repeat(80) { hovering.step(Input(0f, 0f, 0f, vc, rollBoostLimit = 2.0)) }
        assertEquals(0.0, hovering.rollBoost, 0.001)

        val reverse = charged(0.4)
        reverse.step(Input(-1f, 0f, 0f, vc, rollBoostLimit = 2.0))
        assertEquals(0.0, reverse.rollBoost, 0.001)

        val strafe = charged(0.4)
        strafe.step(Input(0f, 1f, 0f, vc, rollBoostLimit = 2.0))
        assertEquals(0.0, strafe.rollBoost, 0.001)
    }

    @Test
    @DisplayName("На потолке обычная бочка остаётся, а приём на R ничего не добавляет")
    fun fullBoostRollDoesNotCharge() {
        val model = charged(2.0)
        assertFalse(model.rollBoostReady)
        model.step(Input(1f, 0f, 0f, vc, rollRequest = 1, rollBoostRequest = true, rollBoostLimit = 2.0))
        var guard = 0
        while (model.rolling && guard++ < 40) {
            model.step(Input(1f, 0f, 0f, vc, rollBoostRequest = true, rollBoostLimit = 2.0))
        }
        assertEquals(2.0, model.rollBoost, 0.02)
        assertFalse(model.rolling)
    }

    @Test
    @DisplayName("Следующие обороты короче, а в воде и в лаве набор высоты бонус не съедает")
    fun spinQuickensAndLiquidKeepsBoost() {
        fun ticksFor(spin: Double): Int {
            val model = PhantomFlightModel()
            cruise(model, 40)
            model.step(
                Input(1f, 0f, 0f, vc, rollRequest = 1, rollBoostRequest = true, rollBoostLimit = 8.0, rollSpin = spin, rollMinTicks = 4),
            )
            var ticks = 1
            while (model.rollBoost < 0.59 && ticks < 500) {
                model.step(
                Input(
                    1f, 0f, 0f, vc,
                    rollBoostRequest = model.rollBoostReady,
                    rollBoostLimit = 8.0,
                    rollBoostStep = 0.25,
                    rollSpin = spin,
                    rollMinTicks = 4,
                ),
            )
                ticks++
            }
            return ticks
        }
        assertTrue(ticksFor(0.55) < ticksFor(1.0))

        val swimming = charged(0.4)
        val kept = swimming.rollBoost
        repeat(80) {
            swimming.step(Input(1f, 0f, 1f, vc, medium = PhantomFlightModel.Medium.WATER, inLiquid = true, rollBoostLimit = 2.0))
        }
        assertEquals(kept, swimming.rollBoost, 0.001)
        repeat(80) { swimming.step(Input(1f, 0f, 1f, vc, inLiquid = true, rollBoostLimit = 2.0)) }
        assertEquals(kept, swimming.rollBoost, 0.02)
    }

    @Test
    @DisplayName("Отпущенное управление срезает бонус до обычной скорости, снижение его копит")
    fun brakeFollowsSpeedAndDiveBuilds() {
        val braking = charged(1.0)
        repeat(250) { braking.step(Input(0f, 0f, 0f, vc, rollBoostLimit = 2.0, rollBoostStep = 0.25)) }
        assertEquals(0.0, braking.rollBoost, 0.05)

        val diving = charged(0.5)
        val before = diving.rollBoost
        repeat(80) { diving.step(Input(1f, 0f, -1f, vc, rollBoostLimit = 2.0, rollBoostStep = 0.25)) }
        assertTrue(diving.rollBoost > before + 0.08, "boost ${diving.rollBoost}")
    }

    @Test
    @DisplayName("Бочка на рывке из петли поднимает потолок на единицу")
    fun barrelDuringZoomRaisesCap() {
        val model = PhantomFlightModel()
        loopUntilZoomWindow(model)
        model.step(Input(1f, 0f, 0f, vc, zoomRequest = true, zoomBoost = 2.0, rollBoostLimit = 2.0, rollBoostStep = 0.25))
        assertTrue(model.zooming)
        mash(model, 3.0)
        assertEquals(3.0, model.rollBoost, 0.05)
    }

    @Test
    @DisplayName("Под водой снижение на ctrl складывает крылья")
    fun waterDiveTucks() {
        val model = PhantomFlightModel()
        repeat(40) {
            model.step(Input(0f, 0f, -1f, vc, medium = PhantomFlightModel.Medium.WATER, rollBoostLimit = 2.0))
        }
        assertTrue(model.tuck > 0.8f, "tuck ${model.tuck}")
    }

    @Test
    @DisplayName("Прямой полёт тратит 0.1 бонуса за две секунды")
    fun straightFlightFadesATenthInTwoSeconds() {
        val model = charged(1.0)
        val before = model.rollBoost
        repeat(40) { model.step(Input(1f, 0f, 0f, vc, rollBoostLimit = 2.0)) }
        assertEquals(before - 0.1, model.rollBoost, 0.02)
    }

    @Test
    @DisplayName("Удар в воде сбрасывает бонус на выходе из жидкости")
    fun liquidImpactDropsBoostOnExit() {
        val swimming = charged(1.0)
        val kept = swimming.rollBoost
        swimming.step(Input(1f, 0f, 0f, vc, medium = PhantomFlightModel.Medium.WATER, inLiquid = true, rollBoostLimit = 2.0))
        swimming.noteLiquidImpact()
        repeat(20) {
            swimming.step(Input(1f, 0f, 0f, vc, medium = PhantomFlightModel.Medium.WATER, inLiquid = true, rollBoostLimit = 2.0))
        }
        assertEquals(kept, swimming.rollBoost, 0.05)
        swimming.leaveLiquid(vc)
        assertEquals(0.0, swimming.rollBoost, 0.001)
        assertTrue(kotlin.math.abs(swimming.speed) <= vc + 0.001)
    }

    private fun mash(model: PhantomFlightModel, until: Double) {
        var started = model.rolling
        var guard = 0
        while (model.rollBoost < until - 0.01 && guard++ < 900) {
            val start = !started
            model.step(
                Input(
                    1f, 0f, 0f, vc,
                    rollRequest = if (start) 1 else 0,
                    rollBoostRequest = start || model.rollBoostReady,
                    rollBoostLimit = 2.0,
                    rollBoostStep = 0.25,
                ),
            )
            started = true
            if (!model.rolling && model.rollBoost < until - 0.01) break
        }
        guard = 0
        while (model.rolling && guard++ < 80) {
            model.step(Input(1f, 0f, 0f, vc, rollBoostLimit = 2.0, rollBoostStep = 0.25))
        }
    }

    private fun charged(atLeast: Double): PhantomFlightModel {
        val model = PhantomFlightModel()
        cruise(model, 40)
        mash(model, atLeast)
        assertTrue(model.rollBoost >= atLeast - 0.05, "boost ${model.rollBoost}")
        assertFalse(model.rolling)
        return model
    }
}
