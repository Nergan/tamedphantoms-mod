package com.tamedphantoms.mod.entity

import com.tamedphantoms.mod.TamedPhantomsMod
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

/**
 * Реестр сущностей мода. Единственная новая сущность — [TamedPhantomEntity].
 * Дикие фантомы остаются полностью ванильными; приручение "пересаживает"
 * дикого фантома в эту сущность (см. [com.tamedphantoms.mod.event.PhantomInteractionHandler]).
 *
 * ПРИМЕЧАНИЕ: `.build("tamed_phantom")` (простая строка, а не ResourceKey)
 * — эта форма подтверждена как реально рабочая при настоящей сборке против
 * NeoForge 21.1.209 (см. README, "История правок").
 */
object ModEntities {

    val ENTITY_TYPES: DeferredRegister<EntityType<*>> =
        DeferredRegister.create(Registries.ENTITY_TYPE, TamedPhantomsMod.MOD_ID)

    /**
     * Хитбокс 0.9x0.5 — как у ванильного Phantom. `fireImmune()` — осознанный
     * компромисс вместо точечного "не гореть только от солнца" (см. докстринг
     * в TamedPhantomEntity). Два места для верховой езды заданы через
     * `passengerAttachments` — [0] переднее ("пилотское", только владелец),
     * [1] заднее (любой другой игрок), см. `PhantomSeatAssignment`.
     */
    val TAMED_PHANTOM = ENTITY_TYPES.register(
        "tamed_phantom",
        Supplier<EntityType<TamedPhantomEntity>> {
            EntityType.Builder.of<TamedPhantomEntity>(
                EntityType.EntityFactory<TamedPhantomEntity> { type, level -> TamedPhantomEntity(type, level) },
                MobCategory.CREATURE,
            )
                .sized(0.9f, 0.5f)
                .clientTrackingRange(64)
                .updateInterval(3)
                .fireImmune()
                .passengerAttachments(
                    *arrayOf(
                        Vec3(0.0, 0.46, 0.08),
                        Vec3(0.0, 0.46, -0.38),
                    ),
                )
                .build("tamed_phantom")
        },
    )
}
