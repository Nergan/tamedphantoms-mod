package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.platform.ModAccess
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.entity.ai.WildPhantomTemptGoal
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.entity.player.Player
import java.util.Collections
import java.util.WeakHashMap

/**
 * Дикий фантом, пока игрок держит предмет приручения: не атакует и летит к нему.
 * Цель с приоритетом 0 глушит ванильное кружение; цель атаки сбрасывается событием.
 */
object PhantomTemptHandler {

    private val injected = Collections.newSetFromMap(WeakHashMap<Phantom, Boolean>())

    fun onJoin(entity: Entity) {
        if (entity.level().isClientSide) return
        val phantom = entity as? Phantom ?: return
        if (phantom is TamedPhantomEntity) return
        if (!injected.add(phantom)) return
        ModAccess.addGoal(phantom, 0, WildPhantomTemptGoal(phantom))
    }

    fun shouldCancelTarget(entity: LivingEntity, next: LivingEntity?): Boolean {
        val phantom = entity as? Phantom ?: return false
        if (phantom is TamedPhantomEntity) {
            return phantom.tamed && next !== phantom.getDefendTarget()
        }
        return next is Player && isHoldingTameItem(next)
    }

    private fun isHoldingTameItem(player: Player): Boolean {
        val tameItem = ServerConfig.resolveTameItem()
        return player.mainHandItem.`is`(tameItem) || player.offhandItem.`is`(tameItem)
    }
}
