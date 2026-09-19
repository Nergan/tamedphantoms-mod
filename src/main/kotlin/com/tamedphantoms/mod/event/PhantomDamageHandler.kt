package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.world.entity.LivingEntity
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent

/**
 * Запускает "режим самозащиты" ручного фантома, когда его кто-то ударил.
 *
 * Изначально это было реализовано как `override fun hurtServer(...)` прямо
 * в [TamedPhantomEntity], но при сборке против реального NeoForge 21.1.209
 * этот override не резолвился (см. README, раздел про историю правок —
 * возможные причины: расхождение версий ModDevGradle/маппингов). Событие
 * [LivingDamageEvent] — куда более стабильная, официально документированная
 * точка входа именно для "прореагировать на то, что сущность получила урон",
 * поэтому самозащита перенесена сюда.
 */
object PhantomDamageHandler {

    @SubscribeEvent
    fun onLivingDamage(event: LivingDamageEvent.Post) {
        val sourceEntity = event.source.entity
        if (sourceEntity is TamedPhantomEntity) {
            val dealt = event.newDamage
            if (dealt > 0.0f) {
                sourceEntity.heal(dealt)
            }
        }

        val victim = event.entity
        if (victim !is TamedPhantomEntity) return

        if (sourceEntity is LivingEntity && sourceEntity !== victim) {
            // Владелец НЕ исключается намеренно — по ТЗ фантом отбивается
            // от кого угодно, включая собственного хозяина, если тот его ударит.
            victim.startDefending(sourceEntity.uuid)
        }
    }
}
