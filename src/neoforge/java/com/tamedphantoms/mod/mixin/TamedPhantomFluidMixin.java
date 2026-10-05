package com.tamedphantoms.mod.mixin;

import com.tamedphantoms.mod.entity.TamedPhantomEntity;
import net.neoforged.neoforge.fluids.FluidType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ручной фантом не тонет ни в воде, ни в жидкостях других модов. */
@Mixin(LivingEntity.class)
public abstract class TamedPhantomFluidMixin {

    @Inject(method = "canDrownInFluidType", at = @At("HEAD"), cancellable = true)
    private void tamedphantoms$noDrown(FluidType type, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof TamedPhantomEntity) {
            cir.setReturnValue(false);
        }
    }
}
