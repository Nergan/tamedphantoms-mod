package com.tamedphantoms.mod.util

import net.minecraft.core.BlockPos
import net.minecraft.tags.BlockTags
import net.minecraft.tags.FluidTags
import net.minecraft.util.Mth
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.CampfireBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Обход столбов и опасных блоков для диких и свободных фантомов.
 * Верхом движение не меняется: его задаёт седок.
 */
object PhantomFlightAvoidance {

    enum class Probe {
        CLEAR,
        BLOCKED,
        HAZARD,
    }

    data class Axes(val x: Double, val y: Double, val z: Double)

    fun apply(phantom: Phantom) {
        if (!phantom.isAlive || phantom.isVehicle) return
        val motion = phantom.deltaMovement
        val steered = steer(phantom, motion)
        if (!diverged(motion, steered)) return
        phantom.deltaMovement = steered
        face(phantom, steered)
    }

    fun steer(phantom: Phantom, motion: Vec3): Vec3 {
        var adjusted = motion
        if (hazardHere(phantom)) {
            adjusted = Vec3(adjusted.x, 0.24, adjusted.z)
        } else if (hazardUnder(phantom, adjusted)) {
            adjusted = Vec3(adjusted.x, adjusted.y.coerceAtLeast(0.14), adjusted.z)
        }
        val horizontal = hypot(adjusted.x, adjusted.z)
        if (horizontal < 1.0E-4 && adjusted.y <= 0.02) return adjusted
        val forwardX = adjusted.x / horizontal.coerceAtLeast(1.0E-4)
        val forwardZ = adjusted.z / horizontal.coerceAtLeast(1.0E-4)
        val preferLeft = phantom.id and 1 == 0
        return steerMotion(adjusted, preferLeft) { forward, side, up ->
            classify(phantom, forwardX, forwardZ, forward, side, up)
        }
    }

    /**
     * [probe] смотрит вперёд по горизонтальной скорости, в сторону (плюс — налево) и вверх.
     * Если путь чистый, компоненты скорости не меняются.
     */
    fun steerAxes(
        x: Double,
        y: Double,
        z: Double,
        preferLeft: Boolean,
        probe: (forward: Double, side: Double, up: Double) -> Probe,
    ): Axes {
        val horizontal = hypot(x, z)
        val forwardX = x / horizontal.coerceAtLeast(1.0E-4)
        val forwardZ = z / horizontal.coerceAtLeast(1.0E-4)
        val here = probe(0.2, 0.0, 0.0)
        val near = probe(1.15, 0.0, 0.0)
        val far = probe(2.8, 0.0, 0.0)
        if (here != Probe.HAZARD && near == Probe.CLEAR && far == Probe.CLEAR) return Axes(x, y, z)

        val speed = horizontal.coerceAtLeast(0.1)
        val lift = if (here == Probe.HAZARD || near == Probe.HAZARD || far == Probe.HAZARD) 0.16 else y
        val firstSide = if (preferLeft) 1.0 else -1.0
        for (sign in doubleArrayOf(firstSide, -firstSide)) {
            for (distance in doubleArrayOf(1.45, 2.6)) {
                if (
                    probe(1.1, sign * distance * 0.65, 0.1) == Probe.CLEAR &&
                    probe(2.2, sign * distance, 0.15) == Probe.CLEAR
                ) {
                    return Axes(-forwardZ * sign * speed, lift, forwardX * sign * speed)
                }
            }
        }
        if (probe(2.0, 0.0, 1.6) == Probe.CLEAR) {
            return Axes(x, 0.22, z)
        }
        return Axes(-forwardZ * firstSide * speed * 0.45, 0.12, forwardX * firstSide * speed * 0.45)
    }

    fun steerMotion(
        motion: Vec3,
        preferLeft: Boolean,
        probe: (forward: Double, side: Double, up: Double) -> Probe,
    ): Vec3 {
        val steered = steerAxes(motion.x, motion.y, motion.z, preferLeft, probe)
        if (steered.x == motion.x && steered.y == motion.y && steered.z == motion.z) return motion
        return Vec3(steered.x, steered.y, steered.z)
    }

