package com.tamedphantoms.mod.util

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Фигуры, которые фантом без всадника выполняет сам: петля, бочка,
 * вираж с разворотом и пике с выходом. Поза и скорость — функции времени,
 * поэтому сервер ведёт траекторию, а клиент рисует ту же позу по своему счёту тиков.
 */
object PhantomManeuvers {

    enum class Kind(val ticks: Int) {
        LOOP(54),
        ROLL(18),
        WINGOVER(46),
        STOOP(64),
    }

    data class Attitude(
        val pitch: Float,
        val roll: Float,
        val tuck: Float,
        val effort: Float,
    )

    /** Скорость в осях курса: вперёд, вверх, влево. И поворот курса за тик в градусах. */
    data class Motion(
        val forward: Double,
        val up: Double,
        val left: Double,
        val yawStep: Float,
    )

    /** Номер в синхронизируемых данных: вид в младших битах, счётчик — чтобы повтор той же фигуры был виден. */
    fun encode(kind: Kind, serial: Int): Int = (kind.ordinal + 1) or ((serial and 0xFFFF) shl 4)

    fun decode(value: Int): Kind? = Kind.entries.getOrNull((value and 0xF) - 1)

    /** Чётный счётчик — фигура влево, нечётный — вправо. */
    fun side(value: Int): Float = if (((value ushr 4) and 1) == 0) 1f else -1f

    fun progress(kind: Kind, elapsed: Int): Float = (elapsed.toFloat() / kind.ticks).coerceIn(0f, 1f)

    fun active(kind: Kind?, elapsed: Int): Boolean = kind != null && elapsed in 0 until kind.ticks

    fun attitude(kind: Kind, elapsed: Int, side: Float = 1f): Attitude {
        val t = progress(kind, elapsed)
        return when (kind) {
            Kind.LOOP -> Attitude(360f * ease(t), 0f, 0f, if (t < 0.5f) 1f else 0.35f)
            Kind.ROLL -> Attitude(0f, -side * 360f * ease(t), 0f, 0.4f)
            Kind.WINGOVER -> {
                val arc = sin(t * PI).toFloat()
                val pitch = 45f * sin(t * 2.0 * PI).toFloat()
                Attitude(pitch, -side * 70f * arc, 0f, if (t < 0.5f) 0.8f else 0.2f)
            }
            Kind.STOOP -> when {
                t < 0.15f -> Attitude(30f * (t / 0.15f), 0f, 0f, 1f)
                t < 0.7f -> {
                    val dive = ((t - 0.15f) / 0.2f).coerceAtMost(1f)
                    Attitude(30f - 92f * dive, 0f, dive, 0f)
                }
                else -> {
                    val out = (t - 0.7f) / 0.3f
                    Attitude(-62f * (1f - out), 0f, 1f - out, 0.8f)
                }
            }
        }
    }

    /**
     * @param speed скорость входа, блоки за тик
     */
    fun motion(kind: Kind, elapsed: Int, speed: Double, side: Float = 1f): Motion {
        val t = progress(kind, elapsed)
        val pose = attitude(kind, elapsed, side)
        val pitch = Math.toRadians(pose.pitch.toDouble())
        return when (kind) {
            Kind.LOOP -> {
                val climbLoss = 1.0 - 0.35 * sin(t * PI)
                Motion(speed * climbLoss * cos(pitch), speed * climbLoss * sin(pitch), 0.0, 0f)
            }
            Kind.ROLL -> Motion(speed, 0.0, side * speed * 0.38 * sin(t * PI), 0f)
            Kind.WINGOVER -> {
                val yawStep = (180.0 / kind.ticks * PI / 2.0 * sin(t * PI)).toFloat() * -side
                Motion(speed * 0.85 * cos(pitch), speed * 0.85 * sin(pitch), 0.0, yawStep)
            }
            Kind.STOOP -> {
                val boost = if (t in 0.15f..0.85f) 1.0 + 0.9 * sin(((t - 0.15f) / 0.7f) * PI) else 1.0
                Motion(speed * boost * cos(pitch), speed * boost * sin(pitch), 0.0, 0f)
            }
        }
    }

    /** Высота, которую фигура занимает над точкой входа и под ней, блоки. */
    fun clearance(kind: Kind, speed: Double): Pair<Double, Double> {
        var up = 0.0
        var down = 0.0
        var height = 0.0
        for (tick in 0 until kind.ticks) {
            height += motion(kind, tick, speed).up
            up = maxOf(up, height)
            down = minOf(down, height)
        }
        return up to -down
    }

    private fun ease(t: Float): Float = (0.5 - 0.5 * cos(t * PI)).toFloat()
}
