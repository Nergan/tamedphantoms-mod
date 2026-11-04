package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.entity.PhantomRosterRow
import com.tamedphantoms.mod.network.PhantomRosterPayload
import com.tamedphantoms.mod.platform.ModNetwork
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

/** Ответ сервера со списком фантомов и подписи для экрана. */
object PhantomRosterClient {
    private val dyeNames = arrayOf(
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black",
    )

    var rows: List<PhantomRosterRow> = emptyList()
        private set
    var ready: Boolean = false
        private set
    var listener: (() -> Unit)? = null
    var quietUntil: Long = 0L

    fun hush() {
        quietUntil = System.nanoTime() + 400_000_000L
    }

    fun request() {
        ModNetwork.sendToServer(PhantomRosterPayload)
    }

    fun accept(rows: List<PhantomRosterRow>) {
        this.rows = rows
        ready = true
        listener?.invoke()
    }

    fun title(row: PhantomRosterRow): Component {
        val name = if (row.name.isBlank()) {
            Component.translatable("tamedphantoms.roster.unnamed")
        } else {
            Component.literal(row.name)
        }
        val where = row.dimension.substringAfter(':')
        return name.copy().append(Component.literal(" — $where"))
    }

    fun details(row: PhantomRosterRow): Component {
        val bits = ArrayList<Component>()
        val state = if (row.sitting) "tamedphantoms.roster.sitting" else "tamedphantoms.roster.moving"
        bits.add(Component.translatable(state))
        dye(row.bodyDye)?.let { bits.add(Component.translatable("tamedphantoms.roster.body", it)) }
        dye(row.saddleDye)?.let { bits.add(Component.translatable("tamedphantoms.roster.saddle", it)) }
        if (row.luminous) bits.add(Component.translatable("tamedphantoms.roster.glow"))
        var line: MutableComponent = bits[0].copy()
        for (i in 1 until bits.size) {
            line = line.append(Component.literal(", ")).append(bits[i])
        }
        return line
    }

    fun coordinates(row: PhantomRosterRow): String = "${row.x} ${row.y} ${row.z}"

    private fun dye(id: Int): Component? {
        if (id !in dyeNames.indices) return null
        return Component.translatable("color.minecraft.${dyeNames[id]}")
    }
}
