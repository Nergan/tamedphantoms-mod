package com.tamedphantoms.mod.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.tamedphantoms.mod.client.texture.PhantomTextureProcessor
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.client.model.PhantomModel
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.PhantomRenderer
import net.minecraft.client.renderer.entity.layers.PhantomEyesLayer
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.monster.Phantom

/**
 * Рендерер ручного фантома.
 *
 * ВАЖНОЕ АРХИТЕКТУРНОЕ РЕШЕНИЕ (изменено после найденного визуального бага
 * "модель рендерится сильно выше хитбокса"): раньше здесь был собственный
 * `MobRenderer<TamedPhantomEntity, PhantomModel<TamedPhantomEntity>>`,
 * построенный с нуля поверх переиспользованной модели. Причина бага была
 * именно в этом: у ВАНИЛЬНОГО `PhantomRenderer`, судя по всему, есть
 * собственная логика позиционирования/поворота модели (переопределённые
 * scale()/setupRotations() или аналоги), нужная именно для парящего
 * существа с очень тонким (0.5 блока) хитбоксом — а самодельный
 * `MobRenderer`, не наследующий именно от `PhantomRenderer`, этой логики не
 * получает и рендерит модель в "нейтральной" позиции, не подходящей для
 * Phantom.
 *
 * Исправление — наследоваться НАПРЯМУЮ от `net.minecraft.client.renderer.entity.PhantomRenderer`,
 * а не от общего `MobRenderer`. Тогда вся позиционная логика достаётся
 * автоматически, и остаётся переопределить только то, что действительно
 * должно отличаться (текстура, дополнительные слои). Это тот же принцип,
 * что уже применялся к модели ([net.minecraft.client.model.PhantomModel])
 * — просто теперь применён последовательно и к рендереру тоже.
 *
 * [TamedPhantomEntity] — подкласс [Phantom], поэтому `PhantomRenderer`
 * (типизированный на `Phantom`) корректно работает и для нашей сущности по
 * обычным правилам подстановки типов.
 */
class TamedPhantomRenderer(context: EntityRendererProvider.Context) : PhantomRenderer(context) {

    private val ownModel = TamedPhantomModel(PhantomModel.createBodyLayer().bakeRoot())

    init {
        // Сетка собирается кодом ванили, а не из общего слоя: чужой CEM его подменяет.
        this.model = ownModel
        this.layers.removeAll { it is PhantomEyesLayer<*> }
        this.addLayer(TamedPhantomEyesLayer(this))
        this.addLayer(TamedPhantomSaddleLayer(this))
    }

    override fun getTextureLocation(entity: Phantom): ResourceLocation {
        if (entity is TamedPhantomEntity) {
            return PhantomTextureProcessor.bodyTexture()
        }
        return super.getTextureLocation(entity)
    }

    override fun setupRotations(
        entity: Phantom,
        poseStack: PoseStack,
        bob: Float,
        yBodyRot: Float,
        partialTick: Float,
        scale: Float,
    ) {
        if (this.model !== ownModel) this.model = ownModel
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick, scale)
        if (entity !is TamedPhantomEntity || entity.isOrderedToSit()) return
        val shown = Mth.lerp(partialTick, entity.xRotO, entity.xRot)
        val crawl = entity.crawlVisual(partialTick)
        if (entity.isVehicle || crawl > 0f) {
            val target = shown * (1f - crawl)
            val extraPitch = target - entity.xRot
            if (extraPitch != 0f) {
                poseStack.mulPose(Axis.XP.rotationDegrees(extraPitch))
            }
        }
        val acro = entity.acroPitchVisual(partialTick)
        if (acro != 0f) {
            poseStack.mulPose(Axis.XP.rotationDegrees(acro))
        }
        poseStack.mulPose(Axis.ZP.rotationDegrees(-entity.bankVisual(partialTick)))
    }

    override fun scale(livingEntity: Phantom, poseStack: PoseStack, partialTickTime: Float) {
        super.scale(livingEntity, poseStack, partialTickTime)
        if (livingEntity is TamedPhantomEntity && livingEntity.isOrderedToSit()) {
            // LivingEntityRenderer после scale() делает translate(0, -1.501, 0).
            // Поворот на 180° до этого сдвига уводил модель на ~4 блока вниз;
            // 2 * 1.501 компенсирует переворот относительно того же якоря.
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f))
            poseStack.translate(0.0, 3.002, 0.0)
        }
    }
}
