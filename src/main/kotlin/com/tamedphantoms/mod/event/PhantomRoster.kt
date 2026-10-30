package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.entity.PhantomOwnerRecall
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

/** Координаты своих фантомов в чат: кто сидит, а кто двигается. */
object PhantomRoster {

    fun report(player: ServerPlayer) {
        val rows = PhantomOwnerRecall.roster(player)
        if (rows.isEmpty()) {
            player.sendSystemMessage(Component.translatable("tamedphantoms.roster.none"), false)
            return
        }
        player.sendSystemMessage(Component.translatable("tamedphantoms.roster.header", rows.size), false)
        for (row in rows) {
            val state = if (row.sitting) "tamedphantoms.roster.sitting" else "tamedphantoms.roster.moving"
            player.sendSystemMessage(
                Component.translatable(
                    "tamedphantoms.roster.line",
                    Component.translatable(state),
                    row.x,
                    row.y,
                    row.z,
                    row.dimension,
                ),
                false,
            )
        }
    }
}
