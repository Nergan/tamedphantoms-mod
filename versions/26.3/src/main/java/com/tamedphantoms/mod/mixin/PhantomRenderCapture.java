package com.tamedphantoms.mod.mixin;

import net.minecraft.world.entity.monster.Phantom;

/** Сущность, которую прямо сейчас разбирает клиентский рендерер. */
public final class PhantomRenderCapture {
    public static Phantom entity;

    private PhantomRenderCapture() {
    }
}
