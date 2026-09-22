package com.tamedphantoms.mod.client.render

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.PhantomHeadLook
import com.tamedphantoms.mod.util.PhantomHover
import com.tamedphantoms.mod.util.PhantomRideTilt
import net.minecraft.client.model.PhantomModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.util.Mth
import net.minecraft.world.entity.monster.Phantom

/**
 * Ванильная сетка фантома плюс поза головы, сложенные крылья
 * и виляние хвостом, когда фантом лежит перевёрнутым.
 */
class TamedPhantomModel(root: ModelPart) : PhantomModel<Phantom>(root) {

    private val body: ModelPart = root.getChild("body")
    private val head: ModelPart = body.getChild("head")
    private val tailBase: ModelPart = body.getChild("tail_base")
    private val tailTip: ModelPart = tailBase.getChild("tail_tip")
    private val leftWingBase: ModelPart = body.getChild("left_wing_base")
    private val leftWingTip: ModelPart = leftWingBase.getChild("left_wing_tip")
    private val rightWingBase: ModelPart = body.getChild("right_wing_base")
    private val rightWingTip: ModelPart = rightWingBase.getChild("right_wing_tip")

    override fun setupAnim(
        entity: Phantom,
        limbSwing: Float,
        limbSwingAmount: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float,
    ) {
        val pet = entity as? TamedPhantomEntity
        val upsideDown = pet?.isOrderedToSit() == true
        if (upsideDown) {
            foldWings()
            wagTail(ageInTicks)
        } else {
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch)
            tailBase.yRot = 0f
            tailTip.yRot = 0f
            if (pet != null) {
                poseWings(pet, ageInTicks)
            }
        }
        if (pet != null) {
            poseHead(pet, ageInTicks, netHeadYaw, headPitch, upsideDown)
        }
    }

    private fun poseWings(pet: TamedPhantomEntity, ageInTicks: Float) {
        val phase = pet.wingPhase
        if (phase.isNaN()) return
        val partial = (ageInTicks - pet.tickCount).coerceIn(0f, 1f)
        val time = phase + partial * PhantomHover.flapRate(PhantomRideTilt.blend(pet, partial))
        val angle = (pet.getUniqueFlapTickOffset() + time) * WING_SPEED_DEG * Mth.DEG_TO_RAD
        val roll = Mth.cos(angle) * WING_AMPLITUDE_DEG * Mth.DEG_TO_RAD
        leftWingBase.zRot = roll
        leftWingTip.zRot = roll
        rightWingBase.zRot = -roll
        rightWingTip.zRot = -roll
    }

    private fun foldWings() {
        leftWingBase.xRot = 0f
        leftWingBase.yRot = 0f
        leftWingBase.zRot = WING_REST
        leftWingTip.xRot = 0f
        leftWingTip.yRot = 0f
        leftWingTip.zRot = WING_REST
        rightWingBase.xRot = 0f
        rightWingBase.yRot = 0f
        rightWingBase.zRot = -WING_REST
        rightWingTip.xRot = 0f
        rightWingTip.yRot = 0f
        rightWingTip.zRot = -WING_REST
    }

    private fun wagTail(ageInTicks: Float) {
        val wag = Mth.cos(ageInTicks * TAIL_WAG_SPEED) * TAIL_WAG_AMPLITUDE
        tailBase.xRot = 0f
        tailBase.zRot = 0f
        tailBase.yRot = wag
        tailTip.xRot = 0f
        tailTip.zRot = 0f
        tailTip.yRot = wag * TAIL_TIP_EXTRA
    }

    private fun poseHead(
        pet: TamedPhantomEntity,
        ageInTicks: Float,
        netHeadYaw: Float,
        bodyPitch: Float,
        upsideDown: Boolean,
    ) {
        if (pet.isVehicle && pet.controllingPassenger != null) {
            head.yRot = 0f
            head.xRot = HEAD_REST_PITCH
            head.zRot = 0f
            return
        }
        val partial = (ageInTicks - pet.tickCount).coerceIn(0f, 1f)
        val lookPitch = Mth.lerp(partial, pet.clientHeadPitchO, pet.clientHeadPitch)
        val relativePitch = lookPitch - bodyPitch
        head.yRot = PhantomHeadLook.modelYawDegrees(netHeadYaw, upsideDown) * Mth.DEG_TO_RAD
        head.xRot = HEAD_REST_PITCH + PhantomHeadLook.modelPitchDegrees(relativePitch, upsideDown) * Mth.DEG_TO_RAD
        head.zRot = 0f
    }

    companion object {
        /** Поза головы в ванильной сетке, радианы. */
        private const val HEAD_REST_PITCH = 0.2f

        /** Лёгкий угол крыльев в покое, как в PartPose ванильной модели. */
        private const val WING_REST = 0.1f

        /** Ванильные множители взмаха: градусы фазы за тик и амплитуда в градусах. */
        private const val WING_SPEED_DEG = 7.448451f
        private const val WING_AMPLITUDE_DEG = 16f

        private const val TAIL_WAG_SPEED = 0.14f
        private const val TAIL_WAG_AMPLITUDE = 0.28f
        private const val TAIL_TIP_EXTRA = 0.45f
    }
}
