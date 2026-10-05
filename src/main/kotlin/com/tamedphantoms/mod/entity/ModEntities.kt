package com.tamedphantoms.mod.entity

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.phys.Vec3
import java.util.function.Supplier

/**
 * Тип прирученного фантома. Регистрацию в реестре делает загрузчик
 * и кладёт результат в [TAMED_PHANTOM].
 *
 * Хитбокс 0.9x0.5 — как у ванильного Phantom. `fireImmune()` — осознанный
 * компромисс вместо точечного "не гореть только от солнца". Два места для
 * верховой езды заданы через `passengerAttachments`.
 */
object ModEntities {

    lateinit var TAMED_PHANTOM: Supplier<EntityType<TamedPhantomEntity>>

    fun builder(): EntityType.Builder<TamedPhantomEntity> =
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
                    Vec3(0.0, 0.46, -0.17),
                    Vec3(0.0, 0.46, -0.63),
                ),
            )
}
