package com.tamedphantoms.mod.item

import com.tamedphantoms.mod.TamedPhantomsMod
import com.tamedphantoms.mod.entity.ModEntities
import com.tamedphantoms.mod.event.PhantomGuideHandler
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.common.DeferredSpawnEggItem
import net.neoforged.neoforge.registries.DeferredRegister

object ModItems {

    val ITEMS: DeferredRegister<Item> =
        DeferredRegister.create(Registries.ITEM, TamedPhantomsMod.MOD_ID)

    val CREATIVE_TABS: DeferredRegister<CreativeModeTab> =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TamedPhantomsMod.MOD_ID)

    /** Иконка вкладки. В список предметов не кладём. */
    val TAB_ICON = ITEMS.register("tab_icon") { Item(Item.Properties()) }

    /**
     * Яйцо освобождённого фантома: те же пятна, что у ванильного яйца фантома
     * (#43518A / #88FF00), только темнее.
     */
    val RELEASED_PHANTOM_SPAWN_EGG = ITEMS.register("released_phantom_spawn_egg") {
        DeferredSpawnEggItem(
            { ModEntities.TAMED_PHANTOM.get() },
            0x252D4C,
            0x4B8C00,
            Item.Properties(),
        )
    }

    val CREATIVE_TAB = CREATIVE_TABS.register("tamedphantoms") {
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.tamedphantoms"))
            .icon { ItemStack(TAB_ICON.get()) }
            .displayItems { _, output ->
                output.accept(RELEASED_PHANTOM_SPAWN_EGG.get())
                PhantomGuideHandler.createBookStack()?.let(output::accept)
            }
            .build()
    }

    fun register(modBus: IEventBus) {
        ITEMS.register(modBus)
        CREATIVE_TABS.register(modBus)
    }
}
