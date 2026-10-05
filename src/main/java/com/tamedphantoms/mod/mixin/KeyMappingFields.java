package com.tamedphantoms.mod.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Доступ к текущей клавише и счётчику нажатий, оба поля приватные. */
@Mixin(KeyMapping.class)
public interface KeyMappingFields {

    @Accessor("key")
    InputConstants.Key tamedphantoms$key();

    @Accessor("clickCount")
    int tamedphantoms$clickCount();

    @Accessor("clickCount")
    void tamedphantoms$clickCount(int value);
}
