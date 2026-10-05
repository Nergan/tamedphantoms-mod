package com.tamedphantoms.mod.mixin;

import com.tamedphantoms.mod.entity.TamedPhantomEntity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.extensions.ILivingEntityExtension;
import net.neoforged.neoforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Метод живёт в интерфейсе NeoForge, а не в самом LivingEntity.
 * Ручной фантом не тонет ни в воде, ни в жидкостях других модов.
 */
@Mixin(value = ILivingEntityExtension.class, remap = false)
public interface TamedPhantomFluidMixin {

    @Inject(method = "canDrownInFluidType", at = @At("HEAD"), cancellable = true, remap = false)
    private void tamedphantoms$noDrown(FluidType type, CallbackInfoReturnable<Boolean> cir) {
        if ((LivingEntity) (Object) this instanceof TamedPhantomEntity) {
            cir.setReturnValue(false);
        }
    }
}
