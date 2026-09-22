package com.tamedphantoms.mod.client.render

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.PhantomCrawl
import com.tamedphantoms.mod.util.PhantomFlightAttitude
import com.tamedphantoms.mod.util.PhantomHeadLook
import com.tamedphantoms.mod.util.PhantomShake
import com.tamedphantoms.mod.util.PhantomSitFlutter
import com.tamedphantoms.mod.util.PhantomTailBend
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
            poseSittingWings(ageInTicks)
            wagTail(ageInTicks)
        } else {
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch)
            tailBase.yRot = 0f
            tailTip.yRot = 0f
            if (pet != null) {
                poseWings(pet, ageInTicks)
            }
        }
        bendTail(entity, ageInTicks)
        if (pet != null) {
            poseTailFlight(pet, ageInTicks, upsideDown)
            poseHead(pet, ageInTicks, netHeadYaw, headPitch, upsideDown)
            poseShake(pet, ageInTicks)
        }
    }

    private fun bendTail(entity: Phantom, ageInTicks: Float) {
        val partial = (ageInTicks - entity.tickCount).coerceIn(0f, 1f)
        val bend = PhantomTailBend.visual(entity, partial)
        tailBase.yRot += bend
        tailTip.yRot += bend * PhantomTailBend.TIP_FOLLOW
    }

    private fun poseTailFlight(pet: TamedPhantomEntity, ageInTicks: Float, upsideDown: Boolean) {
        if (upsideDown) return
        val partial = (ageInTicks - pet.tickCount).coerceIn(0f, 1f)
        val swim = Mth.lerp(partial, pet.swimBlendO, pet.swimBlend).coerceIn(0f, 1f)
        val pitch = Mth.lerp(partial, pet.tailPitchO, pet.tailPitch) * (1f - swim) * Mth.DEG_TO_RAD
        tailBase.xRot = tailBase.xRot * (1f - swim) + pitch
        tailTip.xRot = tailTip.xRot * (1f - swim) + pitch
        if (swim <= 0.01f) return
        val wave = ageInTicks * SWIM_WAVE
        tailBase.yRot += Mth.sin(wave) * 0.34f * swim
        tailTip.yRot += Mth.sin(wave - SWIM_LAG) * 0.5f * swim
    }

    private fun poseSittingWings(ageInTicks: Float) {
        foldWings()
        val left = PhantomSitFlutter.lift(ageInTicks, 0)
        val right = PhantomSitFlutter.lift(ageInTicks, 1)
        leftWingBase.zRot = WING_REST + left
        leftWingTip.zRot = WING_REST + left * 1.15f
        rightWingBase.zRot = -WING_REST - right
        rightWingTip.zRot = -WING_REST - right * 1.15f
    }

    private fun poseWings(pet: TamedPhantomEntity, ageInTicks: Float) {
        val phase = pet.wingPhase
        if (phase.isNaN()) return
        val partial = (ageInTicks - pet.tickCount).coerceIn(0f, 1f)
        val rate = Mth.lerp(partial, pet.wingFlapRateO, pet.wingFlapRate)
        val amplitude = Mth.lerp(partial, pet.wingFlapAmpO, pet.wingFlapAmp)
        val time = phase + partial * rate
        val angle = (pet.getUniqueFlapTickOffset() + time) * WING_SPEED_DEG * Mth.DEG_TO_RAD
        val roll = Mth.cos(angle) * WING_AMPLITUDE_DEG * Mth.DEG_TO_RAD * amplitude
        val flightLeft = roll
        val flightRight = -roll
        val crawl = Mth.lerp(partial, pet.wingCrawlO, pet.wingCrawl).coerceIn(0f, 1f)
        val step = Mth.sin(Mth.lerp(partial, pet.crawlPhaseO, pet.crawlPhase)) * PhantomCrawl.STEP
        val droop = PhantomCrawl.DROOP
        leftWingBase.zRot = Mth.lerp(crawl, flightLeft, droop + step)
        leftWingTip.zRot = Mth.lerp(crawl, flightLeft, droop * PhantomCrawl.TIP + step)
        rightWingBase.zRot = Mth.lerp(crawl, flightRight, -droop + step)
        rightWingTip.zRot = Mth.lerp(crawl, flightRight, -droop * PhantomCrawl.TIP + step)
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
            val partial = (ageInTicks - pet.tickCount).coerceIn(0f, 1f)
            val nod = Mth.lerp(partial, pet.clientRideHeadO, pet.clientRideHead)
            val yaw = Mth.lerp(partial, pet.headBankYawO, pet.headBankYaw)
                .coerceIn(-PhantomFlightAttitude.HEAD_YAW, PhantomFlightAttitude.HEAD_YAW)
            head.yRot = yaw * Mth.DEG_TO_RAD
            head.xRot = PhantomHeadLook.REST_PITCH + nod * Mth.DEG_TO_RAD
            head.zRot = 0f
            return
        }
        val partial = (ageInTicks - pet.tickCount).coerceIn(0f, 1f)
        val lookPitch = Mth.lerp(partial, pet.clientHeadPitchO, pet.clientHeadPitch)
        val relativePitch = lookPitch - bodyPitch
        head.yRot = PhantomHeadLook.modelYawDegrees(netHeadYaw, upsideDown) * Mth.DEG_TO_RAD
        head.xRot = PhantomHeadLook.headPitchRadians(relativePitch, upsideDown, pet.trackingLook)
        head.zRot = 0f
    }

    private fun poseShake(pet: TamedPhantomEntity, ageInTicks: Float) {
        val partial = (ageInTicks - pet.tickCount).coerceIn(0f, 1f)
        val envelope = PhantomShake.envelope(pet.shakeTicks, partial)
        if (envelope <= 0.01f) return
        val swing = Mth.sin(ageInTicks * 1.45f) * envelope
        head.yRot += swing * 0.6f
        head.zRot += Mth.cos(ageInTicks * 1.45f) * envelope * 0.18f
        val wag = Mth.sin(ageInTicks * 1.65f) * envelope
        tailBase.yRot += wag * 0.5f
        tailTip.yRot += wag * 0.66f
        tailBase.xRot += wag * 0.12f
        tailTip.xRot += wag * 0.16f
        val wing = Mth.sin(ageInTicks * 1.7f) * envelope * 0.4f
        leftWingBase.zRot += wing
        leftWingTip.zRot += wing
        rightWingBase.zRot -= wing
        rightWingTip.zRot -= wing
    }

    companion object {
        /** Лёгкий угол крыльев в покое, как в PartPose ванильной модели. */
        private const val WING_REST = 0.1f

        /** Ванильные множители взмаха: градусы фазы за тик и амплитуда в градусах. */
        private const val WING_SPEED_DEG = 7.448451f
        private const val WING_AMPLITUDE_DEG = 16f

        private const val TAIL_WAG_SPEED = 0.14f
        private const val TAIL_WAG_AMPLITUDE = 0.28f
        private const val TAIL_TIP_EXTRA = 0.45f
        private const val SWIM_WAVE = 0.15f
        private const val SWIM_LAG = 0.6f
    }
}
