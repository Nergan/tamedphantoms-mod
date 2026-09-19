package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.entity.ai.WildPhantomTemptGoal
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent
import java.util.Collections
import java.util.WeakHashMap

/**
 * Дикий фантом, пока игрок держит предмет приручения: не атакует и летит к нему.
 * Цель с приоритетом 0 глушит ванильное кружение; цель атаки сбрасывается событием.
 */
object PhantomTemptHandler {

    private val injected = Collections.newSetFromMap(WeakHashMap<Phantom, Boolean>())

    @SubscribeEvent
    fun onJoin(event: EntityJoinLevelEvent) {
        if (event.level.isClientSide) return
        val phantom = event.entity as? Phantom ?: return
        if (phantom is TamedPhantomEntity) return
        if (!injected.add(phantom)) return
        phantom.goalSelector.addGoal(0, WildPhantomTemptGoal(phantom))
    }

    @SubscribeEvent
    fun onChangeTarget(event: LivingChangeTargetEvent) {
        val phantom = event.entity as? Phantom ?: return
        if (phantom is TamedPhantomEntity) {
            if (phantom.tamed && event.originalAboutToBeSetTarget !== phantom.getDefendTarget()) {
                event.isCanceled = true
            }
            return
        }
        val incoming = event.originalAboutToBeSetTarget
        if (incoming is Player && isHoldingTameItem(incoming)) {
            event.isCanceled = true
        }
    }

    private fun isHoldingTameItem(player: Player): Boolean {
        val tameItem = ServerConfig.CONFIG.resolveTameItem()
        return player.mainHandItem.`is`(tameItem) || player.offhandItem.`is`(tameItem)
    }
}
