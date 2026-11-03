package com.tamedphantoms.mod.util

import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.player.Player
import java.util.WeakHashMap

/**
 * Порыв ветра, когда фантом рвётся вверх из петли: вихрь, как от заряда ветра бриза, и его хлопок.
 * Рывок считает клиент пилота, поэтому сервер только показывает эффект и не чаще раза в секунду.
 */
object PhantomZoomBurst {

    private const val COOLDOWN_TICKS = 20L

    private val lastBurst = WeakHashMap<TamedPhantomEntity, Long>()

    fun onRequest(player: Player) {
        val phantom = player.vehicle as? TamedPhantomEntity ?: return
        if (phantom.controllingPassenger !== player || !phantom.isOwnedBy(player)) return
        if (phantom.isOrderedToSit() || ServerConfig.zoomBoost() <= 0.0) return
        val level = phantom.level() as? ServerLevel ?: return
        val now = level.gameTime
        val previous = lastBurst[phantom]
        if (previous != null && now - previous < COOLDOWN_TICKS) return
        lastBurst[phantom] = now
        level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, phantom.x, phantom.y + phantom.bbHeight * 0.5, phantom.z, 1, 0.0, 0.0, 0.0, 0.0)
        level.playSound(
            null,
            phantom.x,
            phantom.y,
            phantom.z,
            SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
            SoundSource.NEUTRAL,
            1.0f,
            0.9f + phantom.random.nextFloat() * 0.2f,
        )
    }
}
