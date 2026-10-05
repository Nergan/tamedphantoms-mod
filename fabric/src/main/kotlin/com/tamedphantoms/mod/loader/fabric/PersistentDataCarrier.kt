package com.tamedphantoms.mod.loader.fabric

import net.minecraft.nbt.CompoundTag

/** Общий NBT сущности. Реализует mixin, общий код ходит через ModAccess. */
interface PersistentDataCarrier {
    fun tamedphantomsPersistentData(): CompoundTag
}
