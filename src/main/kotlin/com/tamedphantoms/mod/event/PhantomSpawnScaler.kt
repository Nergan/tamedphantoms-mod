package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.monster.Phantom
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent

/** Дополнительные ванильные фантомы в естественной группе. Множитель 2 — вдвое больше ванили. */
object PhantomSpawnScaler {

    private val spawningExtra = ThreadLocal.withInitial { false }

    @SubscribeEvent
    fun onFinalizeSpawn(event: FinalizeSpawnEvent) {
        if (spawningExtra.get()) return
        val mob = event.entity
        if (mob !is Phantom || mob is TamedPhantomEntity) return
        if (event.spawnType != MobSpawnType.NATURAL) return
        val level = mob.level()
        if (level !is ServerLevel) return
        val extra = ServerConfig.CONFIG.phantomGroupMultiplier.get() - 1.0
        if (extra <= 0.0) return
        val whole = extra.toInt()
        val count = whole + if (mob.random.nextDouble() < extra - whole) 1 else 0
        if (count <= 0) return
        spawningExtra.set(true)
        try {
            repeat(count) {
                val copy = EntityType.PHANTOM.create(level) ?: return@repeat
                copy.moveTo(mob.x, mob.y, mob.z, mob.yRot, mob.xRot)
                copy.finalizeSpawn(level, event.difficulty, MobSpawnType.NATURAL, event.spawnData)
                level.addFreshEntity(copy)
            }
        } finally {
            spawningExtra.set(false)
        }
    }
}
