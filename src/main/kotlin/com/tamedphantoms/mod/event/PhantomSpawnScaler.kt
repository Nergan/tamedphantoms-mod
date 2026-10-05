package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.DifficultyInstance
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.SpawnGroupData
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.level.ServerLevelAccessor

/** Дополнительные ванильные фантомы в естественной группе. Множитель 2 — вдвое больше ванили. */
object PhantomSpawnScaler {

    private val spawningExtra = ThreadLocal.withInitial { false }

    fun onFinalizeSpawn(
        mob: Mob,
        level: ServerLevelAccessor,
        difficulty: DifficultyInstance,
        spawnType: MobSpawnType,
        spawnData: SpawnGroupData?,
    ) {
        if (spawningExtra.get()) return
        if (mob !is Phantom || mob is TamedPhantomEntity) return
        if (spawnType != MobSpawnType.NATURAL) return
        val server = level as? ServerLevel ?: return
        val extra = ServerConfig.phantomGroupMultiplier() - 1.0
        if (extra <= 0.0) return
        val whole = extra.toInt()
        val count = whole + if (mob.random.nextDouble() < extra - whole) 1 else 0
        if (count <= 0) return
        spawningExtra.set(true)
        try {
            repeat(count) {
                val copy = EntityType.PHANTOM.create(server) ?: return@repeat
                copy.moveTo(mob.x, mob.y, mob.z, mob.yRot, mob.xRot)
                copy.finalizeSpawn(level, difficulty, MobSpawnType.NATURAL, spawnData)
                server.addFreshEntity(copy)
            }
        } finally {
            spawningExtra.set(false)
        }
    }
}
