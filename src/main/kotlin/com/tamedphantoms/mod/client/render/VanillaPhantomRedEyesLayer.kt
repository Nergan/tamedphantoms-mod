package com.tamedphantoms.mod.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.tamedphantoms.mod.client.texture.PhantomTextureProcessor
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.client.model.PhantomModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.monster.Phantom

/**
 * Кроваво-красные светящиеся глаза обычного ванильного фантома
 * (в мире и в книге Patchouli). Ручная сущность рисует свои глаза сама.
 */
class VanillaPhantomRedEyesLayer(parent: RenderLayerParent<Phantom, PhantomModel<Phantom>>) :
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
        if (entity is TamedPhantomEntity) return
        val texture = PhantomTextureProcessor.eyesTexture(tamed = false, defending = true)
        val consumer = buffer.getBuffer(PhantomEyeRenderTypes.of(texture, blood = true))
        this.parentModel.root().render(poseStack, consumer, FULL_BRIGHT_LIGHT, OverlayTexture.NO_OVERLAY, NO_TINT)
    }
}
