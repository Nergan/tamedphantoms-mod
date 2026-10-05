package com.tamedphantoms.mod.loader

import com.tamedphantoms.mod.event.PhantomDamageHandler
import com.tamedphantoms.mod.event.PhantomDismount
import com.tamedphantoms.mod.event.PhantomGuideHandler
import com.tamedphantoms.mod.event.PhantomInteractionHandler
import com.tamedphantoms.mod.event.PhantomOwnerRecallHandler
import com.tamedphantoms.mod.event.PhantomSpawnScaler
import com.tamedphantoms.mod.event.PhantomTemptHandler
import com.tamedphantoms.mod.util.OwnerFlightSpeed
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Mob
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent

object NeoForgeGameEvents {

    fun register() {
        val bus = NeoForge.EVENT_BUS
        bus.addListener(::onEntityInteract)
        bus.addListener(::onUseItem)
        bus.addListener(::onDamage)
        bus.addListener(::onJoin)
        bus.addListener(::onChangeTarget)
        bus.addListener(::onLogin)
        bus.addListener(::onRespawn)
        bus.addListener(::onChangedDimension)
        bus.addListener(::onClone)
        bus.addListener(::onFinalizeSpawn)
        bus.addListener(::onTick)
        bus.addListener(::onLogout)
    }

    private fun onEntityInteract(event: PlayerInteractEvent.EntityInteract) {
        val result = PhantomInteractionHandler.onEntityInteract(event.entity, event.target, event.hand) ?: return
        event.isCanceled = true
        event.cancellationResult = result
    }

    private fun onUseItem(event: PlayerInteractEvent.RightClickItem) {
        val result = PhantomInteractionHandler.onUseItem(event.entity, event.hand) ?: return
        event.isCanceled = true
        event.cancellationResult = result
    }

    private fun onDamage(event: LivingDamageEvent.Post) {
        PhantomDamageHandler.onLivingDamage(event.entity, event.source, event.newDamage)
    }

    private fun onJoin(event: EntityJoinLevelEvent) {
        PhantomTemptHandler.onJoin(event.entity)
    }

    private fun onChangeTarget(event: LivingChangeTargetEvent) {
        if (PhantomTemptHandler.shouldCancelTarget(event.entity, event.originalAboutToBeSetTarget)) {
            event.isCanceled = true
        }
    }

    private fun onLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        PhantomGuideHandler.onPlayerLogin(event.entity)
        PhantomOwnerRecallHandler.onLogin(event.entity)
    }

    private fun onRespawn(event: PlayerEvent.PlayerRespawnEvent) {
        PhantomOwnerRecallHandler.onRespawn(event.entity)
    }

    private fun onChangedDimension(event: PlayerEvent.PlayerChangedDimensionEvent) {
        PhantomOwnerRecallHandler.onChangedDimension(event.entity)
    }

    private fun onClone(event: PlayerEvent.Clone) {
        PhantomOwnerRecallHandler.onClone(event.entity, event.original)
    }

    private fun onFinalizeSpawn(event: FinalizeSpawnEvent) {
        val mob = event.entity as? Mob ?: return
        val level = mob.level() as? ServerLevel ?: return
        PhantomSpawnScaler.onFinalizeSpawn(mob, level, event.difficulty, event.spawnType, event.spawnData)
    }

    private fun onTick(event: PlayerTickEvent.Post) {
        PhantomDismount.onPlayerTick(event.entity)
    }

    private fun onLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        PhantomDismount.onLogout(event.entity)
        OwnerFlightSpeed.clear(event.entity.uuid)
    }
}
