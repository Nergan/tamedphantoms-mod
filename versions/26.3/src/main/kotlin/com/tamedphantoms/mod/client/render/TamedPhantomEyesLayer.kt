package com.tamedphantoms.mod.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.tamedphantoms.mod.client.texture.PhantomTextureProcessor
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.client.model.monster.phantom.PhantomModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.entity.state.PhantomRenderState
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture

class TamedPhantomEyesLayer(
    parent: RenderLayerParent<PhantomRenderState, PhantomModel>,
) : RenderLayer<PhantomRenderState, PhantomModel>(parent) {

    override fun submit(
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        light: Int,
        state: PhantomRenderState,
        yRot: Float,
        xRot: Float,
    ) {
        val pet = (state as? TamedPhantomRenderState)?.phantom as? TamedPhantomEntity ?: return
        val defending = pet.isDefending() || pet.screamEyeTicks > 0
        val texture = PhantomTextureProcessor.eyesTexture(pet.tamed, defending)
        val glow = if (defending) PhantomTextureProcessor.redEyesGlowTexture() else null
        val ordered = collector.order(1)
        if (defending && glow != null) {
            repeat(3) {
                ordered.submitModel(
                    this.parentModel,
                    state,
                    poseStack,
                    RenderTypes.eyes(glow),
                    FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    -1,
                )
            }
        }
        ordered.submitModel(
            this.parentModel,
            state,
            poseStack,
            RenderTypes.eyes(texture),
            FULL_BRIGHT,
            OverlayTexture.NO_OVERLAY,
            -1,
        )
    }

    private companion object {
        const val FULL_BRIGHT = 0xF000F0
    }
}
