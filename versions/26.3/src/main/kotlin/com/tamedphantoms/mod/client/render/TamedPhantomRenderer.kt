package com.tamedphantoms.mod.client.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.tamedphantoms.mod.client.texture.PhantomTextureProcessor
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.client.model.geom.ModelLayers
import net.minecraft.core.BlockPos
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.PhantomRenderer
import net.minecraft.client.renderer.entity.state.PhantomRenderState
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import net.minecraft.world.entity.monster.Phantom

class TamedPhantomRenderer(context: EntityRendererProvider.Context) : PhantomRenderer(context) {

    init {
        this.model = TamedPhantomModel(context.bakeLayer(ModelLayers.PHANTOM))
        this.addLayer(TamedPhantomEyesLayer(this))
        this.addLayer(TamedPhantomSaddleLayer(this))
    }

    override fun createRenderState(): PhantomRenderState = TamedPhantomRenderState()

    override fun extractRenderState(entity: Phantom, state: PhantomRenderState, partialTicks: Float) {
        super.extractRenderState(entity, state, partialTicks)
        if (state is TamedPhantomRenderState) {
            state.phantom = entity
            state.partialTick = partialTicks
        }
    }

    override fun getTextureLocation(state: PhantomRenderState): Identifier {
        val pet = (state as? TamedPhantomRenderState)?.phantom as? TamedPhantomEntity
        if (pet != null) {
            return PhantomTextureProcessor.bodyTexture(pet.bodyDye)
        }
        return super.getTextureLocation(state)
    }

    /** Фантом со светящимися чернилами рисуется в полную яркость, как светящийся спрут. */
    override fun getBlockLightLevel(entity: Phantom, pos: BlockPos): Int {
        if (entity is TamedPhantomEntity && entity.luminous) return 15
        return super.getBlockLightLevel(entity, pos)
    }

    override fun setupRotations(state: PhantomRenderState, poseStack: PoseStack, bodyRot: Float, entityScale: Float) {
        super.setupRotations(state, poseStack, bodyRot, entityScale)
        val pet = (state as? TamedPhantomRenderState)?.phantom as? TamedPhantomEntity ?: return
        if (pet.isOrderedToSit()) return
        val partialTick = (state as TamedPhantomRenderState).partialTick
        val shown = Mth.lerp(partialTick, pet.xRotO, pet.xRot)
        val crawl = pet.crawlVisual(partialTick)
        if (pet.isVehicle || crawl > 0f) {
            val extraPitch = shown * (1f - crawl) - pet.xRot
            if (extraPitch != 0f) {
                poseStack.rotateDegrees(Axis.XP, extraPitch)
            }
        }
        val acro = pet.acroPitchVisual(partialTick)
        if (acro != 0f) {
            poseStack.rotateDegrees(Axis.XP, acro)
        }
        poseStack.rotateDegrees(Axis.ZP, -pet.bankVisual(partialTick))
    }

    override fun scale(state: PhantomRenderState, poseStack: PoseStack) {
        super.scale(state, poseStack)
        val pet = (state as? TamedPhantomRenderState)?.phantom as? TamedPhantomEntity ?: return
        if (pet.isOrderedToSit()) {
            poseStack.rotateDegrees(Axis.ZP, 180.0f)
            poseStack.translate(0.0f, 3.002f, 0.0f)
        }
    }
}
