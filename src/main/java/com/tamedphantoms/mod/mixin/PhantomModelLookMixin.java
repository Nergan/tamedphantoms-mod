package com.tamedphantoms.mod.mixin;

import com.tamedphantoms.mod.entity.TamedPhantomEntity;
import com.tamedphantoms.mod.util.PhantomHeadLook;
import com.tamedphantoms.mod.util.PhantomHeldLook;
import net.minecraft.client.model.PhantomModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Дикий фантом рисуется ванильной моделью, которая голову не крутит.
 * Пока игрок держит предмет приручения, голова поворачивается к нему.
 */
@Mixin(PhantomModel.class)
public abstract class PhantomModelLookMixin {

    @Inject(method = "setupAnim", at = @At("RETURN"))
    private void tamedphantoms$lookAtTameItem(
        Phantom entity,
        float limbSwing,
        float limbSwingAmount,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        CallbackInfo ci
    ) {
        if (entity instanceof TamedPhantomEntity) {
            return;
        }
        ModelPart head = ((PhantomModel<?>) (Object) this).root().getChild("body").getChild("head");
        head.yRot = 0.0F;
        head.zRot = 0.0F;
        head.xRot = 0.2F;
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
        float pitch = PhantomHeadLook.INSTANCE.clampRelative(headPitch, lookPitch, PhantomHeadLook.MAX_PITCH_DEGREES);
        head.yRot = PhantomHeadLook.INSTANCE.modelYawDegrees(yaw - bodyYaw, false) * Mth.DEG_TO_RAD;
        head.xRot = 0.2F + PhantomHeadLook.INSTANCE.modelPitchDegrees(pitch - headPitch, false) * Mth.DEG_TO_RAD;
    }
}
