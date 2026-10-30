package com.tamedphantoms.mod.util

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/** Эффекты еды на 1.21.1. На 26.3 этот файл заменён своей копией. */
object PhantomTreatApply {

    fun applyFood(level: ServerLevel, phantom: TamedPhantomEntity, stack: ItemStack) {
        val food = stack.get(DataComponents.FOOD)
        if (food != null) {
            for (possible in food.effects()) {
                if (phantom.random.nextFloat() < possible.probability()) {
                    phantom.addEffect(possible.effect())
                }
            }
        }
        stack.get(DataComponents.SUSPICIOUS_STEW_EFFECTS)?.effects()?.forEach { entry ->
            phantom.addEffect(entry.createEffectInstance())
        }
        if (stack.`is`(Items.CHORUS_FRUIT)) {
            chorus(level, phantom)
        }
    }

    private fun chorus(level: ServerLevel, phantom: TamedPhantomEntity) {
        val fromX = phantom.x
        val fromY = phantom.y
        val fromZ = phantom.z
        for (i in 0 until 16) {
            val x = phantom.x + (phantom.random.nextDouble() - 0.5) * 16.0
            val y = Mth.clamp(
                phantom.y + (phantom.random.nextInt(16) - 8),
                level.minBuildHeight.toDouble(),
                (level.minBuildHeight + level.logicalHeight - 1).toDouble(),
            )
            val z = phantom.z + (phantom.random.nextDouble() - 0.5) * 16.0
            if (phantom.isPassenger) phantom.stopRiding()
            if (phantom.randomTeleport(x, y, z, true)) {
                level.playSound(null, fromX, fromY, fromZ, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.NEUTRAL, 1.0f, 1.0f)
                phantom.fallDistance = 0.0f
                return
            }
        }
    }
}
