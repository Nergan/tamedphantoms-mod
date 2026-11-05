package com.tamedphantoms.mod.util

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

/**
 * Полёт верхом. Скорость — состояние, а не мгновенная цель:
 * набор высоты тратит её, пикирование копит, взмахи разгоняют только до
 * крейсерской. Без взмахов фантом планирует и постепенно встаёт в зависание.
 *
 * Петля — настоящий круг траектории в плоскости курса, с потерей скорости
 * на подъёме. Не хватило скорости до точки невозврата — сваливание, нос вниз.
 * Бочка — быстрый оборот вокруг продольной оси с боковым рывком.
 * Пикирование без взмахов складывает крылья и разгоняет сильнее.
 * Рывок вверх — выход из петли не в горизонт, а почти отвесно, со сложенными крыльями
 * и толчком, который тратится на подъёме, как разгон пике.
 *
 * Знаки как у остальной позы фантома: тангаж плюс — нос вверх,
 * крен минус — то же направление, что стрейф влево.
 */
class PhantomFlightModel {

    enum class Medium { AIR, GROUND, WATER }

    data class Input(
        /** −1..1, «вперёд» плюс. */
        val forward: Float,
        /** −1..1, влево плюс (`xxa`). */
        val strafe: Float,
        /** −1, 0 или 1. */
        val climb: Float,
        /** Крейсерская скорость в блоках за тик с учётом всех множителей. */
        val cruiseSpeed: Double,
        /** Насколько курс повернулся за тик, градусы. */
        val yawDelta: Float = 0f,
        val medium: Medium = Medium.AIR,
        /** Доля подъёма при взлёте с поверхности, 1 — обычный полёт. */
        val lift: Double = 1.0,
        /** Двойное «вверх». */
        val loopRequest: Boolean = false,
        /** Двойное «влево» (+1) или «вправо» (−1). */
        val rollRequest: Int = 0,
        /** Рывок вверх, пока открыто [zoomReady]. */
        val zoomRequest: Boolean = false,
        /** Сила рывка с сервера. 8 — обычная, 0 — рывка нет. */
        val zoomBoost: Double = 1.0,
        /** R во время бочки: начать набор или закончить его на текущем обороте. */
        val rollBoostRequest: Boolean = false,
        /** Потолок бонуса скорости за бочку, блоки за тик. 0 — набора нет. */
        val rollBoostLimit: Double = 2.0,
        /** Сколько блоков за тик даёт один полный оборот. 0 — приёма нет. */
        val rollBoostStep: Double = 0.2,
        /** Сколько бонуса съедает один тик набора высоты. */
        val rollClimbDrain: Double = 0.2 / 40.0,
        /** Длительность каждого следующего оборота — эта доля от предыдущего. 1 — без ускорения. */
        val rollSpin: Double = 0.85,
        /** Самый короткий оборот заряженной бочки, в тиках. */
        val rollMinTicks: Int = 6,
        /** Вода или лава: бонус не тает сам и не тает на наборе высоты. */
        val inLiquid: Boolean = false,
    )

    data class Output(
        val forward: Double,
        val lateral: Double,
        val vertical: Double,
    )

    /** Воздушная скорость вдоль траектории, блоки за тик. Минус — задний ход. */
    var speed: Double = 0.0
        private set

    /** Угол траектории, градусы. В петле уходит дальше ±90. */
    var pathPitch: Float = 0f
        private set

    /** Крен корпуса, градусы. */
    var roll: Float = 0f
        private set

    /** 0 — крылья раскрыты, 1 — сложены в пике. */
    var tuck: Float = 0f
        private set

    /** 0..1: насколько тяжело работают крылья в этом тике. */
    var effort: Float = 0f
        private set

    /** 1 — висит на месте и машет, как в зависании. */
    var hover: Float = 1f
        private set

    /** Идёт петля, точка невозврата пройдена или нет. */
    var looping: Boolean = false
        private set

