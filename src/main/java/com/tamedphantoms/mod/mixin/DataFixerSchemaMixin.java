package com.tamedphantoms.mod.mixin;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.types.Type;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ваниль при сборке типа сущности вызывает {@code Util.fetchChoiceType} и,
 * не найдя модовый id в своей схеме, пишет ERROR. Результат вызова
 * выбрасывается: сохранение сущности от этого не зависит.
 */
@Mixin(Util.class)
public abstract class DataFixerSchemaMixin {

    @Inject(method = "fetchChoiceType", at = @At("HEAD"), cancellable = true)
    private static void tamedphantoms$skipVanillaSchema(
        DSL.TypeReference type,
        String name,
        CallbackInfoReturnable<Type<?>> callback
    ) {
        if ("tamed_phantom".equals(name)) {
            callback.setReturnValue(null);
        }
    }
}
