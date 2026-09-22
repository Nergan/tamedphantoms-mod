package com.tamedphantoms.mod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tamedphantoms.mod.entity.TamedPhantomEntity;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Крен и тангаж после translate(0, -1.501, 0).
 * До этого сдвиг модели попадал в повёрнутое пространство и уезжал к хвосту.
 */
@Mixin(LivingEntityRenderer.class)
public class PlayerRideBankMixin {

    @Inject(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V",
            ordinal = 1,
            shift = At.Shift.AFTER
        )
    )
    private void tamedphantoms$bankWithPhantom(
        LivingEntity entity,
        float entityYaw,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        CallbackInfo ci
    ) {
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }
        if (!(player.getVehicle() instanceof TamedPhantomEntity phantom) || phantom.isOrderedToSit()) {
            return;
        }
        float pitch = phantom.ridePitchVisual(partialTick);
        float bank = phantom.bankVisual(partialTick);
        if (pitch == 0.0f && bank == 0.0f) {
            return;
        }
        // Бёдра в пространстве модели (Y вниз). Вокруг них крутим, чтобы корпус остался в седле.
        float hip = 0.75f;
        poseStack.translate(0.0, hip, 0.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-bank));
        poseStack.translate(0.0, -hip, 0.0);
    }
}
