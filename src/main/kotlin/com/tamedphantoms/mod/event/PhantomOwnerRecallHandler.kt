package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.entity.PhantomOwnerRecall
import net.minecraft.world.entity.player.Player

/**
 * После возрождения или смены измерения хозяин оказывается далеко,
 * а фантом часто остаётся в старом чанке (особенно если игрок умер верхом).
 * ИИ в выгруженном чанке не тикает — подтягиваем питомца отсюда.
 */
object PhantomOwnerRecallHandler {

    fun onRespawn(player: Player) {
        PhantomOwnerRecall.recallOwned(player)
    }

    fun onChangedDimension(player: Player) {
        PhantomOwnerRecall.recallOwned(player)
    }

    fun onLogin(player: Player) {
        PhantomOwnerRecall.recallOwned(player)
    }

    fun onPlayerTick(player: Player) {
        if (player.level().isClientSide || player.tickCount % 20 != 0) return
        PhantomOwnerRecall.recallUnloaded(player)
    }

    fun onClone(newPlayer: Player, oldPlayer: Player) {
        PhantomOwnerRecall.copyTo(newPlayer, oldPlayer)
        PhantomGuideHandler.copyTo(newPlayer, oldPlayer)
    }
}
