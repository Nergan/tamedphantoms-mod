package com.tamedphantoms.mod.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.tamedphantoms.mod.client.texture.PhantomTextureProcessor
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.PhantomRenderer
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

    init {
        // Своя модель на ванильной сетке: взгляд головы, сложенные крылья и хвост.
        // Слои читают модель через getModel(), поэтому подмена видна и глазам, и седлу.
        this.model = TamedPhantomModel(context.bakeLayer(ModelLayers.PHANTOM))
        // Ванильный PhantomRenderer уже добавил свой собственный слой глаз
        // (обычных, зелёно-жёлтых) в своём конструкторе, который только что
        // отработал (super(context) выше). Безопасного публичного способа
        // убрать уже добавленный слой нет, поэтому добавляем свой
        // перекрашенный слой ПОВЕРХ — рисуется позже, визуально доминирует.
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
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick, scale)
        if (entity !is TamedPhantomEntity || !entity.isVehicle) return
        val shown = Mth.lerp(partialTick, entity.xRotO, entity.xRot)
        poseStack.mulPose(Axis.XP.rotationDegrees(shown - entity.xRot))
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
