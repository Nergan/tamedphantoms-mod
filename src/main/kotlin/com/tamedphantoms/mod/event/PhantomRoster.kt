package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.entity.PhantomOwnerRecall
import com.tamedphantoms.mod.network.PhantomRosterListPayload
import com.tamedphantoms.mod.platform.ModNetwork
import net.minecraft.server.level.ServerPlayer

/** Список своих фантомов на экран координат. */
object PhantomRoster {

    fun report(player: ServerPlayer) {
        ModNetwork.sendToPlayer(player, PhantomRosterListPayload(PhantomOwnerRecall.roster(player)))
    }
}
