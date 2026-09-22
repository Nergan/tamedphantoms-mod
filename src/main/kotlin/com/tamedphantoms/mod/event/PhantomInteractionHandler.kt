package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.TamedPhantomsMod
import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.PhantomTamingLogic
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent

object PhantomInteractionHandler {

    @SubscribeEvent
    fun onInteract(event: PlayerInteractEvent.EntityInteract) {
        val player = event.entity

        when (val target = event.target) {
            is TamedPhantomEntity -> {
                event.setCanceled(true)
                event.cancellationResult = InteractionResult.SUCCESS
                // Обе руки шлют EntityInteract: пустая вторая рука тут же снимала «сидеть».
                if (event.hand != InteractionHand.MAIN_HAND) return
                if (player.level().isClientSide) return
                handleTamedPhantomEntity(player, target, player.getItemInHand(event.hand))
            }
            is Phantom -> {
                val stack = player.getItemInHand(event.hand)
                val tameItem: Item = ServerConfig.CONFIG.resolveTameItem()
                if (stack.`is`(tameItem)) {
                    event.setCanceled(true)
                    handleWildPhantom(player, target, stack)
                }
            }
            else -> Unit
        }
    }

    private fun handleWildPhantom(player: Player, phantom: Phantom, stack: ItemStack) {
        val level = phantom.level()
        if (level.isClientSide) return

        val roll = phantom.random.nextFloat().toDouble()
        val success = PhantomTamingLogic.rollTameSuccess(ServerConfig.CONFIG.tameChance.get(), roll)

        if (!player.abilities.instabuild) {
            stack.shrink(1)
        }

        if (!success) {
            return
        }

        val tamed = TamedPhantomEntity(com.tamedphantoms.mod.entity.ModEntities.TAMED_PHANTOM.get(), level)
        tamed.moveTo(phantom.x, phantom.y, phantom.z, phantom.yRot, phantom.xRot)
        tamed.health = phantom.health

        phantom.discard()
        level.addFreshEntity(tamed)
        tamed.tameTo(player)

        if (player is ServerPlayer) {
            ModAdvancements.grantTamePhantom(player)
        }
    }

    private fun handleTamedPhantomEntity(player: Player, phantom: TamedPhantomEntity, stack: ItemStack) {
        if (phantom.leashHolder === player) {
            phantom.dropLeash(true, !player.abilities.instabuild)
            return
        }

        if (stack.`is`(Items.LEAD) && !phantom.tamed) {
            phantom.setLeashedTo(player, true)
            if (phantom.isLeashed && !player.abilities.instabuild) {
                stack.shrink(1)
            }
            phantom.boltFromLeash(player, dropLead = !player.abilities.instabuild)
            return
        }

        if (stack.`is`(Items.LEAD) && phantom.tamed && phantom.canHaveALeashAttachedToIt()) {
            phantom.setLeashedTo(player, true)
            if (!player.abilities.instabuild) {
                stack.shrink(1)
            }
            return
        }

        if (!phantom.tamed) {
            val tameItem = ServerConfig.CONFIG.resolveTameItem()
            if (stack.`is`(tameItem)) {
                retame(player, phantom, stack)
            }
            return
        }

        handleOwnedPhantom(player, phantom, stack)
    }

    private fun retame(player: Player, phantom: TamedPhantomEntity, stack: ItemStack) {
        val roll = phantom.random.nextFloat().toDouble()
        if (!player.abilities.instabuild) {
            stack.shrink(1)
        }
        if (!PhantomTamingLogic.rollTameSuccess(ServerConfig.CONFIG.tameChance.get(), roll)) {
            return
        }
        phantom.tameTo(player)
        if (player is ServerPlayer) {
            ModAdvancements.grantTamePhantom(player)
        }
    }

    private fun handleOwnedPhantom(player: Player, phantom: TamedPhantomEntity, stack: ItemStack) {
        val isOwner = phantom.isOwnedBy(player)

        if (isOwner && stack.`is`(Items.SHEARS)) {
            phantom.removeSaddle()
            return
        }

        val releaseItem = ServerConfig.CONFIG.resolveReleaseItem()
        if (isOwner && stack.`is`(releaseItem)) {
            if (!player.abilities.instabuild) stack.shrink(1)
            phantom.release()
            if (player is ServerPlayer) {
                ModAdvancements.grantReleasePhantom(player)
            }
            return
        }

        if (isOwner && stack.`is`(Items.SADDLE) && !phantom.isSaddled) {
            val saddleStack = stack.copy()
            if (!player.abilities.instabuild) stack.shrink(1)
            phantom.equipSaddle(saddleStack, SoundSource.NEUTRAL)
            return
        }

        val food = stack.get(DataComponents.FOOD)
        if (isOwner && food != null) {
            val healed = phantom.healWithFood(food.nutrition())
            if (healed && !player.abilities.instabuild) {
                stack.shrink(1)
            }
            return
        }

        if (player.vehicle === phantom) {
            return
        }

        val wantsSit = isOwner && (!phantom.isSaddled || player.isShiftKeyDown)
        if (wantsSit) {
            phantom.setOrderedToSit(!phantom.isOrderedToSit())
            return
        }

        if (phantom.isOrderedToSit()) {
            return
        }

        if (phantom.isSaddled && !player.isShiftKeyDown) {
            val mounted = player.startRiding(phantom)
            TamedPhantomsMod.LOGGER.info(
                "Попытка сесть на фантома: игрок={}, результат={}, фантом#{}, tamed={}, saddled={}, isOwner={}",
                player.name.string, mounted, phantom.id, phantom.tamed, phantom.isSaddled, isOwner,
            )
        }
    }
}
