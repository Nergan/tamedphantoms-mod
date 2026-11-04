package com.tamedphantoms.mod.entity

import net.minecraft.nbt.CompoundTag
import java.util.UUID

/** Id фантома в списке хозяина: и старый массив int, и строка. */
object PhantomIdTag {
    fun read(tag: CompoundTag): UUID? {
        if (tag.hasUUID("U")) return tag.getUUID("U")
        val text = tag.getString("U")
        if (text.isEmpty()) return null
        return try {
            UUID.fromString(text)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun write(tag: CompoundTag, id: UUID) {
        tag.putUUID("U", id)
    }
}
