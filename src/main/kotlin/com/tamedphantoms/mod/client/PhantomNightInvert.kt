package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.client.Minecraft
import net.minecraft.world.effect.MobEffects

/** Ночное зрение на самом фантоме переворачивает цвета всадника. Своё ночное зрение седла — нет. */
object PhantomNightInvert {

    fun active(): Boolean {
        val player = Minecraft.getInstance().player ?: return false
        val ride = player.vehicle as? TamedPhantomEntity ?: return false
        return ride.hasEffect(MobEffects.NIGHT_VISION)
    }
}