    /** Тик, когда петля только что замкнулась. */
    var completedLoop: Boolean = false
        private set

    /** Идёт бочка. */
    val rolling: Boolean
        get() = rollLeft > 0f

    /**
     * Бонус скорости после заряженных бочек, блоки за тик.
     * Держится, пока фантом летит вперёд. Набор высоты съедает его понемногу.
     */
    var rollBoost: Double = 0.0
        private set

    /** Во время бочки можно нажать R и начать копить скорость. На потолке приёма уже нет. */
    val rollBoostReady: Boolean
        get() = rollLeft > 0f && !rollCharging && chargeOpen()

    /** Бочка уже копится: повторное R доделывает текущий оборот и отпускает фантома. */
    val rollBoostCharging: Boolean
        get() = rollCharging

    /** Идёт рывок вверх: крылья сложены, нос тянется почти отвесно. */
    var zooming: Boolean = false
        private set

    /** Тик, когда рывок только что начался. */
    var zoomStarted: Boolean = false
        private set

    /** Можно рвануть вверх: в петле нос уже смотрит вниз, или петля только что замкнулась. */
    val zoomReady: Boolean
        get() = !zooming && ((looping && loopTurned >= ZOOM_WINDOW) || zoomGrace > 0)

    private var loopTurned = 0f
    private var loopDirection = 1f
    private var rollLeft = 0f
    private var rollDirection = 0f
    private var rollTicks = 0
    private var rollCharging = false
    private var rollFinish = false
    private var rollSpan = ROLL_TICKS
    private var boostLimit = 2.0
    private var boostStep = 0.2
    private var climbDrain = 0.2 / 40.0
    private var rollSpin = 0.85
    private var rollMinTicks = 6
    private var stalled = false
    private var zoomTicks = 0
    /** Скорость сразу после толчка. За окно рывка она линейно сходится к обычной. */
    private var zoomPeak = 0.0
    private var zoomTarget = 0f
    private var zoomLoopLeft = 0f
    private var zoomGrace = 0

    fun reset() {
        speed = 0.0
        pathPitch = 0f
        roll = 0f
        tuck = 0f
        effort = 0f
        hover = 1f
        looping = false
        completedLoop = false
        loopTurned = 0f
        rollLeft = 0f
        rollTicks = 0
        rollCharging = false
        rollFinish = false
        rollSpan = ROLL_TICKS
        rollBoost = 0.0
        stalled = false
        endZoom()
    }

    /** Удар о стену гасит скорость и прерывает фигуры. Уже набранный бонус остаётся. */
    fun onCollision() {
        speed *= 0.45
        looping = false
        loopTurned = 0f
        abortRoll()
        endZoom()
        pathPitch = wrap(pathPitch).coerceIn(-45f, 45f)
    }

    fun step(input: Input): Output {
        completedLoop = false
        zoomStarted = false
        boostLimit = input.rollBoostLimit.coerceIn(0.0, 64.0)
        boostStep = input.rollBoostStep.coerceIn(0.0, 8.0)
        climbDrain = input.rollClimbDrain.coerceAtLeast(0.0)
        rollSpin = input.rollSpin.coerceIn(0.5, 1.0)
        rollMinTicks = input.rollMinTicks.coerceIn(2, ROLL_TICKS)
        val vc = input.cruiseSpeed.coerceAtLeast(1.0E-4)
        val medium = if (input.medium == Medium.WATER && this.figureAsked(input)) Medium.AIR else input.medium
        return when (medium) {
            Medium.GROUND -> stepGround(input, vc)
            Medium.WATER -> stepWater(input, vc)
            Medium.AIR -> stepAir(input, vc)
        }
    }

    /** В воде петля, бочка и рывок идут тем же кругом, что в воздухе. Обычное плавание остаётся своим. */
    private fun figureAsked(input: Input): Boolean =
        this.looping || this.zooming || this.rollLeft > 0f || this.rollTicks > 0 ||
            input.loopRequest || input.rollRequest != 0 ||
            (input.zoomRequest && this.zoomReady)

