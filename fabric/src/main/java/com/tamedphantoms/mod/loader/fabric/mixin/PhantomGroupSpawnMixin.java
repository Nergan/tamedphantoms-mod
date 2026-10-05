package com.tamedphantoms.mod.loader.fabric.mixin;

import com.tamedphantoms.mod.event.PhantomSpawnScaler;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class PhantomGroupSpawnMixin {

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void tamedphantoms$group(
        ServerLevelAccessor level,
        DifficultyInstance difficulty,
        MobSpawnType spawnType,
        SpawnGroupData spawnData,
        CallbackInfoReturnable<SpawnGroupData> cir
    ) {
        PhantomSpawnScaler.INSTANCE.onFinalizeSpawn((Mob) (Object) this, level, difficulty, spawnType, spawnData);
    }
}
