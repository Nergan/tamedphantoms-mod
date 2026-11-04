package com.tamedphantoms.mod.client

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

/** Список своих фантомов: имя, краска, свечение и координаты, которые можно скопировать. */
class PhantomRosterScreen(private val parent: Screen) : Screen(Component.translatable("tamedphantoms.configuration.roster")) {
    private var scroll = 0
    private var copiedUntil = 0L
    private val rows = ArrayList<PhantomRosterClientRow>()

    override fun init() {
        PhantomRosterClient.listener = {
            if (minecraft?.gui?.screen() === this) rebuild()
        }
        rebuild()
        PhantomRosterClient.request()
    }

    override fun removed() {
        PhantomRosterClient.listener = null
        super.removed()
    }

    override fun onClose() {
        PhantomRosterClient.listener = null
        PhantomRosterClient.hush()
        runCatching {
            val field = parent.javaClass.getField("selectedCategoryIndex")
            field.setInt(parent, 0)
        }
        minecraft?.gui?.setScreen(parent)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean {
        val limit = maxScroll()
        if (limit <= 0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
        scroll = (scroll - (scrollY * 16).toInt()).coerceIn(0, limit)
        place()
        return true
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        this.extractTransparentBackground(graphics)
        graphics.centeredText(font, title, width / 2, 8, 0xFFFFFFFF.toInt())
        if (!PhantomRosterClient.ready) {
            graphics.centeredText(font, Component.translatable("tamedphantoms.roster.waiting"), width / 2, 40, 0xFFA0A0A0.toInt())
        } else if (PhantomRosterClient.rows.isEmpty()) {
            graphics.centeredText(font, Component.translatable("tamedphantoms.roster.none"), width / 2, 40, 0xFFA0A0A0.toInt())
        } else {
            for (row in rows) {
                if (row.y < listTop() - 4 || row.y > listBottom()) continue
                graphics.text(font, PhantomRosterClient.title(row.source), 16, row.y, 0xFFFFFFFF.toInt())
                graphics.text(font, PhantomRosterClient.details(row.source), 16, row.y + 12, 0xFFC0C0C0.toInt())
            }
        }
        if (System.nanoTime() < copiedUntil) {
            graphics.centeredText(font, Component.translatable("tamedphantoms.roster.copied"), width / 2, height - 46, 0xFF55FF55.toInt())
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
    }

    private fun rebuild() {
        clearWidgets()
        rows.clear()
        scroll = scroll.coerceIn(0, maxScroll())
        addRenderableWidget(
            Button.builder(Component.translatable("gui.back")) { onClose() }
                .bounds(width / 2 - 154, height - 28, 150, 20)
                .build(),
        )
        addRenderableWidget(
            Button.builder(Component.translatable("tamedphantoms.roster.refresh")) { PhantomRosterClient.request() }
                .bounds(width / 2 + 4, height - 28, 150, 20)
                .build(),
        )
        if (!PhantomRosterClient.ready || PhantomRosterClient.rows.isEmpty()) return
        val boxWidth = (width - 16 - 16 - 78).coerceAtLeast(80)
        for (source in PhantomRosterClient.rows) {
            val box = EditBox(font, 16, 0, boxWidth, 18, Component.translatable("tamedphantoms.roster.copy"))
            box.value = PhantomRosterClient.coordinates(source)
            box.setMaxLength(64)
            val copy = Button.builder(Component.translatable("tamedphantoms.roster.copy")) { _ ->
                minecraft?.keyboardHandler?.setClipboard(box.value)
                copiedUntil = System.nanoTime() + 1_500_000_000L
            }.bounds(16 + boxWidth + 4, 0, 74, 18).build()
            addRenderableWidget(box)
            addRenderableWidget(copy)
            rows.add(PhantomRosterClientRow(source, box, copy, 0))
        }
        place()
    }

    private fun place() {
        val top = listTop()
        val bottom = listBottom()
        for ((index, row) in rows.withIndex()) {
            val y = top + index * ROW - scroll
            row.y = y
            val shown = y + 26 >= top && y + 44 <= bottom
            row.box.setX(16)
            row.box.setY(y + 26)
            row.copy.setX(16 + row.box.getWidth() + 4)
            row.copy.setY(y + 26)
            row.box.visible = shown
            row.copy.visible = shown
            row.box.active = shown
            row.copy.active = shown
        }
    }

    private fun listTop(): Int = 28

    private fun listBottom(): Int = height - 50

    private fun maxScroll(): Int = (PhantomRosterClient.rows.size * ROW - (listBottom() - listTop())).coerceAtLeast(0)

    private class PhantomRosterClientRow(
        val source: com.tamedphantoms.mod.entity.PhantomRosterRow,
        val box: EditBox,
        val copy: Button,
        var y: Int,
    )

    companion object {
        private const val ROW = 48
    }
}