    private fun stepAir(input: Input, vc: Double): Output {
        val forward = input.forward.coerceIn(-1f, 1f)
        val climb = sign(input.climb)
        val flapping = forward > FORWARD_EPS
        val reversing = forward < -FORWARD_EPS
        val strafeOnly = abs(forward) <= FORWARD_EPS && abs(input.strafe) > FORWARD_EPS

        if (input.loopRequest && !looping && !zooming && flapping && speed >= vc * LOOP_ENTRY && abs(wrap(pathPitch)) < 50f) {
            looping = true
            loopTurned = 0f
            loopDirection = 1f
            stalled = false
            abortRoll()
        }
        if (input.rollRequest != 0 && rollTicks == 0 && !looping && !zooming && speed >= vc * ROLL_ENTRY && !reversing) {
            rollDirection = if (input.rollRequest > 0) -1f else 1f
            rollLeft = FULL_TURN
            rollSpan = ROLL_TICKS
            rollTicks = rollSpan
        }
        if (input.rollBoostRequest && rollLeft > 0f) {
            if (!rollCharging && chargeOpen()) {
                rollCharging = true
            } else if (rollCharging) {
                rollFinish = true
            }
        }

        val boostTuck = if (boostLimit > 1.0E-6) (rollBoost / boostLimit).toFloat().coerceIn(0f, 1f) else 0f
        val diveTuck = climb < 0f && !flapping && !reversing && speed > vc * 0.4
        val tuckTarget = when {
            zooming -> 1f
            diveTuck -> 1f
            else -> boostTuck
        }
        val tuckRate = when {
            zooming -> ZOOM_TUCK
            !zooming && !diveTuck && boostTuck > 0f -> BOOST_TUCK
            tuckTarget > tuck -> TUCK_IN
            else -> TUCK_OUT
        }
        tuck = approach(tuck, tuckTarget, tuckRate)
        if (input.zoomRequest && zoomReady && input.zoomBoost > 0.0 && !reversing) {
            startZoom(vc, input.zoomBoost)
        }
        if (zoomGrace > 0) zoomGrace--

        if (strafeOnly && rollLeft <= 0f && !looping && !zooming) clearRollBoost()

        if (reversing || (!flapping && !looping && !zooming && speed < vc * HOVER_SPEED && speed > -vc)) {
            return stepHover(input, vc, forward, climb)
        }
        hover = approach(hover, 0f, 0.1f)

        stepPitch(input, vc, climb, flapping)
        stepSpeed(vc, flapping, forward, climb, input.yawDelta)
        stepRoll(input, vc)
        if (zooming) {
            zoomTicks--
            if (zoomTicks <= 0) endZoom()
        }

        val pitchRad = Math.toRadians(pathPitch.toDouble())
        var vertical = speed * sin(pitchRad)
        if (vertical > 0.0) vertical *= input.lift
        val along = speed * cos(pitchRad)
        val juke = if (rollTicks > 0) {
            val progress = 1f - rollTicks.toFloat() / rollSpan.coerceAtLeast(1)
            -rollDirection * vc * ROLL_JUKE * sin(progress * Math.PI)
        } else {
            0.0
        }
        val lateral = input.strafe.coerceIn(-1f, 1f) * vc * STRAFE_SHARE + juke
        if (rollTicks > 0) rollTicks--
        if (!input.inLiquid && !looping && !zooming && climb > 0f && vertical > 0.0 && rollBoost > 0.0) {
            rollBoost = (rollBoost - climbDrain).coerceAtLeast(0.0)
        }
        return Output(along, lateral, vertical)
    }

