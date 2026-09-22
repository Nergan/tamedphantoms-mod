package com.tamedphantoms.mod.util

import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.FlyingMob
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import kotlin.math.hypot

/** Крик: громкий низкий ambient, свечение, разбег мобов. */
object PhantomScream {

    const val EFFECT_TICKS = 20

    fun isFullMoonNight(level: ServerLevel): Boolean = level.moonPhase == 0 && level.isNight

    fun play(level: ServerLevel, phantom: TamedPhantomEntity) {
        val pos = phantom.blockPosition()
        level.playSound(null, pos, SoundEvents.PHANTOM_AMBIENT, SoundSource.HOSTILE, 14f, 0.46f)
        level.playSound(null, pos, SoundEvents.PHANTOM_AMBIENT, SoundSource.HOSTILE, 11f, 0.34f)
    }

    fun frighten(level: ServerLevel, phantom: TamedPhantomEntity, blindness: Boolean) {
        val radius = ServerConfig.CONFIG.screamRadius.get()
        val reach = radius * radius
        val box = phantom.boundingBox.inflate(radius)
        val mobs = level.getEntitiesOfClass(Mob::class.java, box) { mob ->
            mob !== phantom && mob.isAlive && mob.distanceToSqr(phantom) <= reach
        }
        for (mob in mobs) {
            if (blindness && mob !is Player) {
                blindOnce(mob)
            }
            shoveAway(phantom, mob)
        }
        if (blindness) {
            val riding = phantom.passengers.filterIsInstance<Player>().toSet()
            val players = level.getEntitiesOfClass(Player::class.java, box) { player ->
                player.isAlive && player !in riding && player.distanceToSqr(phantom) <= reach
            }
            for (player in players) {
                blindOnce(player)
            }
        }
    }

    private fun blindOnce(target: LivingEntity) {
        if (target.getEffect(MobEffects.BLINDNESS) != null) return
        target.addEffect(MobEffectInstance(MobEffects.BLINDNESS, EFFECT_TICKS, 0, false, false, false))
    }

    private fun shoveAway(phantom: TamedPhantomEntity, mob: Mob) {
        val dx = mob.x - phantom.x
        val dz = mob.z - phantom.z
        val len = hypot(dx, dz).coerceAtLeast(0.1)
        if (mob is PathfinderMob) {
            mob.navigation.moveTo(mob.x + dx / len * 10.0, mob.y, mob.z + dz / len * 10.0, 1.5)
        }
        if (mob is FlyingMob || mob !is PathfinderMob) {
            mob.deltaMovement = Vec3(dx / len * 0.55, mob.deltaMovement.y, dz / len * 0.55)
        }
    }
}
