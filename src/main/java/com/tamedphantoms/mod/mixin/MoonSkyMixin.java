package com.tamedphantoms.mod.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tamedphantoms.mod.util.MoonSickness;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** На секунду полнолунного крика луна и небо окрашиваются в болезненно-жёлтый. */
@Mixin(LevelRenderer.class)
public abstract class MoonSkyMixin {

    private boolean tamedphantoms$moonTint;

    @Inject(
        method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
        at = @At("HEAD")
    )
    private void tamedphantoms$yellowMoon(
        Matrix4f modelView,
        Matrix4f projection,
        float partialTick,
        Camera camera,
        boolean foggy,
        Runnable fogSetup,
        CallbackInfo ci
    ) {
        float strength = MoonSickness.INSTANCE.strength();
        if (strength <= 0.02f) return;
        this.tamedphantoms$moonTint = true;
        RenderSystem.setShaderColor(1.0f, 1.0f - 0.2f * strength, 1.0f - 0.75f * strength, 1.0f);
    }

    @Inject(
        method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
        at = @At("TAIL")
    )
    private void tamedphantoms$clearMoon(
        Matrix4f modelView,
        Matrix4f projection,
        float partialTick,
        Camera camera,
        boolean foggy,
        Runnable fogSetup,
        CallbackInfo ci
    ) {
        if (!this.tamedphantoms$moonTint) return;
        this.tamedphantoms$moonTint = false;
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
