package com.tamedphantoms.mod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tamedphantoms.mod.entity.TamedPhantomEntity;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Модель игрока кренится вместе с фантомом, только влево-вправо. */
@Mixin(PlayerRenderer.class)
public class PlayerRideBankMixin {

    @Inject(method = "setupRotations", at = @At("RETURN"))
    private void tamedphantoms$bankWithPhantom(
        AbstractClientPlayer player,
        PoseStack poseStack,
        float bob,
        float yBodyRot,
        float partialTick,
        float scale,
        CallbackInfo ci
    ) {
        if (!(player.getVehicle() instanceof TamedPhantomEntity phantom) || phantom.isOrderedToSit()) {
            return;
        }
        float pitch = phantom.ridePitchVisual(partialTick);
        float bank = phantom.bankVisual(partialTick);
        poseStack.translate(0.0, 0.9, 0.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-bank));
        poseStack.translate(0.0, -0.9, 0.0);
    }
}
