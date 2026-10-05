package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity

/**
 * Самозащита ручного фантома и лечение за нанесённый урон.
 * Вызывается после того, как урон уже применился.
 */
object PhantomDamageHandler {

    fun onLivingDamage(victim: LivingEntity, source: DamageSource, dealt: Float) {
        val sourceEntity = source.entity
        if (sourceEntity is TamedPhantomEntity && dealt > 0.0f) {
            sourceEntity.heal(dealt)
        }
        if (victim !is TamedPhantomEntity) return
        if (sourceEntity is LivingEntity && sourceEntity !== victim) {
            victim.startDefending(sourceEntity.uuid)
        }
    }
}
