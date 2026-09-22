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
        val takeoff = Mth.lerp(partial, pet.wingTakeoffO, pet.wingTakeoffBlend).coerceIn(0f, 1f)
        val swim = Mth.lerp(partial, pet.swimBlendO, pet.swimBlend).coerceIn(0f, 1f)
        val step = Mth.sin(Mth.lerp(partial, pet.crawlPhaseO, pet.crawlPhase))
        val sweep = step * PhantomCrawl.SWEEP
        val tipSweep = step * PhantomCrawl.TIP_SWEEP
        val lift = PhantomCrawl.LIFT

        var leftBaseZ = flightLeft
        var rightBaseZ = flightRight
        var leftTipZ = flightLeft
        var rightTipZ = flightRight
        var leftBaseY = 0f
        var rightBaseY = 0f

        if (takeoff > 0.01f) {
            val strokePhase = angle * 2.5f
            val stroke = Mth.sin(strokePhase)
            val base = stroke * 1.05f
            val tipWorld = Mth.sin(strokePhase - 1.05f) * 1.25f
            val tipLocal = tipWorld - base
            leftBaseZ = Mth.lerp(takeoff, leftBaseZ, base)
            rightBaseZ = Mth.lerp(takeoff, rightBaseZ, -base)
            leftTipZ = Mth.lerp(takeoff, leftTipZ, tipLocal)
            rightTipZ = Mth.lerp(takeoff, rightTipZ, -tipLocal)
        }

        if (swim > 0.01f) {
            val ripple = ageInTicks * 0.09f
            val baseWave = Mth.sin(ripple) * 0.08f
            val tipWave = Mth.sin(ripple - 1.05f) * 0.13f
            leftBaseZ = Mth.lerp(swim, leftBaseZ, baseWave)
            rightBaseZ = Mth.lerp(swim, rightBaseZ, -baseWave)
            leftTipZ = Mth.lerp(swim, leftTipZ, tipWave - baseWave)
            rightTipZ = Mth.lerp(swim, rightTipZ, -(tipWave - baseWave))
        }

        leftWingBase.zRot = Mth.lerp(crawl, leftBaseZ, -lift)
        rightWingBase.zRot = Mth.lerp(crawl, rightBaseZ, lift)
        leftWingTip.zRot = Mth.lerp(crawl, leftTipZ, 0f)
        rightWingTip.zRot = Mth.lerp(crawl, rightTipZ, 0f)
        // Правое крыло смотрит в −X. Тот же yRot уводит его кончик в другую сторону, чем левый.
        leftWingBase.yRot = Mth.lerp(crawl, leftBaseY, sweep)
        rightWingBase.yRot = Mth.lerp(crawl, rightBaseY, sweep)
        leftWingTip.yRot = Mth.lerp(crawl, 0f, tipSweep)
        rightWingTip.yRot = Mth.lerp(crawl, 0f, tipSweep)
        if (crawl > 0f) {
            val flat = 1f - crawl
            leftWingBase.xRot *= flat
            rightWingBase.xRot *= flat
            leftWingTip.xRot *= flat
            rightWingTip.xRot *= flat
        }
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
        val offering = pet.refuseVisual > 0 || pet.nodVisual > 0
        val lookPitch = if (offering) pet.offerAimPitch else Mth.lerp(partial, pet.clientHeadPitchO, pet.clientHeadPitch)
        val relativeYaw = if (offering) {
            val bodyYaw = Mth.rotLerp(partial, pet.yBodyRotO, pet.yBodyRot)
            PhantomHeadLook.wrapDegrees(pet.offerAimYaw - bodyYaw)
                .coerceIn(-PhantomHeadLook.MAX_YAW_DEGREES, PhantomHeadLook.MAX_YAW_DEGREES)
        } else {
            netHeadYaw
        }
        val relativePitch = lookPitch - bodyPitch
        head.yRot = PhantomHeadLook.modelYawDegrees(relativeYaw, upsideDown) * Mth.DEG_TO_RAD
        head.xRot = PhantomHeadLook.headPitchRadians(relativePitch, upsideDown, offering || pet.trackingLook)
        head.zRot = 0f
        if (pet.refuseVisual > 0) {
            head.yRot += Mth.sin(ageInTicks * 0.85f) * 0.42f
        } else if (pet.nodVisual > 0) {
            head.xRot += Mth.sin(ageInTicks * 0.85f) * 0.42f
        }
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
