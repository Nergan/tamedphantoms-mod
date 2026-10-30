package com.tamedphantoms.mod.mixin

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.tamedphantoms.mod.client.render.PhantomRenderCapture
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.world.entity.LivingEntity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

private const val EXTRACT =
    "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;" +
        "Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V"

private const val SUBMIT =
    "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;" +
        "Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"

/**
 * Седок ручного фантома ([com.tamedphantoms.mod.client.PhantomRiderPose]). Поза считается при сборке
 * состояния и хранится при нём. Сдвиг на седло — в начале submit, вместе с ником и тенью.
 * Поворот — сразу после push модели: ник остаётся ровным, а тело крутится с фантомом.
 */
@Mixin(LivingEntityRenderer::class)
abstract class PlayerRideBankMixin {

    @Inject(method = [EXTRACT], at = [At("HEAD")])
    private fun tamedphantomsCapture(
        entity: LivingEntity,
        state: LivingEntityRenderState,
        partialTick: Float,
        ci: CallbackInfo,
    ) {
        PhantomRenderCapture.rememberRider(state, entity, partialTick)
    }

    @Inject(method = [SUBMIT], at = [At("HEAD")])
    private fun tamedphantomsStickToPhantom(
        state: LivingEntityRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        camera: CameraRenderState,
        ci: CallbackInfo,
    ) {
        val pose = PhantomRenderCapture.rider(state) ?: return
        poseStack.translate(pose.dx, pose.dy, pose.dz)
    }

    @Inject(
        method = [SUBMIT],
        at = [At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0, shift = At.Shift.AFTER)],
    )
    private fun tamedphantomsTurnWithPhantom(
        state: LivingEntityRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        camera: CameraRenderState,
        ci: CallbackInfo,
    ) {
        val pose = PhantomRenderCapture.rider(state) ?: return
        if (!pose.turned) return
        poseStack.rotateDegrees(Axis.YP, -pose.yaw)
        poseStack.rotateDegrees(Axis.XP, -pose.pitch)
        poseStack.rotateDegrees(Axis.ZP, pose.bank)
        poseStack.rotateDegrees(Axis.YP, pose.yaw)
    }
}
