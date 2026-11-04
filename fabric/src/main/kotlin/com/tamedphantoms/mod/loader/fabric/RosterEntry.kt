package com.tamedphantoms.mod.loader.fabric

import com.tamedphantoms.mod.network.PhantomRosterPayload
import com.tamedphantoms.mod.platform.ModNetwork
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry
import me.shedaniel.clothconfig2.api.ConfigCategory
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.network.chat.Component
import java.util.Optional

/** Кнопка в клиентских настройках: координаты своих фантомов в чат. */
object RosterEntry {

    fun add(category: ConfigCategory) {
        category.addEntry(Where())
    }

    private class Where : AbstractConfigListEntry<Boolean>(
        Component.translatable("tamedphantoms.configuration.roster"),
        false,
    ) {
        private val press: Button = Button.builder(Component.translatable("tamedphantoms.configuration.roster")) {
            ModNetwork.sendToServer(PhantomRosterPayload)
        }.bounds(0, 0, 200, 20).build()

        override fun getValue(): Boolean = false

        override fun getDefaultValue(): Optional<Boolean> = Optional.of(false)

        override fun save() = Unit

        override fun children(): MutableList<GuiEventListener> = mutableListOf(press)

        override fun narratables(): MutableList<NarratableEntry> = mutableListOf(press)

        override fun render(
            graphics: GuiGraphics,
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
            press.setX(x)
            press.setY(y)
            press.setWidth(entryWidth)
            press.setHeight((entryHeight - 2).coerceAtLeast(18))
            press.render(graphics, mouseX, mouseY, delta)
        }

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            if (button != 0) return false
            return press.mouseClicked(mouseX, mouseY, button)
        }
    }
}
