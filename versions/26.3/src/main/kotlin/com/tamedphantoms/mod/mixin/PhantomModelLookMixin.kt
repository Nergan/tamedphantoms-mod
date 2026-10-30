package com.tamedphantoms.mod.mixin

import com.tamedphantoms.mod.client.render.PhantomRenderCapture
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.PhantomHeadLook
import com.tamedphantoms.mod.util.PhantomHeldLook
import com.tamedphantoms.mod.util.PhantomTailBend
import net.minecraft.client.model.monster.phantom.PhantomModel
import net.minecraft.client.renderer.entity.PhantomRenderer
import net.minecraft.client.renderer.entity.state.PhantomRenderState
import net.minecraft.util.Mth
import net.minecraft.world.entity.monster.Phantom
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(PhantomRenderer::class)
abstract class PhantomRenderCaptureMixin {
    @Inject(method = ["extractRenderState"], at = [At("HEAD")])
    private fun tamedphantomsCapture(entity: Phantom, state: PhantomRenderState, partialTicks: Float, ci: CallbackInfo) {
        PhantomRenderCapture.rememberPhantom(state, entity)
    }
}

/** Дикий фантом поворачивает голову к игроку с предметом приручения, хвост изгибается в повороте. */
@Mixin(PhantomModel::class)
abstract class PhantomModelLookMixin {

    @Inject(method = ["setupAnim"], at = [At("RETURN")])
    private fun tamedphantomsLookAtTameItem(state: PhantomRenderState, ci: CallbackInfo) {
        val entity = PhantomRenderCapture.phantom(state) ?: return
        if (entity is TamedPhantomEntity) return
        val body = ((this as Any) as PhantomModel).root().getChild("body")
        val head = body.getChild("head")
        val tailBase = body.getChild("tail_base")
        val tailTip = tailBase.getChild("tail_tip")
        val bend = PhantomTailBend.visual(entity, state.ageInTicks - entity.tickCount)
        tailBase.yRot = bend
        tailTip.yRot = bend * PhantomTailBend.TIP_FOLLOW

        head.yRot = 0f
        head.zRot = 0f
        head.xRot = PhantomHeadLook.REST_PITCH
        val player = PhantomHeldLook.nearestHoldingTameItem(entity) ?: return
        val bodyYaw = entity.yBodyRot
        val lookYaw = PhantomHeadLook.yawDegrees(entity.x, entity.z, player.x, player.z)
        val lookPitch = PhantomHeadLook.pitchDegrees(entity.x, entity.eyeY, entity.z, player.x, player.eyeY, player.z)
        val yaw = PhantomHeadLook.clampRelative(bodyYaw, lookYaw, PhantomHeadLook.MAX_YAW_DEGREES)
        val pitch = PhantomHeadLook.clampRelative(state.xRot, lookPitch, PhantomHeadLook.MAX_PITCH_DEGREES)
        head.yRot = PhantomHeadLook.modelYawDegrees(yaw - bodyYaw, false) * Mth.DEG_TO_RAD
        head.xRot = PhantomHeadLook.headPitchRadians(pitch - state.xRot, false, true)
    }
}
