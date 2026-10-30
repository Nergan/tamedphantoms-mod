package com.tamedphantoms.mod.loader.fabric

import com.tamedphantoms.mod.network.PhantomRosterPayload
import com.tamedphantoms.mod.platform.ModNetwork
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry
import me.shedaniel.clothconfig2.api.ConfigCategory
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import java.util.Optional

/** Кнопка в клиентских настройках: координаты своих фантомов в чат. */
object RosterEntry {

    fun add(category: ConfigCategory) {
        category.addEntry(Button())
    }

    private class Button : AbstractConfigListEntry<Boolean>(
        Component.translatable("tamedphantoms.configuration.roster"),
        false,
    ) {
        private var boxX = 0
        private var boxY = 0
        private var boxW = 0
        private var boxH = 0

        override fun getValue(): Boolean = false

        override fun getDefaultValue(): Optional<Boolean> = Optional.of(false)

        override fun save() = Unit

        override fun children(): MutableList<out GuiEventListener> = mutableListOf()

        override fun narratables(): MutableList<out NarratableEntry> = mutableListOf()

        override fun extractRenderState(
            graphics: GuiGraphicsExtractor,
            index: Int,
            y: Int,
            x: Int,
            entryWidth: Int,
            entryHeight: Int,
            mouseX: Int,
            mouseY: Int,
            isHovered: Boolean,
            delta: Float,
        ) {
            boxX = x
            boxY = y
            boxW = entryWidth
            boxH = entryHeight
            graphics.fill(x, y, x + entryWidth, y + entryHeight - 2, 0xFF2B2B2B.toInt())
            graphics.centeredText(
                Minecraft.getInstance().font,
                Component.translatable("tamedphantoms.configuration.roster"),
                x + entryWidth / 2,
                y + 6,
                0xFFFFFF,
            )
        }

        override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
            if (event.button() != 0) return false
            val mouseX = event.x()
            val mouseY = event.y()
            if (mouseX < boxX || mouseX > boxX + boxW || mouseY < boxY || mouseY > boxY + boxH) return false
            ModNetwork.sendToServer(PhantomRosterPayload)
            return true
        }
    }
}
