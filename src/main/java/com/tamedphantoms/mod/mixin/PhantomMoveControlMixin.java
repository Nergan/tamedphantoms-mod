package com.tamedphantoms.mod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Ванильный фантом доворачивает корпус к точке полёта на 4° за тик.
 * Тот же шаг, что у ручного, чтобы к игроку с предметом он поворачивался быстрее.
 */
@Mixin(targets = "net.minecraft.world.entity.monster.Phantom$PhantomMoveControl")
public abstract class PhantomMoveControlMixin {

    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 4.0F))
    private float tamedphantoms$fasterTurn(float original) {
        return 12.0F;
    }
}
