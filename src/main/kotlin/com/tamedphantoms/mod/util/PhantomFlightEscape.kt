package com.tamedphantoms.mod.util

import net.minecraft.util.Mth
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.phys.Vec3
import java.util.WeakHashMap

/**
 * Короткий прямой отход, когда фантом упёрся.
 * Ванильный контроль иначе прибавляет 180° к курсу каждый тик, пока столкновение не спадёт,
 * и фантом дёргается на одном месте.
 */
object PhantomFlightEscape {
    private const val TICKS = 22
    private val untilTick = WeakHashMap<Phantom, Int>()

    fun active(phantom: Phantom): Boolean {
        val until = untilTick[phantom] ?: return false
        if (phantom.tickCount >= until) {
            untilTick.remove(phantom)
            return false
        }
        return true
    }

    fun begin(phantom: Phantom) {
        val until = phantom.tickCount + TICKS
        val current = untilTick[phantom]
        if (current == null || current < until) {
            untilTick[phantom] = until
        }
    }

    fun turnAway(phantom: Phantom) {
        phantom.yRot = Mth.wrapDegrees(phantom.yRot + 180f)
        phantom.yBodyRot = phantom.yRot
    }

    fun coast(phantom: Phantom) {
        val yawRad = Math.toRadians(phantom.yRot.toDouble())
        val speed = 0.32
        val push = Vec3(-Math.sin(yawRad) * speed, 0.04, Math.cos(yawRad) * speed)
        phantom.deltaMovement = phantom.deltaMovement.lerp(push, 0.45)
        phantom.yBodyRot = phantom.yRot
    }
}
