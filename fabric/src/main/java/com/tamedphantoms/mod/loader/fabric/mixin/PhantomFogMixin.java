package com.tamedphantoms.mod.loader.fabric.mixin;

import com.tamedphantoms.mod.client.PhantomMoonLight;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class PhantomFogMixin {

    @Shadow private static float fogRed;
    @Shadow private static float fogGreen;
    @Shadow private static float fogBlue;

    @Inject(method = "setupColor", at = @At("RETURN"))
    private static void tamedphantoms$fog(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, CallbackInfo ci) {
        PhantomMoonLight.FogTint tint = PhantomMoonLight.INSTANCE.tint(fogRed, fogGreen, fogBlue);
        if (tint == null) return;
        fogRed = tint.getRed();
        fogGreen = tint.getGreen();
        fogBlue = tint.getBlue();
    }
}
