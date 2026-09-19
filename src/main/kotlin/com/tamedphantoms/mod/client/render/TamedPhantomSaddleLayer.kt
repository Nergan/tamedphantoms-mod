package com.tamedphantoms.mod.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.tamedphantoms.mod.client.texture.PhantomTextureProcessor
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.client.model.PhantomModel
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.Mth
import net.minecraft.world.entity.monster.Phantom

/**
 * Плоский намёк на седло вдоль спины: не объёмная модель, а метка,
 * что фантом осёдлан. +Y уводит слой под модель, поэтому сиденье в −Y.
 */
class TamedPhantomSaddleLayer(parent: RenderLayerParent<Phantom, PhantomModel<Phantom>>) :
    RenderLayer<Phantom, PhantomModel<Phantom>>(parent) {

    companion object {
        private const val NO_TINT = -1
    }

    private val saddleRoot: ModelPart = createSaddleModel()

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
        if (entity !is TamedPhantomEntity || !entity.isSaddled) return

        poseStack.pushPose()
        parentModel.root().getChild("body").translateAndRotate(poseStack)
        val consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(PhantomTextureProcessor.saddleTexture()))
        saddleRoot.render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, NO_TINT)
        poseStack.popPose()
    }

    private fun createSaddleModel(): ModelPart {
        val mesh = MeshDefinition()
        val root = mesh.root
        val saddle = root.addOrReplaceChild(
            "saddle",
            CubeListBuilder.create(),
            PartPose.offsetAndRotation(-0.5f, -2.85f, -3.5f, 0.0f, -Mth.HALF_PI, 0.0f),
        )
        saddle.addOrReplaceChild(
            "seat",
            CubeListBuilder.create().texOffs(0, 0).addBox(-5.6f, -0.7f, -2.0f, 11.2f, 0.7f, 4.0f),
            PartPose.ZERO,
        )
        saddle.addOrReplaceChild(
            "pommel",
            CubeListBuilder.create().texOffs(20, 0).addBox(-5.7f, -1.15f, -1.1f, 1.5f, 0.5f, 2.2f),
            PartPose.ZERO,
        )
        saddle.addOrReplaceChild(
            "cantle",
            CubeListBuilder.create().texOffs(20, 4).addBox(4.3f, -1.1f, -1.15f, 1.3f, 0.45f, 2.3f),
            PartPose.ZERO,
        )
        saddle.addOrReplaceChild(
            "left_flap",
            CubeListBuilder.create().texOffs(0, 9).addBox(-4.0f, 0.0f, 1.9f, 8.0f, 1.0f, 0.5f),
            PartPose.ZERO,
        )
        saddle.addOrReplaceChild(
            "right_flap",
            CubeListBuilder.create().texOffs(12, 9).addBox(-4.0f, 0.0f, -2.4f, 8.0f, 1.0f, 0.5f),
            PartPose.ZERO,
        )
        return LayerDefinition.create(mesh, 32, 16).bakeRoot()
    }
}
