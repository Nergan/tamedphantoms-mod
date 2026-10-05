package com.tamedphantoms.mod.loader.fabric.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** goalSelector у Mob защищён. NeoForge открывает поле, Fabric читает его так. */
@Mixin(Mob.class)
public interface MobGoalAccessor {

    @Accessor("goalSelector")
    GoalSelector tamedphantomsGoals();
}
