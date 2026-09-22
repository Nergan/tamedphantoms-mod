package com.tamedphantoms.mod.util

import com.tamedphantoms.mod.config.ServerConfig
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack

/**
 * Когда фантом должен непрерывно смотреть на игрока с предметом в руке.
 * Прирученный — на любую еду. Освобождённый и дикий — на предмет приручения из конфига.
 */
object PhantomHeldLook {

    const val RANGE = 16.0

    fun shouldStare(tamed: Boolean, holdsFood: Boolean, holdsTameItem: Boolean): Boolean =
        if (tamed) holdsFood else holdsTameItem

    fun nearestHoldingTameItem(phantom: Phantom): Player? = nearest(phantom, ::holdsTameItem)

    fun nearest(phantom: Phantom, predicate: (Player) -> Boolean): Player? {
        val box = phantom.boundingBox.inflate(RANGE)
        return phantom.level().getEntitiesOfClass(Player::class.java, box) { player ->
            player.isAlive && !player.isSpectator && predicate(player)
        }.minByOrNull { phantom.distanceToSqr(it) }
    }

    fun holdsFood(player: Player): Boolean =
        isFood(player.mainHandItem) || isFood(player.offhandItem)

    fun holdsTameItem(player: Player): Boolean {
        val item = ServerConfig.CONFIG.resolveTameItem()
        return player.mainHandItem.`is`(item) || player.offhandItem.`is`(item)
    }

    fun isFood(stack: ItemStack): Boolean = stack.get(DataComponents.FOOD) != null
}
