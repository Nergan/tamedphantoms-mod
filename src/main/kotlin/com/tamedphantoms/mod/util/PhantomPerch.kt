package com.tamedphantoms.mod.util

import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

/** Точка на твёрдой поверхности рядом, куда фантом может сесть. */
object PhantomPerch {

    fun spot(
        level: Level,
        random: RandomSource,
        x: Double,
        z: Double,
        y: Double,
        maxAbove: Double,
        minRadius: Double,
        maxRadius: Double,
    ): Vec3? {
        val angle = random.nextDouble() * Mth.TWO_PI
        val radius = minRadius + random.nextDouble() * (maxRadius - minRadius).coerceAtLeast(0.0)
        val bx = Mth.floor(x + cos(angle) * radius)
        val bz = Mth.floor(z + sin(angle) * radius)
        val surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz)
        if (surface <= level.minBuildHeight + 1) return null
        if (y - surface > maxAbove) return null
        val groundPos = BlockPos(bx, surface - 1, bz)
        val ground = level.getBlockState(groundPos)
        if (!ground.fluidState.isEmpty || ground.getCollisionShape(level, groundPos).isEmpty) return null
        if (PhantomFlightAvoidance.isHazard(ground)) return null
        val above = level.getBlockState(BlockPos(bx, surface, bz))
        if (!above.fluidState.isEmpty || PhantomFlightAvoidance.isHazard(above)) return null
        return Vec3(bx + 0.5, surface.toDouble(), bz + 0.5)
    }
}
