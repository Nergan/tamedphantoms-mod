package com.tamedphantoms.mod.mixin

import com.tamedphantoms.mod.event.PhantomInteractionHandler
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/**
 * Чужой мод может забрать ПКМ раньше нашего события и вернуть успех.
 * Для бессмертного фантома это оставляет его лежать: клик до посадки не доходит.
 * На NeoForge событие EntityInteract вызывается внутри interactOn, поэтому действие здесь идёт раньше него.
 * На Fabric UseEntityCallback срабатывает до interactOn, там порядок задаёт ранняя фаза события.
 */
@Mixin(value = [Player::class], priority = 2000)
abstract class PhantomInteractMixin {
    @Inject(method = ["interactOn"], at = [At("HEAD")], cancellable = true)
    private fun tamedphantomsFirst(
        target: Entity,
        hand: InteractionHand,
        callback: CallbackInfoReturnable<InteractionResult>,
    ) {
        val player = this as Player
        if (player.level().isClientSide) return
        val result = PhantomInteractionHandler.onEntityInteract(player, target, hand) ?: return
        callback.returnValue = result
    }
}
