package com.tamedphantoms.mod.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.tamedphantoms.mod.client.texture.PhantomTextureProcessor
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.client.model.PhantomModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.world.entity.monster.Phantom

/**
 * Светящиеся глаза ручного и освобождённого фантома.
 * Ванильный жёлтый слой выключен.
 */
class TamedPhantomEyesLayer(parent: RenderLayerParent<Phantom, PhantomModel<Phantom>>) :
    RenderLayer<Phantom, PhantomModel<Phantom>>(parent) {

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
        val defending = tamedPhantom.isDefending() || tamedPhantom.screamEyeTicks > 0
        val texture = PhantomTextureProcessor.eyesTexture(tamedPhantom.tamed, defending)
        val glow = if (defending) PhantomTextureProcessor.redEyesGlowTexture() else null
        PhantomEyeLayers.render(poseStack, buffer, this.parentModel, texture, blood = defending, glowTexture = glow)
    }
}
