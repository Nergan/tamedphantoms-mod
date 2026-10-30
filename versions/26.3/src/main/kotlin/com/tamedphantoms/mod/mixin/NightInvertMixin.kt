package com.tamedphantoms.mod.mixin

import com.tamedphantoms.mod.client.PhantomNightInvert
import net.minecraft.client.DeltaTracker
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.resources.Identifier
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/** Ванильный post-эффект invert. checkEntityPostEffect не зовётся каждый кадр, update — зовётся. */
@Mixin(GameRenderer::class)
abstract class NightInvertMixin {
    private var tamedphantomsOurs = false

    @Shadow
    abstract fun setSpectatedEntityPostEffect(id: Identifier)

    @Shadow
    abstract fun clearSpectatedEntityPostEffect()

    @Inject(method = ["update"], at = [At("HEAD")])
    private fun tamedphantomsNightInvert(tracker: DeltaTracker, ci: CallbackInfo) {
        if (PhantomNightInvert.active()) {
            setSpectatedEntityPostEffect(Identifier.withDefaultNamespace("invert"))
            tamedphantomsOurs = true
        } else if (tamedphantomsOurs) {
            clearSpectatedEntityPostEffect()
            tamedphantomsOurs = false
        }
    }
}
