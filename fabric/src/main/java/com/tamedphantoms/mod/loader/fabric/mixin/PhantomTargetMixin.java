package com.tamedphantoms.mod.loader.fabric.mixin;

import com.tamedphantoms.mod.event.PhantomTemptHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class PhantomTargetMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void tamedphantoms$target(LivingEntity target, CallbackInfo ci) {
        if (PhantomTemptHandler.INSTANCE.shouldCancelTarget((Mob) (Object) this, target)) {
            ci.cancel();
        }
    }
}
