package com.tamedphantoms.mod.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.tamedphantoms.mod.client.texture.PhantomTextureProcessor
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.client.model.monster.phantom.PhantomModel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.entity.state.PhantomRenderState
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.Mth

class TamedPhantomSaddleLayer(
    parent: RenderLayerParent<PhantomRenderState, PhantomModel>,
) : RenderLayer<PhantomRenderState, PhantomModel>(parent) {

    private val saddleRoot: ModelPart = createSaddleModel()

    override fun submit(
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        light: Int,
        state: PhantomRenderState,
        yRot: Float,
        xRot: Float,
    ) {
        val pet = (state as? TamedPhantomRenderState)?.phantom as? TamedPhantomEntity ?: return
        if (!pet.isSaddled) return
        poseStack.pushPose()
        parentModel.root().getChild("body").translateAndRotate(poseStack)
        collector.order(1).submitModelPart(
            saddleRoot,
            poseStack,
            RenderTypes.entityCutout(PhantomTextureProcessor.saddleTexture(pet.saddleDye)),
            light,
            OverlayTexture.NO_OVERLAY,
            null,
        )
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
