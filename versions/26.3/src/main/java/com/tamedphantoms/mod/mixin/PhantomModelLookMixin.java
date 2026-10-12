package com.tamedphantoms.mod.mixin;

import com.tamedphantoms.mod.client.render.PhantomRenderCapture;
import com.tamedphantoms.mod.entity.TamedPhantomEntity;
import com.tamedphantoms.mod.util.PhantomHeadLook;
import com.tamedphantoms.mod.util.PhantomHeldLook;
import com.tamedphantoms.mod.util.PhantomTailBend;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.phantom.PhantomModel;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.client.renderer.entity.state.PhantomRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PhantomRenderer.class)
abstract class PhantomRenderCaptureMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void tamedphantoms$capture(Phantom entity, PhantomRenderState state, float partialTicks, CallbackInfo ci) {
        PhantomRenderCapture.entity = entity;
    }
}

@Mixin(PhantomModel.class)
public abstract class PhantomModelLookMixin {

    @Inject(method = "setupAnim", at = @At("RETURN"))
    private void tamedphantoms$lookAtTameItem(PhantomRenderState state, CallbackInfo ci) {
        Phantom entity = PhantomRenderCapture.entity;
        if (entity == null || entity instanceof TamedPhantomEntity) {
            return;
        }
        ModelPart body = ((PhantomModel) (Object) this).root().getChild("body");
        ModelPart head = body.getChild("head");
        ModelPart tailBase = body.getChild("tail_base");
        ModelPart tailTip = tailBase.getChild("tail_tip");
        float ageInTicks = state.ageInTicks;
        float bend = PhantomTailBend.INSTANCE.visual(entity, ageInTicks - entity.tickCount);
        tailBase.yRot = bend;
        tailTip.yRot = bend * PhantomTailBend.TIP_FOLLOW;

        head.yRot = 0.0F;
        head.zRot = 0.0F;
        head.xRot = PhantomHeadLook.REST_PITCH;
        Player player = PhantomHeldLook.INSTANCE.nearestHoldingTameItem(entity);
        if (player == null) {
            return;
        }
        float bodyYaw = entity.yBodyRot;
        float lookYaw = PhantomHeadLook.INSTANCE.yawDegrees(entity.getX(), entity.getZ(), player.getX(), player.getZ());
        float lookPitch = PhantomHeadLook.INSTANCE.pitchDegrees(
            entity.getX(), entity.getEyeY(), entity.getZ(), player.getX(), player.getEyeY(), player.getZ()
        );
        float yaw = PhantomHeadLook.INSTANCE.clampRelative(bodyYaw, lookYaw, PhantomHeadLook.MAX_YAW_DEGREES);
        float pitch = PhantomHeadLook.INSTANCE.clampRelative(state.xRot, lookPitch, PhantomHeadLook.MAX_PITCH_DEGREES);
        head.yRot = PhantomHeadLook.INSTANCE.modelYawDegrees(yaw - bodyYaw, false) * Mth.DEG_TO_RAD;
        head.xRot = PhantomHeadLook.INSTANCE.headPitchRadians(pitch - state.xRot, false, true);
    }
}
