package com.tamedphantoms.mod.compat

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.monster.Phantom

object VanillaTypes {
    fun phantom(): EntityType<out Phantom> {
        val type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("phantom"))
        @Suppress("UNCHECKED_CAST")
        return type as EntityType<out Phantom>
    }
}
