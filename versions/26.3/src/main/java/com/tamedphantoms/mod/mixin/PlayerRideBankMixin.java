package com.tamedphantoms.mod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tamedphantoms.mod.entity.TamedPhantomEntity;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class PlayerRideBankMixin {

    @Unique
    private static LivingEntity tamedphantoms$entity;

    @Unique
    private static float tamedphantoms$partial;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("HEAD"))
    private void tamedphantoms$capture(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        tamedphantoms$entity = entity;
        tamedphantoms$partial = partialTick;
    }

    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At("HEAD")
    )
    private void tamedphantoms$bank(
        LivingEntityRenderState state,
        PoseStack poseStack,
        SubmitNodeCollector collector,
        CameraRenderState camera,
        CallbackInfo ci
    ) {
        LivingEntity entity = tamedphantoms$entity;
        float partialTick = tamedphantoms$partial;
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }
        if (!(player.getVehicle() instanceof TamedPhantomEntity phantom) || phantom.isOrderedToSit()) {
            return;
        }
        Vec3 phantomPos = new Vec3(
            Mth.lerp(partialTick, phantom.xo, phantom.getX()),
            Mth.lerp(partialTick, phantom.yo, phantom.getY()),
            Mth.lerp(partialTick, phantom.zo, phantom.getZ())
        );
        Vec3 playerPos = new Vec3(
            Mth.lerp(partialTick, player.xo, player.getX()),
            Mth.lerp(partialTick, player.yo, player.getY()),
            Mth.lerp(partialTick, player.zo, player.getZ())
        );
        Vec3 seat = phantomPos.add(player.position().subtract(phantom.position())).add(phantom.seatVisualShift(player, partialTick));
        poseStack.translate(seat.x - playerPos.x, seat.y - playerPos.y, seat.z - playerPos.z);
        float pitch = phantom.renderedBodyPitch(partialTick);
        float bank = phantom.bankVisual(partialTick);
        if (pitch != 0.0f) {
            poseStack.rotateDegrees(Axis.XP, -pitch);
        }
        if (bank != 0.0f) {
            poseStack.rotateDegrees(Axis.ZP, -bank);
        }
    }
}