    /**
     * Нос продолжает тянуться вверх в ту же сторону, что и в петле, до почти отвесного набора.
     * Замкнуть круг можно и самим рывком: петля засчитывается, когда поворот проходит полный оборот.
     * Толчок один, на всю силу сразу. Окно набора не зависит от силы: скорость спадает к обычной.
     */
    private fun startZoom(vc: Double, boost: Double) {
        val strength = boost.coerceIn(0.0, ZOOM_MAX_BOOST)
        zoomLoopLeft = if (looping) (FULL_TURN - loopTurned).coerceAtLeast(0f) else 0f
        looping = false
        loopTurned = 0f
        stalled = false
        abortRoll()
        zoomGrace = 0
        zooming = true
        zoomStarted = true
        zoomTicks = ZOOM_TICKS
        val rise = ((ZOOM_ANGLE - pathPitch) % FULL_TURN + FULL_TURN) % FULL_TURN
        zoomTarget = pathPitch + rise
        val kick = vc * (1.0 + ZOOM_KICK * strength)
        speed = max(speed, kick)
        zoomPeak = max(speed, vc * ZOOM_END_SPEED)
    }

    private fun endZoom() {
        zooming = false
        zoomTicks = 0
        zoomPeak = 0.0
        zoomLoopLeft = 0f
        zoomGrace = 0
    }

    private fun stepPitch(input: Input, vc: Double, climb: Float, flapping: Boolean) {
        if (zooming) {
            val before = pathPitch
            pathPitch = approach(pathPitch, zoomTarget, ZOOM_PULL)
            if (zoomLoopLeft > 0f) {
                zoomLoopLeft -= pathPitch - before
                if (zoomLoopLeft <= 0f) completedLoop = true
            }
            return
        }
        if (looping) {
            val pull = (LOOP_RATE * (speed / vc).toFloat().coerceIn(0.55f, 1.5f)) *
                if (climb > 0f) 1.15f else 0.85f
            val previous = loopTurned
            if (loopTurned < LOOP_COMMIT && speed < vc * STALL_SPEED) {
                looping = false
                stalled = true
                pathPitch = wrap(pathPitch)
                return
            }
            loopTurned += pull
            pathPitch += pull * loopDirection
            if (previous < FULL_TURN && loopTurned >= FULL_TURN) {
                looping = false
                completedLoop = true
                loopTurned = 0f
                pathPitch = wrap(pathPitch)
                zoomGrace = ZOOM_GRACE_TICKS
            }
            return
        }
        pathPitch = wrap(pathPitch)
        if (stalled || (speed < vc * STALL_SPEED && pathPitch > 20f)) {
            stalled = speed < vc * RECOVER_SPEED || pathPitch > -10f
            pathPitch = approach(pathPitch, STALL_NOSE, 4.5f)
            return
        }
        val target = when {
            climb > 0f -> CLIMB_ANGLE
            climb < 0f && tuck > 0.5f -> STOOP_ANGLE
            climb < 0f -> DIVE_ANGLE
            flapping -> 0f
            else -> GLIDE_ANGLE * (1f - ((speed - vc) / vc).toFloat().coerceIn(0f, 1f))
        }
        val rate = if (abs(target) > abs(pathPitch)) PITCH_RATE else PITCH_RATE * 1.3f
        pathPitch = approach(pathPitch, target, rate)
    }

