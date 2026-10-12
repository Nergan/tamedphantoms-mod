package com.tamedphantoms.mod.mixin;

import com.tamedphantoms.mod.client.PhantomMoonLight;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** На секунду полнолунного крика туман окрашивается в болезненно-жёлтый. */
@Mixin(FogRenderer.class)
public abstract class MoonFogMixin {

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void tamedphantoms$yellowFog(
        Camera camera,
        int renderDistance,
        DeltaTracker delta,
        float darkenWorldAmount,
        ClientLevel level,
        CallbackInfoReturnable<FogData> callback
    ) {
        FogData data = callback.getReturnValue();
        if (data == null || data.color == null) {
            return;
        }
        PhantomMoonLight.FogTint tint = PhantomMoonLight.INSTANCE.tint(data.color.x, data.color.y, data.color.z);
        if (tint == null) {
            return;
        }
        data.color.set(tint.getRed(), tint.getGreen(), tint.getBlue(), data.color.w);
    }
}
