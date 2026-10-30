package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.TamedPhantomsMod
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import com.tamedphantoms.mod.platform.ModNetwork
import com.tamedphantoms.mod.platform.storedData
import net.minecraft.world.entity.player.Player

/**
 * Книга выдаётся, если стоит мод книг: Patchouli на 1.21.1, Modonomicon на 26.3.
 * Без них этот обработчик ничего не делает.
 */
object PhantomGuideHandler {

    const val BOOK_ID = "tamedphantoms:phantom_guide"
    private const val PATCHOULI_ID = "patchouli"
    private const val MODONOMICON_ID = "modonomicon"
    private const val TAG_RECEIVED = "tamedphantoms.received_guide"

    fun isPatchouliLoaded(): Boolean = ModNetwork.isModLoaded(PATCHOULI_ID)

    fun isModonomiconLoaded(): Boolean = ModNetwork.isModLoaded(MODONOMICON_ID)

    fun onPlayerLogin(player: Player) {
        if (player.level().isClientSide) return
        if (player.storedData.getBoolean(TAG_RECEIVED)) return

        val book = createBookStack() ?: return
        if (!player.addItem(book)) {
            player.drop(book, false)
        }
        player.storedData.putBoolean(TAG_RECEIVED, true)
    }

    /**
     * Смерть и выход из Энда создают нового игрока, а постоянные данные мода
     * сами не переезжают. Без этого книга выдавалась бы снова при следующем входе.
     */
    fun copyTo(newPlayer: Player, oldPlayer: Player) {
        if (oldPlayer.storedData.getBoolean(TAG_RECEIVED)) {
            newPlayer.storedData.putBoolean(TAG_RECEIVED, true)
        }
    }

    fun createBookStack(): ItemStack? = when {
        isPatchouliLoaded() -> bookStack("patchouli:guide_book", "patchouli:book")
        isModonomiconLoaded() -> bookStack("modonomicon:modonomicon", "modonomicon:book_id")
        else -> null
    }

    /** Предмет книги мода книг и компонент с её id. */
    private fun bookStack(itemId: String, componentId: String): ItemStack? {
        val item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemId)).orElse(null) ?: return null
        val componentType = BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(ResourceLocation.parse(componentId)).orElse(null)
            ?: return null
        val stack = ItemStack(item)
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
                TamedPhantomsMod.LOGGER.warn("Не удалось создать книгу {}.", itemId, error)
                null
            }
        }
    }
}
