package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.config.ClientConfig
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.PlayLevelSoundEvent

/**
 * Громкость приручённого фантома — клиентская настройка: каждый игрок
 * слышит «своё» значение, сервер звук не ослабляет.
 *
 * Регистрируется через NeoForge.EVENT_BUS, как остальные обработчики:
 * `@EventBusSubscriber` на Kotlin `object` падает, потому что методы не static.
 */
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
