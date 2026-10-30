package com.tamedphantoms.mod.client.render

import com.tamedphantoms.mod.client.PhantomRiderPose
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.client.renderer.entity.state.PhantomRenderState
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.Phantom
import java.util.WeakHashMap

/**
 * Данные сущности для каждого состояния кадра. В 26.3 все состояния собираются раньше,
 * чем рисуются: одна общая «текущая сущность» к отрисовке указывала бы на последнюю собранную.
 */
object PhantomRenderCapture {
    private val phantoms = WeakHashMap<PhantomRenderState, Phantom>()
    private val riders = WeakHashMap<LivingEntityRenderState, PhantomRiderPose>()

    @JvmStatic
    fun rememberPhantom(state: PhantomRenderState, entity: Phantom) {
        phantoms[state] = entity
    }

    @JvmStatic
    fun phantom(state: PhantomRenderState): Phantom? = phantoms[state]

    @JvmStatic
    fun rememberRider(state: LivingEntityRenderState, entity: LivingEntity, partialTick: Float) {
        val pose = PhantomRiderPose.of(entity, partialTick)
        if (pose != null) {
            riders[state] = pose
        } else if (riders.isNotEmpty()) {
            riders.remove(state)
        }
    }

    @JvmStatic
    fun rider(state: LivingEntityRenderState): PhantomRiderPose? = riders[state]
}
