package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.entity.PhantomOwnerRecall
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent

/**
 * После возрождения или смены измерения хозяин оказывается далеко,
 * а фантом часто остаётся в старом чанке (особенно если игрок умер верхом).
 * ИИ в выгруженном чанке не тикает — подтягиваем питомца отсюда.
 */
object PhantomOwnerRecallHandler {

    @SubscribeEvent
    fun onRespawn(event: PlayerEvent.PlayerRespawnEvent) {
        PhantomOwnerRecall.recallOwned(event.entity)
    }

    @SubscribeEvent
    fun onChangedDimension(event: PlayerEvent.PlayerChangedDimensionEvent) {
        PhantomOwnerRecall.recallOwned(event.entity)
    }

    @SubscribeEvent
    fun onLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        PhantomOwnerRecall.recallOwned(event.entity)
    }

    @SubscribeEvent
    fun onClone(event: PlayerEvent.Clone) {
        PhantomOwnerRecall.copyTo(event.entity, event.original)
    }
}
