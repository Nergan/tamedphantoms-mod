package com.tamedphantoms.mod.platform

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.goal.Goal

/**
 * То, что у NeoForge есть в ванильных классах, а у Fabric — нет:
 * постоянные данные сущности и доступ к целям моба из другого пакета.
 * Загрузчик подставляет реализацию при старте.
 */
object ModAccess {

    lateinit var persistentData: (Entity) -> CompoundTag
    lateinit var addGoal: (Mob, Int, Goal) -> Unit

    /** Держит чанк и соседей в радиусе, пока фантом не найдётся. Билет сам истекает. */
    lateinit var keepChunkLoaded: (ServerLevel, BlockPos, Int) -> Unit
}

val Entity.storedData: CompoundTag
    get() = ModAccess.persistentData(this)
