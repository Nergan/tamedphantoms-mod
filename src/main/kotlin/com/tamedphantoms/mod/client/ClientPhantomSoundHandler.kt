package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.config.ClientConfig
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.PlayLevelSoundEvent

/**
 * Громкость приручённого фантома — клиентская настройка: каждый игрок
 * слышит «своё» значение, сервер звук не ослабляет.
 *
 * Регистрируется вручную через [net.neoforged.neoforge.common.NeoForge.EVENT_BUS],
 * как остальные обработчики мода. `@EventBusSubscriber` на Kotlin `object`
 * не работает: методы объекта не static, а AutomaticEventSubscriber требует static.
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
