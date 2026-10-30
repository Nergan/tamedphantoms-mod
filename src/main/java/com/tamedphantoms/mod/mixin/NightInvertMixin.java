package com.tamedphantoms.mod.mixin;

import com.tamedphantoms.mod.client.PhantomNightInvert;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Инверсия цветов, пока фантом под седоком под зельем ночного зрения. */
@Mixin(GameRenderer.class)
public abstract class NightInvertMixin {

    private static final ResourceLocation TAMEDPHANTOMS_INVERT =
        ResourceLocation.fromNamespaceAndPath("tamedphantoms", "shaders/post/invert.json");

    private boolean tamedphantoms$ours;

    @Shadow
    public abstract void loadEffect(ResourceLocation resourceLocation);

    @Shadow
    public abstract void shutdownEffect();

    @Inject(method = "checkEntityPostEffect", at = @At("RETURN"))
    private void tamedphantoms$nightInvert(Entity entity, CallbackInfo ci) {
        if (PhantomNightInvert.INSTANCE.active()) {
            if (!this.tamedphantoms$ours) {
                this.loadEffect(TAMEDPHANTOMS_INVERT);
                this.tamedphantoms$ours = true;
            }
        } else if (this.tamedphantoms$ours) {
            this.shutdownEffect();
            this.tamedphantoms$ours = false;
        }
    }
}
