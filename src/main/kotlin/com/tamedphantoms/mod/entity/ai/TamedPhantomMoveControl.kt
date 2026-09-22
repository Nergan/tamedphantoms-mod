package com.tamedphantoms.mod.entity.ai

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.PhantomEffectSpeed
import com.tamedphantoms.mod.util.PhantomFlightPace
import net.minecraft.util.Mth
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.control.MoveControl
import net.minecraft.world.phys.Vec3
import kotlin.math.sqrt

/**
 * Плавное управление полётом ручного/освобождённого фантома.
 *
 * Ванильный `PhantomMoveControl` слушает только внутреннее `moveTargetPoint`
 * и игнорирует [setWantedPosition]. Здесь точка Goal'ов используется напрямую.
 * Скорость набирается плавно, а корпус доворачивается к точке за несколько тиков.
 */
class TamedPhantomMoveControl(mob: Mob) : MoveControl(mob) {

    /** Было 4° и 3.2° за тик — корпус заметно отставал от точки, куда он летит. */
    private companion object {
        const val YAW_STEP = 12f
        const val PITCH_STEP = 8f
    }

    private var currentSpeed = 0.12f

    override fun tick() {
        val phantom = mob as? TamedPhantomEntity
        val bolt = phantom?.takeBoltMotion()
        if (bolt != null) {
            operation = Operation.WAIT
            mob.deltaMovement = bolt
            return
        }
        if (phantom == null || phantom.isOrderedToSit() || phantom.isVehicle) {
            operation = Operation.WAIT
            return
        }

        val dx = wantedX - mob.x
        var dy = wantedY - mob.y
        val dz = wantedZ - mob.z
        if (phantom.takeoffHoldTicks > 0 && dy > 0.0) {
            dy = 0.0
        }
        val horiz = sqrt(dx * dx + dz * dz)
        val dist = sqrt(dx * dx + dy * dy + dz * dz)

        if (operation != Operation.MOVE_TO) {
            mob.deltaMovement = mob.deltaMovement.scale(0.94)
            return
        }

        if (dist < 0.45) {
            operation = Operation.WAIT
            mob.deltaMovement = mob.deltaMovement.scale(0.86)
            return
        }

        if (horiz > 1.0E-4) {
            val targetYaw = (Mth.atan2(dz, dx) * (180.0 / Math.PI)).toFloat() - 90.0f
            mob.yRot = rotlerp(mob.yRot, targetYaw, YAW_STEP)
            mob.yBodyRot = mob.yRot
        }

        val pitch = (-(Mth.atan2(-dy, horiz.coerceAtLeast(1.0E-4)) * (180.0 / Math.PI))).toFloat()
        mob.xRot = rotlerp(mob.xRot, pitch.coerceIn(-32.0f, 32.0f), PITCH_STEP)

        val ease = (dist / 4.0).coerceIn(0.28, 1.0)
        val pace = PhantomFlightPace.pace(phantom)
        val targetSpeed = ((0.16 + speedModifier * 0.52) * ease).toFloat().coerceIn(0.08f, 1.55f) *
            pace * PhantomFlightPace.waterScale(phantom.isUnderWater).toFloat() *
            PhantomEffectSpeed.scale(phantom).toFloat()
        currentSpeed = Mth.approach(currentSpeed, targetSpeed, 0.022f)

        val desired = Vec3(dx / dist * currentSpeed, dy / dist * currentSpeed, dz / dist * currentSpeed)
        mob.deltaMovement = mob.deltaMovement.lerp(desired, 0.12)
    }
}
