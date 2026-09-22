package com.tamedphantoms.mod.util

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.attributes.Attributes

/** Зелья скорости и замедления: отношение текущей скорости передвижения к базовой. */
object PhantomEffectSpeed {

    fun scale(entity: LivingEntity): Double {
        val attribute = entity.getAttribute(Attributes.MOVEMENT_SPEED) ?: return 1.0
        val base = attribute.baseValue
        if (base <= 1.0E-4) return 1.0
        return (attribute.value / base).coerceIn(0.12, 6.0)
    }
}
