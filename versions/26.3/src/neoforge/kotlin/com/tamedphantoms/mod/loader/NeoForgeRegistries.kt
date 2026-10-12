package com.tamedphantoms.mod.loader

import com.tamedphantoms.mod.TamedPhantomsMod
import com.tamedphantoms.mod.entity.ModEntities
import com.tamedphantoms.mod.item.ModItems
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SpawnEggItem
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object NeoForgeRegistries {

    private val ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, TamedPhantomsMod.MOD_ID)
    private val ITEMS = DeferredRegister.create(Registries.ITEM, TamedPhantomsMod.MOD_ID)
    private val CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TamedPhantomsMod.MOD_ID)

    private val ENTITY_KEY = ResourceKey.create(
        Registries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "tamed_phantom"),
    )

    private val TAMED_PHANTOM = ENTITY_TYPES.register(
        "tamed_phantom",
        Supplier { ModEntities.builder().build(ENTITY_KEY) },
    )

    private val TAB_ICON = ITEMS.register(
        "tab_icon",
        Supplier<Item> {
            Item(Item.Properties().setId(itemKey("tab_icon")))
        },
    )

    private val RELEASED_PHANTOM_SPAWN_EGG = ITEMS.register(
        "released_phantom_spawn_egg",
        Supplier<Item> {
            SpawnEggItem(
                Item.Properties()
                    .setId(itemKey("released_phantom_spawn_egg"))
                    .spawnEgg(ModEntities.TAMED_PHANTOM.get()),
            )
        },
    )

    private val CREATIVE_TAB = CREATIVE_TABS.register(
        "tamedphantoms",
        Supplier<CreativeModeTab> {
            CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.tamedphantoms"))
                .icon { ItemStack(TAB_ICON.get()) }
                .displayItems { _, output -> ModItems.fillTab(output) }
                .build()
        },
    )

    fun register(modBus: IEventBus) {
        ENTITY_TYPES.register(modBus)
        ITEMS.register(modBus)
        CREATIVE_TABS.register(modBus)
        ModEntities.TAMED_PHANTOM = TAMED_PHANTOM
        ModItems.TAB_ICON = TAB_ICON
        ModItems.RELEASED_PHANTOM_SPAWN_EGG = RELEASED_PHANTOM_SPAWN_EGG
        ModItems.CREATIVE_TAB = CREATIVE_TAB
    }

    private fun itemKey(path: String): ResourceKey<Item> = ResourceKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, path),
    )
}
