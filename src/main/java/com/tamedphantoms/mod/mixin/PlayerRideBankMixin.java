package com.tamedphantoms.mod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tamedphantoms.mod.entity.TamedPhantomEntity;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Крен и тангаж после translate(0, -1.501, 0), вокруг точки опоры модели.
 * Отдельный сдвиг в начале render держит модель на седле, когда фантом
 * набирает скорость: интерполяция игрока отстаёт от интерполяции маунта.
 */
@Mixin(LivingEntityRenderer.class)
public class PlayerRideBankMixin {

    @Inject(
        method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At("HEAD")
    )
    private void tamedphantoms$stickToPhantom(
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
        Vec3 seat = phantomPos.add(player.position().subtract(phantom.position())).add(phantom.seatVisualShift(partialTick));
        poseStack.translate(seat.x - playerPos.x, seat.y - playerPos.y, seat.z - playerPos.z);
    }

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
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-bank));
    }
}
