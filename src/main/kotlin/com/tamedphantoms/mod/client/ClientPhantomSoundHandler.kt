package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.config.ClientConfig
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.PlayLevelSoundEvent

/**
 * Громкость приручённого фантома — клиентская настройка: каждый игрок
 * слышит «своё» значение, сервер звук не ослабляет.
 */
@EventBusSubscriber(modid = com.tamedphantoms.mod.TamedPhantomsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = [Dist.CLIENT])
object ClientPhantomSoundHandler {

    @SubscribeEvent
    fun onPlaySoundAtEntity(event: PlayLevelSoundEvent.AtEntity) {
        if (!event.level.isClientSide) return
        val entity = event.entity as? TamedPhantomEntity ?: return
        if (!entity.tamed) return
        val mul = ClientConfig.CONFIG.tamedSoundVolume.get().toFloat().coerceIn(0.0f, 1.0f)
        event.setNewVolume(event.newVolume * mul)
    }
}
