package com.tamedphantoms.mod.util

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack

/** Эффекты еды на 26.3: их несёт компонент consumable, а не FoodProperties. */
object PhantomTreatApply {

    fun applyFood(level: ServerLevel, phantom: TamedPhantomEntity, stack: ItemStack) {
        val consumable = stack.get(DataComponents.CONSUMABLE)
        if (consumable != null) {
            for (effect in consumable.onConsumeEffects()) {
                effect.apply(level, stack, phantom)
            }
        }
        val stew = stack.get(DataComponents.SUSPICIOUS_STEW_EFFECTS) ?: return
        for (entry in stew.effects()) {
            phantom.addEffect(entry.createEffectInstance())
        }
    }
}
