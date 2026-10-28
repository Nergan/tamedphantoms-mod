package com.tamedphantoms.mod.mixin

import com.mojang.blaze3d.audio.SoundBuffer
import com.tamedphantoms.mod.client.VanillaPhantomBuffers
import net.minecraft.client.sounds.AudioStream
import net.minecraft.resources.ResourceLocation
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.util.concurrent.CompletableFuture

@Mixin(net.minecraft.client.sounds.SoundBufferLibrary::class)
abstract class VanillaPhantomBufferMixin {
    @Inject(method = ["getCompleteBuffer"], at = [At("HEAD")], cancellable = true)
    private fun tamedphantomsVanillaBuffer(
        location: ResourceLocation,
        callback: CallbackInfoReturnable<CompletableFuture<SoundBuffer>>,
    ) {
        val future = VanillaPhantomBuffers.completeBuffer(location) ?: return
        callback.returnValue = future
    }

    @Inject(method = ["getStream"], at = [At("HEAD")], cancellable = true)
    private fun tamedphantomsVanillaStream(
        location: ResourceLocation,
        looping: Boolean,
        callback: CallbackInfoReturnable<CompletableFuture<AudioStream>>,
    ) {
        val future = VanillaPhantomBuffers.stream(location, looping) ?: return
        callback.returnValue = future
    }
}