    fun isHazard(state: BlockState): Boolean {
        if (state.fluidState.`is`(FluidTags.LAVA)) return true
        if (state.`is`(BlockTags.FIRE)) return true
        if (state.`is`(BlockTags.CAMPFIRES)) return CampfireBlock.isLitCampfire(state)
        return state.`is`(net.minecraft.world.level.block.Blocks.CACTUS) ||
            state.`is`(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK) ||
            state.`is`(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH) ||
            state.`is`(net.minecraft.world.level.block.Blocks.WITHER_ROSE) ||
            state.`is`(net.minecraft.world.level.block.Blocks.POWDER_SNOW) ||
            state.`is`(net.minecraft.world.level.block.Blocks.COBWEB)
    }

    private fun classify(
        phantom: Phantom,
        forwardX: Double,
        forwardZ: Double,
        forward: Double,
        side: Double,
        up: Double,
    ): Probe {
        val box = chestBox(phantom).move(
            forwardX * forward - forwardZ * side,
            up,
            forwardZ * forward + forwardX * side,
        )
        if (containsHazard(phantom.level(), box)) return Probe.HAZARD
        if (!phantom.level().noCollision(phantom, box)) return Probe.BLOCKED
        return Probe.CLEAR
    }

    private fun chestBox(phantom: Phantom): AABB {
        val box = phantom.boundingBox
        val midY = (box.minY + box.maxY) * 0.5
        val half = ((box.maxX - box.minX) * 0.32).coerceIn(0.28, 0.85)
        return AABB(phantom.x - half, midY - 0.25, phantom.z - half, phantom.x + half, midY + 0.32, phantom.z + half)
    }

    private fun hazardHere(phantom: Phantom): Boolean {
        val box = phantom.boundingBox
        val x = (box.minX + box.maxX) * 0.5
        val z = (box.minZ + box.maxZ) * 0.5
        val sample = AABB(x - 0.35, box.minY, z - 0.35, x + 0.35, box.maxY, z + 0.35)
        return containsHazard(phantom.level(), sample)
    }

    private fun hazardUnder(phantom: Phantom, motion: Vec3): Boolean {
        val box = phantom.boundingBox
        val horizontal = hypot(motion.x, motion.z).coerceAtLeast(1.0E-4)
        val lead = 2.4
        val x = (box.minX + box.maxX) * 0.5 + motion.x / horizontal * lead
        val z = (box.minZ + box.maxZ) * 0.5 + motion.z / horizontal * lead
        val drop = if (motion.y < 0.0) -motion.y * 5.0 else 0.0
        val sample = AABB(x - 0.4, box.minY - 1.15 - drop, z - 0.4, x + 0.4, box.minY + 0.05, z + 0.4)
        return containsHazard(phantom.level(), sample)
    }

    private fun containsHazard(level: Level, box: AABB): Boolean {
        var minX = Mth.floor(box.minX)
        var minY = Mth.floor(box.minY)
        var minZ = Mth.floor(box.minZ)
        var maxX = Mth.floor(box.maxX)
        var maxY = Mth.floor(box.maxY)
        var maxZ = Mth.floor(box.maxZ)
        if (maxX - minX > 4 || maxY - minY > 4 || maxZ - minZ > 4) {
            return isHazard(level.getBlockState(BlockPos.containing(box.center)))
        }
        val cursor = BlockPos.MutableBlockPos()
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                for (z in minZ..maxZ) {
                    if (isHazard(level.getBlockState(cursor.set(x, y, z)))) return true
                }
            }
        }
        return false
    }

    private fun face(phantom: Phantom, steered: Vec3) {
        val horizontal = hypot(steered.x, steered.z)
        if (horizontal < 0.02) return
        val yaw = Math.toDegrees(atan2(steered.z, steered.x)).toFloat() - 90.0f
        phantom.yRot = Mth.approachDegrees(phantom.yRot, yaw, 12.0f)
        phantom.yBodyRot = phantom.yRot
    }

    private fun diverged(original: Vec3, steered: Vec3): Boolean {
        if (original === steered) return false
        val originalHorizontal = hypot(original.x, original.z)
        val steeredHorizontal = hypot(steered.x, steered.z)
        if (originalHorizontal < 1.0E-4 || steeredHorizontal < 1.0E-4) {
            return kotlin.math.abs(steered.y - original.y) > 0.04
        }
        val dot = (original.x * steered.x + original.z * steered.z) / (originalHorizontal * steeredHorizontal)
        return dot < 0.96 || kotlin.math.abs(steered.y - original.y) > 0.04
    }
}
