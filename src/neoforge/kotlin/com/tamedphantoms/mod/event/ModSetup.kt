package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.entity.ModEntities
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent

/**
 * Клиентские классы подключаются только если [FMLEnvironment.dist] равен [Dist.CLIENT],
 * чтобы выделенный сервер их не загружал.
 */
object ModSetup {

    fun init(modBus: IEventBus, modContainer: ModContainer) {
        modBus.addListener(::onEntityAttributeCreation)

        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.tamedphantoms.mod.client.ClientModEvents.init(modBus, modContainer)
        }
    }

    private fun onEntityAttributeCreation(event: EntityAttributeCreationEvent) {
        event.put(ModEntities.TAMED_PHANTOM.get(), TamedPhantomEntity.createAttributes().build())
    }
}
