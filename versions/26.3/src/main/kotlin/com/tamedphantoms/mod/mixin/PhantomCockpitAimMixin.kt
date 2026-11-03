package com.tamedphantoms.mod.mixin

import com.tamedphantoms.mod.client.PhantomCockpitCamera
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

/**
 * Прицел в первом лице верхом берётся из глаз на спине. Нет метода — миксин молчит.
 */
@Mixin(Entity::class)
abstract class PhantomCockpitAimMixin {

    @Inject(method = ["getEyePosition(F)Lnet/minecraft/world/phys/Vec3;"], at = [At("HEAD")], cancellable = true, require = 0)
    private fun tamedphantomsCockpitEye(partialTick: Float, cir: CallbackInfoReturnable<Vec3>) {
        val eye = PhantomCockpitCamera.aimEye(this, partialTick) ?: return
        cir.returnValue = eye
    }

    @Inject(method = ["getViewVector(F)Lnet/minecraft/world/phys/Vec3;"], at = [At("HEAD")], cancellable = true, require = 0)
    private fun tamedphantomsCockpitLook(partialTick: Float, cir: CallbackInfoReturnable<Vec3>) {
        val look = PhantomCockpitCamera.aimLook(this, partialTick) ?: return
        cir.returnValue = look
    }
}
