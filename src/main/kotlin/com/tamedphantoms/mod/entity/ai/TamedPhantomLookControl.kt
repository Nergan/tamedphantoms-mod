package com.tamedphantoms.mod.entity.ai

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.PhantomHeadLook
import net.minecraft.world.entity.ai.control.LookControl

/**
 * Поворачивает только голову: на игрока или, в самообороне, на цель атаки.
 * Тангаж тела (xRot) — это наклон полёта, ванильный LookControl затирал бы его.
 */
class TamedPhantomLookControl(private val phantom: TamedPhantomEntity) : LookControl(phantom) {

    override fun tick() {
        if (phantom.isVehicle) {
            phantom.yHeadRot = phantom.yBodyRot
            phantom.trackingLook = false
            return
        }
        val target = phantom.glanceTarget
        if (target == null || !target.isAlive) {
            phantom.trackingLook = false
            easeTowardBody()
            return
        }
        phantom.trackingLook = true

        if (phantom.offeringLock) {
            val yaw = PhantomHeadLook.yawDegrees(phantom.x, phantom.z, target.x, target.z)
            phantom.yHeadRot = yaw
            phantom.headLookPitch = PhantomHeadLook.pitchDegrees(
                phantom.x, phantom.eyeY, phantom.z, target.x, target.eyeY, target.z,
            )
            return
        }

        val desiredYaw = PhantomHeadLook.clampRelative(
            phantom.yBodyRot,
            PhantomHeadLook.yawDegrees(phantom.x, phantom.z, target.x, target.z),
            PhantomHeadLook.MAX_YAW_DEGREES,
        )
        val desiredPitch = PhantomHeadLook.clampRelative(
            phantom.xRot,
            PhantomHeadLook.pitchDegrees(phantom.x, phantom.eyeY, phantom.z, target.x, target.eyeY, target.z),
            PhantomHeadLook.MAX_PITCH_DEGREES,
        )
        phantom.yHeadRot = PhantomHeadLook.approachDegrees(
            phantom.yHeadRot,
            desiredYaw,
            PhantomHeadLook.YAW_SPEED_DEGREES,
        )
        phantom.headLookPitch = PhantomHeadLook.approachDegrees(
            phantom.headLookPitch,
            desiredPitch,
            PhantomHeadLook.PITCH_SPEED_DEGREES,
        )
    }

    private fun easeTowardBody() {
        phantom.yHeadRot = PhantomHeadLook.approachDegrees(
            phantom.yHeadRot,
            phantom.yBodyRot,
            PhantomHeadLook.YAW_SPEED_DEGREES,
        )
        phantom.headLookPitch = PhantomHeadLook.approachDegrees(
            phantom.headLookPitch,
            phantom.xRot,
            PhantomHeadLook.PITCH_SPEED_DEGREES,
        )
    }
}
