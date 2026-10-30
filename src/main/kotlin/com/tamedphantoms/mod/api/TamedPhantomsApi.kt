package com.tamedphantoms.mod.api

import com.tamedphantoms.mod.TamedPhantomsMod
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Точки входа для аддонов. Обработчики вызываются на сервере.
 * Облик фантома (краска тела и седла, свечение) хранит и рисует сам этот мод:
 * см. [TamedPhantomEntity.bodyDye], [TamedPhantomEntity.saddleDye], [TamedPhantomEntity.luminous].
 */
object TamedPhantomsApi {

    /** Часть фантома, по которой пришёлся клик. */
    enum class Part { BODY, SADDLE }

    fun interface Interaction {
        /** null — клик не этого аддона. Иначе он поглощён, встроенные действия не выполняются. */
        fun onInteract(player: Player, phantom: TamedPhantomEntity, hand: InteractionHand, stack: ItemStack, part: Part): InteractionResult?
    }

    fun interface ReleaseListener {
        /** Хозяина уже нет, седло ещё на фантоме. */
        fun onReleased(phantom: TamedPhantomEntity, previousOwner: Player?)
    }

    private val interactions = CopyOnWriteArrayList<Interaction>()
    private val releaseListeners = CopyOnWriteArrayList<ReleaseListener>()

    @JvmStatic
    fun registerInteraction(handler: Interaction) {
        interactions += handler
    }

    @JvmStatic
    fun registerReleaseListener(listener: ReleaseListener) {
        releaseListeners += listener
    }

    internal fun interact(player: Player, phantom: TamedPhantomEntity, hand: InteractionHand, stack: ItemStack, part: Part): InteractionResult? {
        for (handler in interactions) {
            val result = try {
                handler.onInteract(player, phantom, hand, stack, part)
            } catch (error: Throwable) {
                TamedPhantomsMod.LOGGER.warn("Аддон упал на клике по фантому.", error)
                null
            }
            if (result != null) return result
        }
        return null
    }

    internal fun released(phantom: TamedPhantomEntity, previousOwner: Player?) {
        for (listener in releaseListeners) {
            try {
                listener.onReleased(phantom, previousOwner)
            } catch (error: Throwable) {
                TamedPhantomsMod.LOGGER.warn("Аддон упал на освобождении фантома.", error)
            }
        }
    }
}
