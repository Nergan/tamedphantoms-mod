package com.tamedphantoms.mod.mixin

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.tamedphantoms.mod.client.PhantomRiderPose
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.world.entity.LivingEntity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

private const val RENDER =
    "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;" +
        "Lnet/minecraft/client/renderer/MultiBufferSource;I)V"

/**
 * Седок ручного фантома ([PhantomRiderPose]). Сдвиг на седло — в начале render, вместе с ником и тенью.
 * Поворот — сразу после push модели: ник остаётся ровным, а тело крутится с фантомом.
 */
@Mixin(LivingEntityRenderer::class)
abstract class PlayerRideBankMixin {

    @Inject(method = [RENDER], at = [At("HEAD")])
    private fun tamedphantomsStickToPhantom(
        entity: LivingEntity,
        entityYaw: Float,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        ci: CallbackInfo,
    ) {
        val pose = PhantomRiderPose.of(entity, partialTick) ?: return
        poseStack.translate(pose.dx, pose.dy, pose.dz)
    }

    @Inject(
        method = [RENDER],
        at = [At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0, shift = At.Shift.AFTER)],
    )
    private fun tamedphantomsTurnWithPhantom(
        entity: LivingEntity,
        entityYaw: Float,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        ci: CallbackInfo,
    ) {
        val pose = PhantomRiderPose.of(entity, partialTick) ?: return
        if (!pose.turned) return
        poseStack.mulPose(Axis.YP.rotationDegrees(-pose.yaw))
        poseStack.mulPose(Axis.XP.rotationDegrees(-pose.pitch))
        poseStack.mulPose(Axis.ZP.rotationDegrees(pose.bank))
        poseStack.mulPose(Axis.YP.rotationDegrees(pose.yaw))
    }
}