    private fun stepSpeed(vc: Double, flapping: Boolean, forward: Float, climb: Float, yawDelta: Float) {
        if (zooming) {
            // Импульс уже отдан. Потолок обычного полёта его не срезает, тяги по тикам нет.
            val floor = vc * ZOOM_END_SPEED
            val span = ZOOM_TICKS.toDouble().coerceAtLeast(1.0)
            val remaining = (zoomTicks.toDouble() / span).coerceIn(0.0, 1.0)
            speed = floor + (zoomPeak - floor) * remaining
            effort = approach(effort, 0f, 0.25f)
            return
        }
        val pitchRad = Math.toRadians(pathPitch.toDouble())
        speed -= GRAVITY * vc * sin(pitchRad)
        val ceiling = vc + rollBoost
        val wanted = ceiling * forward.coerceAtLeast(0f)
        var work = 0f
        if (flapping && speed < wanted) {
            val gain = if (pathPitch > 5f) CLIMB_THRUST else THRUST
            speed += gain * (wanted - speed)
            work = ((wanted - speed) / vc * 3.0).toFloat().coerceIn(0.25f, 1f)
        } else if (flapping) {
            work = 0.18f
        }
        if (speed > ceiling) {
            val excess = (speed - ceiling) / vc
            speed -= HIGH_DRAG * (1.0 - tuck * 0.55) * excess * (1.0 + excess) * vc
        }
        // Раскрытые крылья без взмахов — воздушный тормоз. В пике и в рывке его нет, там держит лобовое сопротивление.
        if (!flapping && !looping && !zooming && climb >= 0f) {
            speed -= GLIDE_DRAG * speed
        }
        speed -= abs(yawDelta) / 90.0 * TURN_COST * speed
        if (looping && pathPitch > 0f) work = max(work, 0.7f)
        speed = speed.coerceIn(0.0, max(vc * MAX_SPEED, ceiling))
        effort = approach(effort, work * (1f - tuck), 0.25f)
    }

    private fun stepRoll(input: Input, vc: Double) {
        if (rollLeft > 0f) {
            val step = min(rollLeft, FULL_TURN / rollSpan.coerceAtLeast(1))
            roll += step * rollDirection
            rollLeft -= step
            if (rollLeft <= 0f) {
                roll = wrap(roll)
                if (rollCharging && chargeOpen()) {
                    rollBoost = (rollBoost + boostStep).coerceAtMost(boostLimit)
                    val keep = !rollFinish && chargeOpen()
                    if (keep) {
                        rollSpan = fasterSpan()
                        rollLeft = FULL_TURN
                        rollTicks = rollSpan
                    } else {
                        rollCharging = false
                        rollFinish = false
                        rollTicks = 0
                    }
                } else if (rollCharging) {
                    rollCharging = false
                    rollFinish = false
                    rollTicks = 0
                }
            }
            return
        }
        val strafeBank = -input.strafe.coerceIn(-1f, 1f) * PhantomFlightAttitude.BANK
        val turnBank = (input.yawDelta * TURN_BANK * (speed / vc).toFloat()).coerceIn(-MAX_TURN_BANK, MAX_TURN_BANK)
        roll = approach(wrap(roll), (strafeBank + turnBank).coerceIn(-MAX_TURN_BANK, MAX_TURN_BANK), 5f)
    }

    /** Потолок или нулевой шаг закрывают приём. Обычная бочка при этом остаётся. */
    private fun chargeOpen(): Boolean =
        boostLimit > 1.0E-6 && boostStep > 1.0E-6 && rollBoost < boostLimit - 1.0E-4

    /** Каждый следующий оборот короче предыдущего, но не короче серверного предела. */
    private fun fasterSpan(): Int {
        val spun = kotlin.math.floor(rollSpan * rollSpin).toInt()
        val dropped = if (rollSpin < 0.999 && spun >= rollSpan) rollSpan - 1 else spun
        return dropped.coerceIn(rollMinTicks, ROLL_TICKS)
    }

    /** Незаконченный оборот не прибавляет скорость. Уже накопленный бонус остаётся. */
    private fun abortRoll() {
        rollLeft = 0f
        rollTicks = 0
        rollSpan = ROLL_TICKS
        rollCharging = false
        rollFinish = false
    }

    private fun clearRollBoost() {
        rollBoost = 0.0
        rollCharging = false
        rollFinish = false
    }

