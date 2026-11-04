package com.tamedphantoms.mod.entity

import com.tamedphantoms.mod.util.PhantomOwnedList
import net.minecraft.nbt.CompoundTag
import java.util.UUID

/** Id фантома в списке хозяина: строка 26.3 и старый массив int из прежних миров. */
object PhantomIdTag {
    fun read(tag: CompoundTag): UUID? {
        val text = tag.getString("U").orElse("")
        if (text.isNotEmpty()) {
            return try {
                UUID.fromString(text)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
        val ints = tag.getIntArray("U").orElse(null) ?: return null
        return PhantomOwnedList.uuidFromIntArray(ints)
    }

    fun write(tag: CompoundTag, id: UUID) {
        tag.putString("U", id.toString())
    }
}
