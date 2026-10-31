package com.tamedphantoms.mod.util

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.event.ModAdvancements
import net.minecraft.core.component.DataComponents
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.WeakHashMap

/** Еда и обычные зелья. Взрывное зелье сюда не попадает: его эффект приходит в [TamedPhantomEntity.addEffect]. */
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
        if (stack.`is`(Items.GLOW_BERRIES)) {
            phantom.addEffect(MobEffectInstance(MobEffects.GLOWING, 8 * 20, 0, false, true, true))
        }
        if (food != null) phantom.healWithFood(food.nutrition())
        consume(player, stack, containerOf(stack))
        return true
    }

    /** Молоко снимает эффекты. Ведро остаётся в руке, фантом кивает, как на еду. */
    fun tryMilk(player: Player, phantom: TamedPhantomEntity, stack: ItemStack): Boolean {
        if (!stack.`is`(Items.MILK_BUCKET) || !phantom.tamed) return false
        val level = phantom.level() as? ServerLevel ?: return false
        if (!once(phantom)) return false
        phantom.removeAllEffects()
        consume(player, stack, Items.BUCKET)
        level.playSound(null, phantom.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_DRINK, net.minecraft.sounds.SoundSource.NEUTRAL, 0.6f, 1.0f)
        return true
    }

    fun tryPotion(player: Player, phantom: TamedPhantomEntity, stack: ItemStack): Boolean {
        if (!stack.`is`(Items.POTION) || !phantom.tamed) return false
        val level = phantom.level() as? ServerLevel ?: return false
        if (!once(phantom)) return false
        val contents = stack.get(DataComponents.POTION_CONTENTS)
        var aggro = false
        var nastyHeal = 0f
        var inverted = false
        if (contents != null) {
            for (effect in contents.getAllEffects()) {
                when (effect.effect) {
                    MobEffects.HEAL -> aggro = true
                    MobEffects.HARM -> nastyHeal += 6f * (1 shl effect.amplifier.coerceAtLeast(0))
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
            if (inverted) ModAdvancements.grantOverload(player)
        }
        consume(player, stack, Items.GLASS_BOTTLE)
        level.playSound(null, phantom.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_DRINK, net.minecraft.sounds.SoundSource.NEUTRAL, 0.6f, 1.0f)
        return true
    }

    /**
     * Фантом и так не горит. Огнестойкость из бутылочки, взрывного или туманного зелья
     * становится сопротивлением урону на то же время и ту же силу.
     */
    fun forPhantom(effect: MobEffectInstance): MobEffectInstance {
        if (effect.effect != MobEffects.FIRE_RESISTANCE) return effect
        return MobEffectInstance(
            MobEffects.DAMAGE_RESISTANCE,
            effect.duration,
            effect.amplifier,
            effect.isAmbient,
            effect.isVisible,
            effect.showIcon(),
        )
    }

    private fun containerOf(stack: ItemStack): Item? = when {
        stack.`is`(Items.HONEY_BOTTLE) -> Items.GLASS_BOTTLE
        stack.`is`(Items.MUSHROOM_STEW) || stack.`is`(Items.RABBIT_STEW) ||
            stack.`is`(Items.BEETROOT_SOUP) || stack.`is`(Items.SUSPICIOUS_STEW) -> Items.BOWL
        else -> null
    }

    /** Пустой остаток — тарелка, бутылочка или ведро — встаёт в ту же руку. */
    private fun consume(player: Player, stack: ItemStack, leftoverItem: Item?) {
        if (player.abilities.instabuild) return
        val hand = if (player.offhandItem === stack) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND
        stack.shrink(1)
        if (leftoverItem == null) return
        val leftover = ItemStack(leftoverItem)
        if (stack.isEmpty) {
            player.setItemInHand(hand, leftover)
        } else if (!player.addItem(leftover)) {
            player.drop(leftover, false)
        }
    }

    private fun once(phantom: TamedPhantomEntity): Boolean {
        if (fedAt[phantom] == phantom.tickCount) return false
        fedAt[phantom] = phantom.tickCount
        return true
    }
}
