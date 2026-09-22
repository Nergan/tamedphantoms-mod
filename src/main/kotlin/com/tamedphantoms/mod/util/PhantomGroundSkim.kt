package com.tamedphantoms.mod.util

import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

/** Тело вплотную к земле: крылья в этой позе не машут, а висят. */
object PhantomGroundSkim {

    private const val CLEARANCE = 0.5

    fun pressed(entity: Entity): Boolean {
        if (entity.isInWater) return false
        if (entity.onGround()) return true
        val level = entity.level()
        val feet = entity.boundingBox.minY
        val samples = arrayOf(
            Vec3(entity.x, feet, entity.z),
            Vec3(entity.x + 0.35, feet, entity.z),
            Vec3(entity.x - 0.35, feet, entity.z),
        )
        for (from in samples) {
            val hit = level.clip(
                ClipContext(
                    from,
                    Vec3(from.x, from.y - CLEARANCE, from.z),
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    entity,
                ),
            )
            if (hit.type == HitResult.Type.BLOCK && from.y - hit.location.y <= CLEARANCE) {
                return true
            }
        }
        return false
    }
}
