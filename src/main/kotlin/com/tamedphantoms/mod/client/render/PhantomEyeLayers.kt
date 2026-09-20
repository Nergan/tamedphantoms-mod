package com.tamedphantoms.mod.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.tamedphantoms.mod.client.texture.PhantomTextureProcessor
import net.minecraft.client.model.PhantomModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.monster.Phantom

/**
 * Зелёные и жёлтые — одно аддитивное свечение.
 * Кровь: тёмный цвет как есть, плюс отдельное тусклое свечение,
 * чтобы [RenderType.eyes] не вымывал её в алый.
 */
object PhantomEyeLayers {
    private const val FULL_BRIGHT_LIGHT = 0xF000F0
    private const val NO_TINT = -1

    fun render(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        model: PhantomModel<Phantom>,
        texture: ResourceLocation,
        blood: Boolean,
    ) {
        if (blood) {
            val solid = buffer.getBuffer(RenderType.entityTranslucentEmissive(texture))
            model.root().render(poseStack, solid, FULL_BRIGHT_LIGHT, OverlayTexture.NO_OVERLAY, NO_TINT)
            val glow = PhantomTextureProcessor.bloodGlowTexture()
            if (glow != null) {
                val glowBuffer = buffer.getBuffer(RenderType.eyes(glow))
                model.root().render(poseStack, glowBuffer, FULL_BRIGHT_LIGHT, OverlayTexture.NO_OVERLAY, NO_TINT)
            }
            return
        }
        val consumer = buffer.getBuffer(RenderType.eyes(texture))
        model.root().render(poseStack, consumer, FULL_BRIGHT_LIGHT, OverlayTexture.NO_OVERLAY, NO_TINT)
    }
}
