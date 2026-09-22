package com.tamedphantoms.mod.util

import com.tamedphantoms.mod.config.ServerConfig
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.UUID

/**
 * Когда фантом должен непрерывно смотреть на игрока с предметом в руке.
 * Прирученный — на любую еду. Освобождённый и дикий — на предмет приручения из конфига.
 */
object PhantomHeldLook {

    const val RANGE = 16.0

    /** Дистанция, на которой фантом замирает: отказ или кивок. */
    const val OFFER_RANGE = 8.0

    enum class Offering { SHAKE, NOD }

    data class OfferTarget(val player: Player, val offering: Offering)

    fun shouldStare(tamed: Boolean, holdsFood: Boolean, holdsTameItem: Boolean): Boolean =
        if (tamed) holdsFood else holdsTameItem

    /** Отказ важнее еды: ядовитая картошка сама является едой. */
    fun classify(refusal: Boolean, food: Boolean): Offering? = when {
        refusal -> Offering.SHAKE
        food -> Offering.NOD
        else -> null
    }

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

    fun isRefusal(stack: ItemStack): Boolean =
        stack.`is`(Items.POISONOUS_POTATO) || stack.`is`(ServerConfig.CONFIG.resolveReleaseItem())

    fun holdsRefusal(player: Player): Boolean =
        isRefusal(player.mainHandItem) || isRefusal(player.offhandItem)

    fun offering(player: Player): Offering? = classify(holdsRefusal(player), holdsFood(player))

    /**
     * Ближайший игрок с отказом или обычной едой.
     * Седок этого фантома не считается. На отказ хозяин важнее ближайшего.
     */
    fun nearestOffer(phantom: Phantom, ownerId: UUID?, range: Double = OFFER_RANGE): OfferTarget? {
        val reach = range * range
        val box = phantom.boundingBox.inflate(range)
        var shake: Player? = null
        var shakeDist = Double.POSITIVE_INFINITY
        var ownerShake: Player? = null
        var nod: Player? = null
        var nodDist = Double.POSITIVE_INFINITY
        val players = phantom.level().getEntitiesOfClass(Player::class.java, box) { player ->
            player.isAlive && !player.isSpectator && !phantom.hasPassenger(player)
        }
        for (player in players) {
            val dist = phantom.distanceToSqr(player)
            if (dist > reach) continue
            when (offering(player)) {
                Offering.SHAKE -> {
                    if (ownerId != null && player.uuid == ownerId) ownerShake = player
                    if (dist < shakeDist) {
                        shake = player
                        shakeDist = dist
                    }
                }
                Offering.NOD -> if (dist < nodDist) {
                    nod = player
                    nodDist = dist
                }
                null -> Unit
            }
        }
        val refusal = ownerShake ?: shake
        if (refusal != null) return OfferTarget(refusal, Offering.SHAKE)
        val treat = nod ?: return null
        return OfferTarget(treat, Offering.NOD)
    }
}
