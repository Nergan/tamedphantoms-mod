package com.tamedphantoms.mod.loader.fabric.mixin;

import com.tamedphantoms.mod.client.ClientPhantomSoundHandler;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public abstract class PhantomSoundMixin {

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void tamedphantoms$play(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> callback) {
        SoundInstance next = ClientPhantomSoundHandler.INSTANCE.adjust(sound);
        if (next == null) {
            callback.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }

    @Inject(method = "playDelayed", at = @At("HEAD"), cancellable = true)
    private void tamedphantoms$playDelayed(SoundInstance sound, int delay, CallbackInfo callback) {
        SoundInstance next = ClientPhantomSoundHandler.INSTANCE.adjust(sound);
        if (next == null) {
            callback.cancel();
        }
    }
}
