package com.tamedphantoms.mod.client.render

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.PhantomModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.monster.Phantom

/**
 * Все цвета глаз идут через [RenderType.eyes].
 * Кровь — один проход основного оттенка и один более тёмный ореол.
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
        glowTexture: ResourceLocation? = null,
    ) {
        val consumer = buffer.getBuffer(RenderType.eyes(texture))
        model.root().render(poseStack, consumer, FULL_BRIGHT_LIGHT, OverlayTexture.NO_OVERLAY, NO_TINT)
        if (blood && glowTexture != null) {
            val glowConsumer = buffer.getBuffer(RenderType.eyes(glowTexture))
            model.root().render(poseStack, glowConsumer, FULL_BRIGHT_LIGHT, OverlayTexture.NO_OVERLAY, NO_TINT)
        }
    }
}
