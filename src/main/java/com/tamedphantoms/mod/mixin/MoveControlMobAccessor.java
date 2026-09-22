package com.tamedphantoms.mod.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MoveControl.class)
public interface MoveControlMobAccessor {

    @Accessor("mob")
    Mob tamedphantoms$mob();
}
