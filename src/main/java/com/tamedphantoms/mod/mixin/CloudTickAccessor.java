package com.tamedphantoms.mod.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Счётчик, которым ваниль сдвигает облака. Он не равен игровому времени:
 * облака отсчитывают тики с создания отрисовщика.
 */
@Mixin(LevelRenderer.class)
public interface CloudTickAccessor {

    @Accessor("ticks")
    int tamedphantoms$ticks();
}
