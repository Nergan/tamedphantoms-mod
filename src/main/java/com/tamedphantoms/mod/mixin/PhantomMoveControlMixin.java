package com.tamedphantoms.mod.mixin;

import com.tamedphantoms.mod.util.PhantomFlightAvoidance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Phantom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ванильный фантом доворачивает корпус к точке полёта на 4° за тик.
 * Тот же шаг, что у ручного, чтобы к игроку с предметом он поворачивался быстрее.
 * После шага полёта скорость ещё раз правится: стволы и опасные блоки обходятся.
 */
@Mixin(targets = "net.minecraft.world.entity.monster.Phantom$PhantomMoveControl")
public abstract class PhantomMoveControlMixin {

    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 4.0F))
    private float tamedphantoms$fasterTurn(float original) {
        return 12.0F;
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void tamedphantoms$avoid(CallbackInfo ci) {
        Mob mob = ((MoveControlMobAccessor) (Object) this).tamedphantoms$mob();
        if (mob instanceof Phantom phantom) {
            PhantomFlightAvoidance.INSTANCE.apply(phantom);
        }
    }
}
