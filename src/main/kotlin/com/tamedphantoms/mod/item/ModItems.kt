package com.tamedphantoms.mod.item

import com.tamedphantoms.mod.event.PhantomGuideHandler
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import java.util.function.Supplier

object ModItems {

    lateinit var TAB_ICON: Supplier<Item>
    lateinit var RELEASED_PHANTOM_SPAWN_EGG: Supplier<Item>
    lateinit var CREATIVE_TAB: Supplier<CreativeModeTab>

    fun fillTab(output: CreativeModeTab.Output) {
        output.accept(RELEASED_PHANTOM_SPAWN_EGG.get())
        PhantomGuideHandler.createBookStack()?.let { output.accept(it) }
    }
}
