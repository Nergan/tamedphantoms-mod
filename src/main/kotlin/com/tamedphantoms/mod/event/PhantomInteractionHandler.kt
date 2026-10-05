package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.TamedPhantomsMod
import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.PhantomHeldLook
import com.tamedphantoms.mod.util.PhantomTamingLogic
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object PhantomInteractionHandler {

    /**
     * @return результат, которым надо отменить взаимодействие, или null, если его не трогать.
     * Обе руки шлют клик по сущности: пустая вторая рука тут же снимала «сидеть».
     */
    fun onEntityInteract(player: Player, target: Entity, hand: InteractionHand): InteractionResult? {
        when (target) {
            is TamedPhantomEntity -> {
                if (hand == InteractionHand.MAIN_HAND && !player.level().isClientSide) {
                    handleTamedPhantomEntity(player, target, player.getItemInHand(hand))
                }
                return InteractionResult.SUCCESS
            }
            is Phantom -> {
                val stack = player.getItemInHand(hand)
                if (stack.`is`(ServerConfig.resolveTameItem())) {
                    handleWildPhantom(player, target, stack)
                    return InteractionResult.SUCCESS
                }
            }
        }
        return null
    }

    /** Кормление верхом. null — ванильное использование предмета. */
    fun onUseItem(player: Player, hand: InteractionHand): InteractionResult? {
        val phantom = player.vehicle as? TamedPhantomEntity ?: return null
        if (!phantom.tamed) return null
        val stack = player.getItemInHand(hand)
        if (PhantomHeldLook.isRefusal(stack) && !phantom.isOwnedBy(player)) {
            if (!player.level().isClientSide) phantom.rejectOffering(player)
            return InteractionResult.CONSUME
        }
        val releaseItem = ServerConfig.resolveReleaseItem()
        if (stack.`is`(releaseItem) && !stack.`is`(Items.POISONOUS_POTATO)) return null
        if (stack.get(DataComponents.FOOD) == null) return null
        if (!PhantomTamingLogic.canHeal(phantom.health, phantom.maxHealth)) return null
        if (player.level().isClientSide) return InteractionResult.CONSUME
        return if (tryHeal(player, phantom, stack)) InteractionResult.CONSUME else null
    }

    private fun handleWildPhantom(player: Player, phantom: Phantom, stack: ItemStack) {
        val level = phantom.level()
        if (level.isClientSide) return

        val roll = phantom.random.nextFloat().toDouble()
        val success = PhantomTamingLogic.rollTameSuccess(ServerConfig.tameChance(), roll)

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
            val tameItem = ServerConfig.resolveTameItem()
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
        if (!PhantomTamingLogic.rollTameSuccess(ServerConfig.tameChance(), roll)) {
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

        val releaseItem = ServerConfig.resolveReleaseItem()
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

        if (PhantomHeldLook.isRefusal(stack) && !isOwner) {
            phantom.rejectOffering(player)
            return
        }

        val food = stack.get(DataComponents.FOOD)
        if (food != null) {
            tryHeal(player, phantom, stack)
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

    /** Обычную еду принимает кто угодно. Картошку и предмет освобождения — только хозяин. */
    private fun tryHeal(player: Player, phantom: TamedPhantomEntity, stack: ItemStack): Boolean {
        if (!phantom.tamed) return false
        if (PhantomHeldLook.isRefusal(stack) && !phantom.isOwnedBy(player)) return false
        val food = stack.get(DataComponents.FOOD) ?: return false
        if (!phantom.healWithFood(food.nutrition())) return false
        if (!player.abilities.instabuild) stack.shrink(1)
        return true
    }
}
