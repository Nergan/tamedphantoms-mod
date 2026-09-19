package com.tamedphantoms.mod.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.tamedphantoms.mod.client.texture.PhantomTextureProcessor
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.client.model.PhantomModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.monster.Phantom

/**
 * Светящиеся глаза ручного и освобождённого фантома.
 * Ванильный слой глаз отключён прозрачной текстурой, поэтому здесь
 * используется тот же [RenderType.eyes], что и у диких, во всех цветах.
 */
class TamedPhantomEyesLayer(parent: RenderLayerParent<Phantom, PhantomModel<Phantom>>) :
    RenderLayer<Phantom, PhantomModel<Phantom>>(parent) {

    companion object {
        private const val FULL_BRIGHT_LIGHT = 0xF000F0
        private const val NO_TINT = -1
    }

    override fun render(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        entity: Phantom,
        limbSwing: Float,
        limbSwingAmount: Float,
        partialTicks: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float,
    ) {
        val tamedPhantom = entity as? TamedPhantomEntity ?: return
        val texture = PhantomTextureProcessor.eyesTexture(tamedPhantom.tamed, tamedPhantom.isDefending())
        val consumer = buffer.getBuffer(RenderType.eyes(texture))
        this.parentModel.root().render(poseStack, consumer, FULL_BRIGHT_LIGHT, OverlayTexture.NO_OVERLAY, NO_TINT)
    }
}
