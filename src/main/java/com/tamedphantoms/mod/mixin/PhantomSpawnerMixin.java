package com.tamedphantoms.mod.mixin;

import com.tamedphantoms.mod.config.ServerConfig;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Порог бессонницы: ванильные 72000 тиков (3 суток) заменяются настройкой сервера. */
@Mixin(PhantomSpawner.class)
public abstract class PhantomSpawnerMixin {

    @ModifyConstant(method = "tick", constant = @Constant(intValue = 72000))
    private int tamedphantoms$insomniaTicks(int original) {
        int days = ServerConfig.Companion.getCONFIG().getInsomniaDays().get();
        return Math.max(0, days) * 24000;
    }
}
