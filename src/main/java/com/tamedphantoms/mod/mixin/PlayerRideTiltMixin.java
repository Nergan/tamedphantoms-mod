package com.tamedphantoms.mod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tamedphantoms.mod.entity.TamedPhantomEntity;
import com.tamedphantoms.mod.util.PhantomRideTilt;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Модель пилота и пассажира наклоняется вместе с фантомом, когда тот
 * задирает нос на зависании. Поворот локальный: после рыскания корпуса,
 * тем же Axis.XP, что и у самого фантома.
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRideTiltMixin {

    @Inject(method = "setupRotations", at = @At("RETURN"))
    private void tamedphantoms$tiltWithPhantom(
        AbstractClientPlayer player,
        PoseStack poseStack,
        float bob,
        float yBodyRot,
        float partialTick,
        float scale,
        CallbackInfo ci
    ) {
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof TamedPhantomEntity phantom)) {
            return;
        }
        float noseUp = PhantomRideTilt.INSTANCE.noseUpDegrees(phantom, partialTick);
        if (noseUp < 0.05F) {
            return;
        }
        poseStack.mulPose(Axis.XP.rotationDegrees(-noseUp));
    }
}