    private fun stepHover(input: Input, vc: Double, forward: Float, climb: Float): Output {
        looping = false
        loopTurned = 0f
        stalled = false
        abortRoll()
        val risingInLiquid = input.inLiquid && climb > 0f && forward >= -FORWARD_EPS
        if (!risingInLiquid) clearRollBoost()
        endZoom()
        hover = approach(hover, 1f, 0.1f)
        val wanted = if (forward < -FORWARD_EPS) vc * forward * PhantomHover.REVERSE_SPEED else 0.0
        speed += (wanted - speed) * HOVER_BRAKE
        pathPitch = approach(wrap(pathPitch), 0f, PITCH_RATE * 1.5f)
        roll = approach(wrap(roll), -input.strafe.coerceIn(-1f, 1f) * PhantomFlightAttitude.BANK, 5f)
        effort = approach(effort, 1f, 0.25f)
        tuck = approach(tuck, 0f, TUCK_OUT)
        var vertical = climb * vc * HOVER_CLIMB
        if (vertical > 0.0) vertical *= input.lift
        val lateral = input.strafe.coerceIn(-1f, 1f) * vc * STRAFE_SHARE
        return Output(speed, lateral, vertical)
    }

    private fun stepGround(input: Input, vc: Double): Output {
        looping = false
        loopTurned = 0f
        abortRoll()
        clearRollBoost()
        stalled = false
        endZoom()
        tuck = approach(tuck, 0f, TUCK_OUT)
        val climb = sign(input.climb)
        val wanted = vc * input.forward.coerceIn(-1f, 1f) * if (input.forward < 0f) PhantomHover.REVERSE_SPEED else GROUND_SPEED
        speed += (wanted - speed) * GROUND_GRIP
        pathPitch = approach(wrap(pathPitch), 0f, 8f)
        roll = approach(wrap(roll), 0f, 8f)
        hover = approach(hover, 0f, 0.2f)
        effort = approach(effort, if (abs(input.forward) > FORWARD_EPS) 0.3f else 0f, 0.2f)
        var vertical = climb * vc * HOVER_CLIMB
        if (vertical > 0.0) vertical *= input.lift
        if (climb == 0f) vertical = -GROUND_SETTLE
        val lateral = input.strafe.coerceIn(-1f, 1f) * vc * GROUND_SPEED * 0.8
        return Output(speed, lateral, vertical)
    }

    private fun stepWater(input: Input, vc: Double): Output {
        looping = false
        loopTurned = 0f
        abortRoll()
        stalled = false
        endZoom()
        val climb = sign(input.climb)
        val forward = input.forward.coerceIn(-1f, 1f)
        val reversing = forward < -FORWARD_EPS
        val strafeOnly = abs(forward) <= FORWARD_EPS && abs(input.strafe) > FORWARD_EPS
        val halted = abs(forward) <= FORWARD_EPS && speed < vc * HOVER_SPEED && speed > -vc
        if (reversing || strafeOnly || (halted && climb <= 0f)) clearRollBoost()
        val boostTuck = if (boostLimit > 1.0E-6) (rollBoost / boostLimit).toFloat().coerceIn(0f, 1f) else 0f
        tuck = approach(tuck, boostTuck, if (boostTuck > 0f) BOOST_TUCK else TUCK_OUT)
        val wanted = (vc + rollBoost) * forward * if (forward < 0f) PhantomHover.REVERSE_SPEED else 1.0
        val grip = if (abs(forward) > FORWARD_EPS) WATER_GRIP else WATER_GLIDE
        speed += (wanted - speed) * grip
        val target = when {
            climb > 0f -> 28f
            climb < 0f -> -28f
            else -> 0f
        }
        pathPitch = approach(wrap(pathPitch), target, 2.2f)
        val turnBank = (input.yawDelta * TURN_BANK * 1.4f).coerceIn(-MAX_TURN_BANK, MAX_TURN_BANK)
        roll = approach(wrap(roll), (-input.strafe * PhantomFlightAttitude.BANK + turnBank).coerceIn(-MAX_TURN_BANK, MAX_TURN_BANK), 3f)
        hover = approach(hover, if (abs(forward) > FORWARD_EPS) 0f else 1f, 0.06f)
        effort = approach(effort, if (abs(forward) > FORWARD_EPS || climb != 0f) 0.8f else 0.1f, 0.12f)
        val along = speed * cos(Math.toRadians(pathPitch.toDouble()))
        val vertical = climb * vc * WATER_CLIMB
        val lateral = input.strafe.coerceIn(-1f, 1f) * vc * STRAFE_SHARE * 0.7
        return Output(along, lateral, vertical)
    }

