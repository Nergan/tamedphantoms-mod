package com.tamedphantoms.mod.util

import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity

/**
 * Зелья, которые ванильный полёт фантома не читает сам.
 * Яд, регенерация, сопротивление, огнестойкость и сила идут через обычный тик живого существа
 * и через атрибут урона.
 */
object PhantomEffectSpeed {

    /** +20% за уровень скорости, −15% за уровень замедления. Нет эффекта — null. */
    fun movementScale(speedAmplifier: Int?, slowAmplifier: Int?): Double {
        var scale = 1.0
        if (speedAmplifier != null) scale *= 1.0 + 0.2 * (speedAmplifier + 1)
        if (slowAmplifier != null) scale *= (1.0 - 0.15 * (slowAmplifier + 1)).coerceAtLeast(0.0)
        return scale.coerceIn(0.12, 6.0)
    }

    fun scale(entity: LivingEntity): Double = movementScale(
        entity.getEffect(MobEffects.MOVEMENT_SPEED)?.amplifier,
        entity.getEffect(MobEffects.MOVEMENT_SLOWDOWN)?.amplifier,
    )

    /** Под водой грация дельфина удваивает и без того половинную скорость. */
    fun dolphin(entity: LivingEntity): Double {
        if (!entity.isUnderWater) return 1.0
        val grace = entity.getEffect(MobEffects.DOLPHINS_GRACE) ?: return 1.0
        return 2.0 + grace.amplifier
    }

    fun axis(value: Float, invert: Boolean): Float = if (invert) -value else value

    fun axis(value: Double, invert: Boolean): Double = if (invert) -value else value

    /** Прыгучесть ускоряет набор высоты, левитация тянет вверх, плавное падение гасит спуск. */
    fun adjustVertical(entity: LivingEntity, vertical: Double): Double {
        var y = vertical
        val jump = entity.getEffect(MobEffects.JUMP)
        if (jump != null && y > 0.0) y *= 1.0 + 0.5 * (jump.amplifier + 1)
        val levitation = entity.getEffect(MobEffects.LEVITATION)
        if (levitation != null) y += 0.05 * (levitation.amplifier + 1)
        if (y < 0.0 && entity.hasEffect(MobEffects.SLOW_FALLING)) y *= 0.25
        return y
    }
}
