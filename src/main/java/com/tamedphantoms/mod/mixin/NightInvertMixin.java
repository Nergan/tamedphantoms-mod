package com.tamedphantoms.mod.mixin;

import com.tamedphantoms.mod.client.PhantomNightInvert;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ванильный invert из бывших Super Secret Settings.
 * checkEntityPostEffect зовётся только при смене камеры, поэтому шейдер включается каждый кадр.
 */
@Mixin(GameRenderer.class)
public abstract class NightInvertMixin {

    private static final ResourceLocation TAMEDPHANTOMS_INVERT =
        ResourceLocation.withDefaultNamespace("shaders/post/invert.json");

    private boolean tamedphantoms$ours;

    @Shadow
    public abstract void loadEffect(ResourceLocation resourceLocation);

    @Shadow
    public abstract void shutdownEffect();

    @Shadow
    public abstract PostChain currentEffect();

    @Inject(method = "render", at = @At("HEAD"))
    private void tamedphantoms$nightInvert(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        if (PhantomNightInvert.INSTANCE.active()) {
            if (!this.tamedphantoms$ours || this.currentEffect() == null) {
                this.loadEffect(TAMEDPHANTOMS_INVERT);
                this.tamedphantoms$ours = true;
            }
        } else if (this.tamedphantoms$ours) {
            this.shutdownEffect();
            this.tamedphantoms$ours = false;
        }
    }
}
