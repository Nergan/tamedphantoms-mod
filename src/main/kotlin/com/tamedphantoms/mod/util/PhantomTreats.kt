package com.tamedphantoms.mod.util

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.event.ModAdvancements
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.WeakHashMap

/** Еда и обычные зелья. Взрывные зелья сюда не попадают. */
object PhantomTreats {

    private val fedAt = WeakHashMap<TamedPhantomEntity, Int>()

    fun tryFood(player: Player, phantom: TamedPhantomEntity, stack: ItemStack): Boolean {
        if (!phantom.tamed) return false
        if (PhantomHeldLook.isRefusal(stack) && !phantom.isOwnedBy(player)) return false
        if (stack.get(DataComponents.FOOD) == null && stack.get(DataComponents.SUSPICIOUS_STEW_EFFECTS) == null) return false
        val level = phantom.level() as? ServerLevel ?: return false
        if (!once(phantom)) return false
        val food = stack.get(DataComponents.FOOD)
        PhantomTreatApply.applyFood(level, phantom, stack)
        if (food != null) phantom.healWithFood(food.nutrition())
        if (!player.abilities.instabuild) stack.shrink(1)
        return true
    }

    fun tryPotion(player: Player, phantom: TamedPhantomEntity, stack: ItemStack): Boolean {
        if (!stack.`is`(Items.POTION) || !phantom.tamed) return false
        val level = phantom.level() as? ServerLevel ?: return false
        if (!once(phantom)) return false
        val contents = stack.get(DataComponents.POTION_CONTENTS)
        var aggro = false
        var nastyHeal = 0f
        var flipped = false
        var inverted = false
        if (contents != null) {
            for (effect in contents.getAllEffects()) {
                when (effect.effect) {
                    MobEffects.HEAL -> aggro = true
                    MobEffects.HARM -> nastyHeal += 6f * (1 shl effect.amplifier.coerceAtLeast(0))
                    MobEffects.WEAKNESS -> if (phantom.isOwnedBy(player)) {
                        flipped = true
                        phantom.flipFromWeakness(effect.duration)
                    }
                    MobEffects.NIGHT_VISION -> {
                        inverted = true
                        phantom.addEffect(MobEffectInstance(effect))
                    }
                    else -> phantom.addEffect(MobEffectInstance(effect))
                }
            }
        }
        if (aggro) phantom.scorn(player)
        if (nastyHeal > 0f) phantom.heal(nastyHeal)
        if (player is ServerPlayer) {
            if (aggro) ModAdvancements.grantFilth(player)
            if (nastyHeal > 0f) ModAdvancements.grantNastyHeal(player)
            if (flipped) ModAdvancements.grantEnough(player)
            if (inverted) ModAdvancements.grantOverload(player)
        }
        if (!player.abilities.instabuild) {
            stack.shrink(1)
            val bottle = ItemStack(Items.GLASS_BOTTLE)
            if (!player.addItem(bottle)) player.drop(bottle, false)
        }
        level.playSound(null, phantom.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_DRINK, net.minecraft.sounds.SoundSource.NEUTRAL, 0.6f, 1.0f)
        return true
    }

    private fun once(phantom: TamedPhantomEntity): Boolean {
        if (fedAt[phantom] == phantom.tickCount) return false
        fedAt[phantom] = phantom.tickCount
        return true
    }
}
