package com.tamedphantoms.mod.loader.fabric.mixin;

import net.minecraft.nbt.CompoundTag;

/** Общий NBT сущности. Реализует mixin, общий код ходит через ModAccess. */
public interface PersistentDataCarrier {

    CompoundTag tamedphantomsPersistentData();
}
