package com.tamedphantoms.mod.mixin

import com.tamedphantoms.mod.client.PhantomNightInvert
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Entity
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

/** Инверсия цветов ванильным post-эффектом invert, пока на фантоме зелье ночного зрения. */
@Mixin(GameRenderer::class)
abstract class NightInvertMixin {
    private var tamedphantomsOurs = false

    @Shadow
    abstract fun setSpectatedEntityPostEffect(id: Identifier)

    @Shadow
    abstract fun clearSpectatedEntityPostEffect()

    @Inject(method = ["checkEntityPostEffect"], at = [At("RETURN")])
    private fun tamedphantomsNightInvert(entity: Entity, ci: CallbackInfo) {
        val invert = Identifier.withDefaultNamespace("invert")
        if (PhantomNightInvert.active()) {
            setSpectatedEntityPostEffect(invert)
            tamedphantomsOurs = true
        } else if (tamedphantomsOurs) {
            clearSpectatedEntityPostEffect()
            tamedphantomsOurs = false
        }
    }
}
