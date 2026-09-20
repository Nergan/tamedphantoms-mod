package com.tamedphantoms.mod.client.render

import net.minecraft.client.renderer.RenderType
import net.minecraft.resources.ResourceLocation

/**
 * Зелёные и жёлтые глаза остаются аддитивным свечением [RenderType.eyes].
 * Кровь нельзя рисовать так же: сложение с ванильным жёлтым даёт алый
 * и оранжевый. Кровавые глаза — непрозрачный полный свет, цвет как в текстуре.
 */
object PhantomEyeRenderTypes {
    fun of(texture: ResourceLocation, blood: Boolean): RenderType =
        if (blood) RenderType.entityTranslucentEmissive(texture) else RenderType.eyes(texture)
}
