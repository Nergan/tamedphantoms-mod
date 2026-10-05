package com.tamedphantoms.mod.loader.fabric.mixin;

import com.tamedphantoms.mod.entity.TamedPhantomEntity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** На Fabric утопление идёт через ванильный метод, без FluidType. */
@Mixin(LivingEntity.class)
public abstract class PhantomBreathMixin {

    @Inject(method = "canBreatheUnderwater", at = @At("HEAD"), cancellable = true)
    private void tamedphantoms$breathe(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof TamedPhantomEntity) {
            cir.setReturnValue(true);
        }
    }
}