    companion object {
        const val FULL_TURN = 360f

        /** Тормоз силы тяжести на подъёме, доля крейсерской за тик при отвесном полёте. */
        const val GRAVITY = 0.026
        /** Взмахи разгоняют сильнее на малой скорости: U-кривая мощности птиц. */
        const val THRUST = 0.11
        const val CLIMB_THRUST = 0.13
        const val HIGH_DRAG = 0.012
        const val GLIDE_DRAG = 0.035
        const val TURN_COST = 0.03
        const val MAX_SPEED = 2.3

        const val CLIMB_ANGLE = 38f
        const val DIVE_ANGLE = -38f
        const val STOOP_ANGLE = -62f
        const val GLIDE_ANGLE = -5f
        const val PITCH_RATE = 3.5f

        const val LOOP_ENTRY = 0.7
        const val LOOP_RATE = 6.5f
        /** После этого угла петля дотягивается сама, даже на малой скорости. */
        const val LOOP_COMMIT = 100f
        const val STALL_SPEED = 0.3
        const val RECOVER_SPEED = 0.55
        const val STALL_NOSE = -40f

        const val ROLL_ENTRY = 0.5
        const val ROLL_TICKS = 16
        const val ROLL_JUKE = 0.38
        /** Сколько блоков за тик добавляет один полный оборот заряженной бочки. */
        const val ROLL_BOOST_STEP = 0.2
        /** Набор высоты съедает бонус примерно на 0.2 за две секунды. */
        const val CLIMB_DRAIN = 0.2 / 40.0
        /** Крылья догоняют долю бонуса медленнее, чем складываются в пике. */
        const val BOOST_TUCK = 0.025f

        /** С этого угла петли (нос смотрит вниз) седоку предлагают рывок вверх. */
        const val ZOOM_WINDOW = 270f
        /** Столько тиков после замкнутой петли рывок ещё можно начать. */
        const val ZOOM_GRACE_TICKS = 6
        /** Почти отвесный набор: курс ещё виден. */
        const val ZOOM_ANGLE = 80f
        const val ZOOM_PULL = 9f
        /** Скорость сразу после толчка: 1 + это × сила, в крейсерских. Потолок обычного полёта её не режет. */
        const val ZOOM_KICK = 0.6
        /** Окно набора в тиках. Одно и то же при любой силе: сила меняет только скорость в начале. */
        const val ZOOM_TICKS = 64
        /** К концу окна скорость сходится сюда, в крейсерских, и дальше полёт обычный. */
        const val ZOOM_END_SPEED = 0.85
        const val ZOOM_MAX_BOOST = 64.0
        const val ZOOM_TUCK = 0.25f

        const val TURN_BANK = 1.6f
        const val MAX_TURN_BANK = 58f
        const val STRAFE_SHARE = 0.55

        const val HOVER_SPEED = 0.3
        const val HOVER_BRAKE = 0.16
        const val HOVER_CLIMB = 0.6

        const val GROUND_SPEED = 0.45
        const val GROUND_GRIP = 0.2

        /** У земли без подъёма фантом садится, а не висит в полублоке над ней. */
        const val GROUND_SETTLE = 0.08
        const val WATER_GRIP = 0.07
        const val WATER_GLIDE = 0.03
        const val WATER_CLIMB = 0.55

        const val TUCK_IN = 0.08f
        const val TUCK_OUT = 0.14f
        private const val FORWARD_EPS = 0.045f

        fun wrap(degrees: Float): Float {
            var d = degrees % 360f
            if (d > 180f) d -= 360f
            if (d <= -180f) d += 360f
            return d
        }

        fun approach(current: Float, target: Float, step: Float): Float = when {
            current < target -> min(current + step, target)
            current > target -> max(current - step, target)
            else -> current
        }
    }
}
