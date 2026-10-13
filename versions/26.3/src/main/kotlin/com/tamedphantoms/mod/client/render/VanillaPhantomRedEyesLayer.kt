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

class VanillaPhantomRedEyesLayer(
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
        val entity = (state as? TamedPhantomRenderState)?.phantom
        if (entity is TamedPhantomEntity) return
        val texture = PhantomTextureProcessor.eyesTexture(tamed = false, defending = true)
        collector.order(1).submitModel(parentModel, state, poseStack, RenderTypes.eyes(texture), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1)
    }

    private companion object {
        const val FULL_BRIGHT = 0xF000F0
    }
}
