package com.tamedphantoms.mod.mixin

import com.tamedphantoms.mod.client.PhantomCockpitCamera
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/**
 * Прицел своего игрока в первом лице верхом идёт из глаз на спине фантома и туда, куда смотрит
 * камера ([PhantomCockpitCamera]). Иначе перекрестие показывало бы одно, а клик попадал в другое.
 */
@Mixin(Entity::class)
abstract class PhantomCockpitAimMixin {

    @Inject(method = ["getEyePosition(F)Lnet/minecraft/world/phys/Vec3;"], at = [At("HEAD")], cancellable = true)
    private fun tamedphantomsCockpitEye(partialTick: Float, cir: CallbackInfoReturnable<Vec3>) {
        val eye = PhantomCockpitCamera.aimEye(this, partialTick) ?: return
        cir.returnValue = eye
    }

    @Inject(method = ["getViewVector(F)Lnet/minecraft/world/phys/Vec3;"], at = [At("HEAD")], cancellable = true)
    private fun tamedphantomsCockpitLook(partialTick: Float, cir: CallbackInfoReturnable<Vec3>) {
        val look = PhantomCockpitCamera.aimLook(this, partialTick) ?: return
        cir.returnValue = look
    }
}
