package com.tamedphantoms.mod.util

import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth

/**
 * Шлейф за фантомом, пока держится бонус скорости за бочку.
 * В воздухе это порыв бриза, под водой пузырьки, в лаве огоньки.
 * Чем выше бонус, тем плотнее шлейф. Сложенные крылья собирают его к корпусу.
 */
object PhantomRollTrail {

    fun spawn(phantom: TamedPhantomEntity, boost: Float, tuck: Float) {
        if (boost <= 0.04f || phantom.isRemoved) return
        val level = phantom.level() as? ServerLevel ?: return
        val limit = ServerConfig.rollBoostLimit().toFloat().coerceAtLeast(0.25f)
        val share = (boost / limit).coerceIn(0f, 1f)
        val gate = when {
            share >= 0.8f -> 1
            share >= 0.4f -> 2
            else -> 3
        }
        if (phantom.tickCount % gate != 0) return
        val particle = particleFor(phantom)
        val folded = tuck.coerceIn(0f, 1f)
        val wing = 0.28 + 1.05 * (1.0 - folded)
        val yaw = phantom.yRot * Mth.DEG_TO_RAD
        val pitch = phantom.xRot * Mth.DEG_TO_RAD
        val fx = -Mth.sin(yaw).toDouble()
        val fz = Mth.cos(yaw).toDouble()
        val cp = Mth.cos(pitch).toDouble()
        val sp = Mth.sin(pitch).toDouble()
        val backX = -fx * cp
        val backY = -sp
        val backZ = -fz * cp
        val sideX = -fz
        val sideZ = fx
        val count = if (share >= 0.85f) 2 else 1
        val bodyY = phantom.y + phantom.bbHeight * 0.42
        repeat(count) { index ->
            val along = 0.55 + index * 0.45
            val side = (phantom.random.nextDouble() - 0.5) * 2.0 * wing
            val lift = (phantom.random.nextDouble() - 0.5) * (0.22 + 0.35 * (1.0 - folded))
            level.sendParticles(
                particle,
                phantom.x + backX * along + sideX * side,
                bodyY + backY * along + lift,
                phantom.z + backZ * along + sideZ * side,
                1,
                0.02,
                0.02,
                0.02,
                if (particle == ParticleTypes.GUST) 0.0 else 0.01,
            )
        }
        if (share > 0.9f && phantom.tickCount % 10 == 0 && particle == ParticleTypes.GUST) {
            level.sendParticles(
                ParticleTypes.GUST_EMITTER_SMALL,
                phantom.x + backX * 0.9,
                bodyY + backY * 0.9,
                phantom.z + backZ * 0.9,
                1,
                0.1,
                0.1,
                0.1,
                0.0,
            )
        }
    }

    private fun particleFor(phantom: TamedPhantomEntity): ParticleOptions = when {
        phantom.isInLava -> ParticleTypes.FLAME
        phantom.isInWater || phantom.isUnderWater -> ParticleTypes.BUBBLE
        else -> ParticleTypes.GUST
    }
}
