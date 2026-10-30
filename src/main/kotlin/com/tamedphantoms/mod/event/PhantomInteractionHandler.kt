package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.TamedPhantomsMod
import com.tamedphantoms.mod.api.TamedPhantomsApi
import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.PhantomHeldLook
import com.tamedphantoms.mod.util.PhantomHitParts
import com.tamedphantoms.mod.util.PhantomTamingLogic
import com.tamedphantoms.mod.util.PhantomTreats
import net.minecraft.world.phys.Vec3
import java.util.WeakHashMap
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

    private class Hit(val entityId: Int, val tick: Long, val pos: Vec3)

    private val lastHit = WeakHashMap<Player, Hit>()

    /**
     * Точка клика приходит отдельным пакетом прямо перед обычным кликом.
     * На NeoForge её видит только [TamedPhantomEntity.interactAt], поэтому она запоминается здесь.
     */
    fun rememberHit(player: Player, phantom: TamedPhantomEntity, worldPos: Vec3) {
        lastHit[player] = Hit(phantom.id, player.tickCount.toLong(), worldPos)
    }

    /**
     * @return результат, которым надо отменить взаимодействие, или null, если его не трогать.
     * Обе руки шлют клик по сущности: пустая вторая рука тут же снимала «сидеть».
     * @param hitPos точка клика в мире, если загрузчик её знает
     */
    fun onEntityInteract(player: Player, target: Entity, hand: InteractionHand, hitPos: Vec3? = null): InteractionResult? {
        when (target) {
            is TamedPhantomEntity -> {
                if (hand == InteractionHand.MAIN_HAND && !player.level().isClientSide) {
                    val stack = player.getItemInHand(hand)
                    val part = partOf(target, hitOf(player, target, hitPos))
                    if (TamedPhantomsApi.interact(player, target, hand, stack, part) == null) {
                        handleTamedPhantomEntity(player, target, stack)
                    }
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

    private fun hitOf(player: Player, phantom: TamedPhantomEntity, given: Vec3?): Vec3? {
        if (given != null) return given
        val hit = lastHit.remove(player) ?: return null
        if (hit.entityId != phantom.id || player.tickCount - hit.tick > 1) return null
        return hit.pos
    }

    private fun partOf(phantom: TamedPhantomEntity, hit: Vec3?): TamedPhantomsApi.Part {
        if (hit == null) return TamedPhantomsApi.Part.BODY
        val saddle = PhantomHitParts.onSaddle(
            hit.x - phantom.x,
            hit.y - phantom.y,
            hit.z - phantom.z,
            phantom.yBodyRot,
            phantom.bbHeight.toDouble(),
            phantom.bbWidth / 0.9,
            phantom.isSaddled,
            phantom.isOrderedToSit(),
        )
        return if (saddle) TamedPhantomsApi.Part.SADDLE else TamedPhantomsApi.Part.BODY
    }

    /**
     * Кормление и инструменты аддонов верхом. null — ванильное использование предмета.
     * На клиенте ответ SUCCESS: Fabric отправляет клик на сервер только с ним.
     */
    fun onUseItem(player: Player, hand: InteractionHand): InteractionResult? {
        val phantom = player.vehicle as? TamedPhantomEntity ?: return null
        if (!phantom.tamed) return null
        val stack = player.getItemInHand(hand)
        riderToolUse(player, phantom, hand, stack)?.let { return it }
        if (PhantomHeldLook.isRefusal(stack) && !phantom.isOwnedBy(player)) {
            if (player.level().isClientSide) return InteractionResult.SUCCESS
            phantom.rejectOffering(player)
            return InteractionResult.CONSUME
        }
        val releaseItem = ServerConfig.resolveReleaseItem()
        if (stack.`is`(releaseItem) && !stack.`is`(Items.POISONOUS_POTATO)) return null
        val treat = stack.`is`(Items.POTION) || stack.`is`(Items.MILK_BUCKET) || stack.get(DataComponents.FOOD) != null
        if (!treat) return null
        if (player.level().isClientSide) return InteractionResult.SUCCESS
        val used = when {
            stack.`is`(Items.MILK_BUCKET) -> PhantomTreats.tryMilk(player, phantom, stack)
            stack.`is`(Items.POTION) -> PhantomTreats.tryPotion(player, phantom, stack)
            else -> PhantomTreats.tryFood(player, phantom, stack)
        }
        return if (used) InteractionResult.CONSUME else null
    }

    /** Инструмент аддона верхом: клик по той части фантома, куда смотрит седок, см. [TamedPhantomsApi.RiderTool]. */
    private fun riderToolUse(player: Player, phantom: TamedPhantomEntity, hand: InteractionHand, stack: ItemStack): InteractionResult? {
        if (stack.isEmpty || !TamedPhantomsApi.isRiderTool(stack)) return null
        val eye = player.getEyePosition(1f)
        val reach = eye.add(player.getViewVector(1f).scale(RIDER_REACH))
        val hit = phantom.boundingBox.inflate(RIDER_PICK_MARGIN).clip(eye, reach).orElse(null) ?: return null
        if (player.level().isClientSide) return InteractionResult.SUCCESS
        return TamedPhantomsApi.interact(player, phantom, hand, stack, partOf(phantom, hit))
    }

    private const val RIDER_REACH = 5.0
    private const val RIDER_PICK_MARGIN = 0.3

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
            val post = PhantomLeashTie.postBehind(player)
            if (post != null && PhantomLeashTie.onUseBlock(player, player.level(), post) != null) return
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

        if (PhantomTreats.tryMilk(player, phantom, stack)) return
        if (PhantomTreats.tryPotion(player, phantom, stack)) return
        if (stack.get(DataComponents.FOOD) != null || stack.get(DataComponents.SUSPICIOUS_STEW_EFFECTS) != null) {
            PhantomTreats.tryFood(player, phantom, stack)
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
