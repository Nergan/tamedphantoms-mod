package com.tamedphantoms.mod.entity.ai

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.PhantomTemperament
import net.minecraft.core.BlockPos
import net.minecraft.tags.BiomeTags
import net.minecraft.util.Mth
import net.minecraft.world.level.levelgen.Heightmap
import java.util.WeakHashMap

/**
 * Что фантом знает о месте и времени: ночь, фаза Луны, погода, местность,
 * высота земли под ним. Пересчитывается раз в секунду, цели читают готовое.
 */
object PhantomSurroundings {

    enum class Terrain { OPEN, WATER, HOT, COLD }

    data class Sample(
        val tick: Int,
        val night: Boolean,
        /** 0 — полнолуние, 4 — новолуние. */
        val moonPhase: Int,
        val raining: Boolean,
        val thundering: Boolean,
        val terrain: Terrain,
        /** Верх земли или листвы под фантомом, без воды. */
        val ground: Double,
        val waterSurface: Double?,
    ) {
        val fullMoon: Boolean get() = night && moonPhase == 0
        val newMoon: Boolean get() = night && moonPhase == 4
    }

    private val cache = WeakHashMap<TamedPhantomEntity, Sample>()

    fun sample(phantom: TamedPhantomEntity): Sample {
        val old = cache[phantom]
        if (old != null && phantom.tickCount - old.tick in 0 until 20) return old
        val level = phantom.level()
        val x = Mth.floor(phantom.x)
        val z = Mth.floor(phantom.z)
        val solid = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)
        val canopy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z)
        val floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z)
        val water = if (canopy > floor) canopy.toDouble() else null
        val biome = level.getBiome(BlockPos(x, Mth.floor(phantom.y), z))
        val terrain = when {
            biome.`is`(BiomeTags.IS_OCEAN) || biome.`is`(BiomeTags.IS_RIVER) || water != null -> Terrain.WATER
            biome.value().baseTemperature >= 1.5f -> Terrain.HOT
            biome.value().baseTemperature <= 0.15f -> Terrain.COLD
            else -> Terrain.OPEN
        }
        val sample = Sample(
            tick = phantom.tickCount,
            night = !level.isDay,
            moonPhase = level.moonPhase,
            raining = level.isRaining,
            thundering = level.isThundering,
            terrain = terrain,
            ground = maxOf(solid, canopy, floor).toDouble(),
            waterSurface = water,
        )
        cache[phantom] = sample
        return sample
    }

    fun temperament(phantom: TamedPhantomEntity): PhantomTemperament =
        PhantomTemperament.of(phantom.uuid.mostSignificantBits xor phantom.uuid.leastSignificantBits)
}
