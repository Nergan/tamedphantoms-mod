package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.TamedPhantomsMod
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModList
import net.neoforged.neoforge.event.entity.player.PlayerEvent

/**
 * Книжка Patchouli выдаётся только если мод установлен.
 * Без Patchouli этот обработчик ничего не делает.
 */
object PhantomGuideHandler {

    const val BOOK_ID = "tamedphantoms:phantom_guide"
    private const val PATCHOULI_ID = "patchouli"
    private const val TAG_RECEIVED = "tamedphantoms.received_guide"

    fun isPatchouliLoaded(): Boolean = ModList.get().isLoaded(PATCHOULI_ID)

    @SubscribeEvent
    fun onPlayerLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity
        if (player.level().isClientSide) return
        if (!isPatchouliLoaded()) return
        if (player.persistentData.getBoolean(TAG_RECEIVED)) return

        val book = createBookStack() ?: return
        if (!player.addItem(book)) {
            player.drop(book, false)
        }
        player.persistentData.putBoolean(TAG_RECEIVED, true)
    }

    fun createBookStack(): ItemStack? {
        if (!isPatchouliLoaded()) return null
        val item = BuiltInRegistries.ITEM.get(ResourceLocation.parse("patchouli:guide_book"))
        if (item === Items.AIR) return null
        val stack = ItemStack(item)
        val componentType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse("patchouli:book"))
            ?: return null
        val bookId = ResourceLocation.parse(BOOK_ID)
        return try {
            @Suppress("UNCHECKED_CAST")
            stack.set(componentType as DataComponentType<Any>, bookId)
            stack
        } catch (_: Throwable) {
            try {
                @Suppress("UNCHECKED_CAST")
                stack.set(componentType as DataComponentType<Any>, BOOK_ID)
                stack
            } catch (error: Throwable) {
                TamedPhantomsMod.LOGGER.warn("Не удалось создать книгу Patchouli.", error)
                null
            }
        }
    }

    @Suppress("unused")
    private fun hasBookAlready(player: Player): Boolean {
        val book = createBookStack() ?: return false
        return player.inventory.items.any { matchesGuide(it, book) } ||
            player.inventory.offhand.any { matchesGuide(it, book) }
    }

    private fun matchesGuide(stack: ItemStack, template: ItemStack): Boolean {
        return !stack.isEmpty && ItemStack.isSameItemSameComponents(stack, template)
    }
}
