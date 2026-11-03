package com.tamedphantoms.mod.util

import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

/** Глаза седока на спине фантома, см. [PhantomRiderSight]. */
object PhantomRiderEye {

    /** Глаза в мире. null — корпус не наклоняет седока. */
    fun of(player: Player, partial: Float, eyeHeight: Double = player.eyeHeight.toDouble()): Vec3? {
        val vehicle = player.vehicle ?: return null
        val attitude = (vehicle as? PhantomCockpit)?.cockpitAttitude(partial) ?: return null
        val tilt = PhantomRiderSight.worldTilt(attitude.yaw, PhantomRiderSight.bodyTilt(attitude.pitch, attitude.bank))
        val offset = tilt.transform(player.x - vehicle.x, player.y - vehicle.y + eyeHeight, player.z - vehicle.z)
        val p = partial.toDouble()
        return Vec3(
            Mth.lerp(p, vehicle.xo, vehicle.x) + offset[0],
            Mth.lerp(p, vehicle.yo, vehicle.y) + offset[1],
            Mth.lerp(p, vehicle.zo, vehicle.z) + offset[2],
        )
    }
}
