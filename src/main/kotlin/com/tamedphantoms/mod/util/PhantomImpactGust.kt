package com.tamedphantoms.mod.util

import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.resources.ResourceKey
import java.util.UUID
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Удар фантома на полном бонусе скорости о поверхность сдувает тех, кто стоит
 * на пути ветра. Дальность растёт со скоростью удара, а она упирается в потолок бонуса.
 * Сквозь блоки ветер не проходит, и это именно снос, а не удар.
 */
object PhantomImpactGust {

    private class Carry(
        val dimension: ResourceKey<Level>,
        val x: Double,
        val y: Double,
        val z: Double,
        val peak: Double,
        var ticks: Int,
        val span: Int,
        val stepCap: Double,
    )

    private val carried = HashMap<UUID, Carry>()

    /** Сколько блоков пронесёт ветер на этой скорости удара. Потолок бонуса как раз её и задаёт. */
    fun blowDistance(speed: Double): Double = (speed * 1.15).coerceIn(1.2, 8.0)

    fun tryBlast(phantom: TamedPhantomEntity, impact: Vec3, boost: Float) {
        if (phantom.impactGustCooldown > 0) return
        val level = phantom.level() as? ServerLevel ?: return
        val limit = ServerConfig.rollBoostLimit()
        if (limit <= 0.05) return
        if (boost < limit - 0.15f) return
        val reported = impact.length()
        if (reported < 0.55 || reported > 16.0) return
        val speed = reported.coerceAtMost(0.9 + limit * 1.35)
        val dir = impact.scale(1.0 / reported)
        phantom.impactGustCooldown = 16
        val origin = Vec3(phantom.x, phantom.y + phantom.bbHeight * 0.45, phantom.z)
        val horizontal = hypot(dir.x, dir.z)
        val verticalHit = abs(dir.y) > horizontal
        val radius = (2.6 + speed * 0.35).coerceIn(2.6, 5.2)
        val box = phantom.boundingBox.inflate(radius, 2.4, radius)
        val victims = level.getEntitiesOfClass(LivingEntity::class.java, box) { living ->
            living.isAlive && !living.isRemoved && living !== phantom &&
                !phantom.hasPassenger(living) && living.vehicle == null &&
                (living !is Player || !living.isSpectator)
        }
        var any = false
        for (living in victims) {
            val body = Vec3(living.x, living.y + living.bbHeight * 0.5, living.z)
            val offset = body.subtract(origin)
            val distance = offset.length()
            if (distance < 0.2 || distance > radius) continue
            if (!verticalHit) {
                val ahead = offset.normalize().dot(dir)
                if (ahead < -0.05 && distance > 2.1) continue
            } else if (abs(offset.y) > 2.4) {
                continue
            }
            if (!clearAir(level, origin.subtract(dir.scale(0.35)), body, phantom)) continue
            val wind = windDirection(dir, offset, verticalHit)
            val closeness = (1.0 - distance / radius).coerceIn(0.35, 1.0)
            val carry = blowDistance(speed) * closeness
            startCarry(living, level, wind, carry)
            level.sendParticles(ParticleTypes.GUST, living.x, living.y + living.bbHeight * 0.6, living.z, 3, 0.25, 0.2, 0.25, 0.0)
            level.playSound(
                null,
                living.x,
                living.y,
                living.z,
                SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS,
                0.7f,
                0.9f + living.random.nextFloat() * 0.15f,
            )
            any = true
        }
        if (!any) return
        val gusts = (4 + speed).toInt().coerceIn(4, 9)
        repeat(gusts) { index ->
            val along = 0.4 + index * 0.35
            val side = (index - gusts / 2.0) * 0.18
            level.sendParticles(
                ParticleTypes.GUST,
                origin.x + dir.x * along + dir.z * side,
                origin.y + dir.y * along * 0.3,
                origin.z + dir.z * along - dir.x * side,
                1,
                0.08,
                0.08,
                0.08,
                0.0,
            )
        }
        level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, origin.x, origin.y, origin.z, 1, 0.2, 0.15, 0.2, 0.0)
        val volume = (0.65 + (speed / 8.0) * 0.55).toFloat().coerceAtMost(1.2f)
        level.playSound(
            null,
            phantom.x,
            phantom.y,
            phantom.z,
            SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
            SoundSource.NEUTRAL,
            volume,
            0.95f + phantom.random.nextFloat() * 0.1f,
        )
    }

    fun tick(server: MinecraftServer) {
        if (carried.isEmpty()) return
        val iterator = carried.iterator()
        while (iterator.hasNext()) {
            val (id, carry) = iterator.next()
            val level = server.getLevel(carry.dimension)
            val entity = level?.getEntity(id)
            if (level == null || entity == null || !entity.isAlive || carry.ticks <= 0) {
                iterator.remove()
                continue
            }
            val scale = carry.peak * carry.ticks / carry.span.toDouble()
            val step = scale.coerceAtMost(carry.stepCap)
            val dx = carry.x * step
            val dy = carry.y * step
            val dz = carry.z * step
            if (!entity.isFree(dx, dy, dz)) {
                iterator.remove()
                continue
            }
            entity.deltaMovement = Vec3(
                carry.x * scale,
                entity.deltaMovement.y * 0.82 + carry.y * scale,
                carry.z * scale,
            )
            entity.hurtMarked = true
            entity.hasImpulse = true
            carry.ticks--
            if (carry.ticks <= 0) iterator.remove()
        }
    }

    /**
     * Резкая остановка с любым бонусом. Ветер под крыльями и снос вчетверо сильнее,
     * чем удар о поверхность на полной скорости. Чем выше скорость до остановки, тем сильнее.
     */
    fun trySharpStop(phantom: TamedPhantomEntity, preSpeed: Float, boost: Float) {
        if (phantom.impactGustCooldown > 0) return
        val level = phantom.level() as? ServerLevel ?: return
        if (boost < 0.05f) return
        val speed = preSpeed.toDouble()
        if (speed < 0.55 || speed > 24.0) return
        val limit = ServerConfig.rollBoostLimit().coerceAtLeast(0.0)
        val full = (0.9 + limit * 1.35).coerceAtLeast(1.2)
        val share = (speed / full).coerceIn(0.35, 1.0)
        val distance = blowDistance(full) * 4.0 * share
        phantom.impactGustCooldown = 16
        val yaw = Math.toRadians(phantom.yRot.toDouble())
        val fx = -kotlin.math.sin(yaw)
        val fz = kotlin.math.cos(yaw)
        val dir = Vec3(fx, -0.08, fz).normalize()
        val origin = Vec3(phantom.x, phantom.y + phantom.bbHeight * 0.28, phantom.z)
        val wing = phantom.bbWidth * 0.7
        val rx = fz
        val rz = -fx
        val gusts = (4 + speed * 1.6).toInt().coerceIn(4, 18)
        repeat(gusts) { index ->
            val side = if (index % 2 == 0) 1.0 else -1.0
            val along = (index / 2) * 0.22
            level.sendParticles(
                ParticleTypes.GUST,
                origin.x + fx * along + rx * wing * side,
                origin.y,
                origin.z + fz * along + rz * wing * side,
                1,
                0.06,
                0.02,
                0.06,
                0.0,
            )
        }
        level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, origin.x + rx * wing, origin.y, origin.z + rz * wing, 1, 0.12, 0.04, 0.12, 0.0)
        level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, origin.x - rx * wing, origin.y, origin.z - rz * wing, 1, 0.12, 0.04, 0.12, 0.0)
        val volume = (0.8 + (speed / full).coerceAtMost(1.0) * 0.5).toFloat()
        level.playSound(
            null,
            phantom.x,
            phantom.y,
            phantom.z,
            SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
            SoundSource.NEUTRAL,
            volume,
            0.85f + phantom.random.nextFloat() * 0.1f,
        )
        val radius = (3.4 + speed * 0.55).coerceIn(3.4, 9.0)
        val box = phantom.boundingBox.inflate(radius, 3.2, radius)
        val victims = level.getEntitiesOfClass(LivingEntity::class.java, box) { living ->
            living.isAlive && !living.isRemoved && living !== phantom &&
                !phantom.hasPassenger(living) && living.vehicle == null &&
                (living !is Player || !living.isSpectator)
        }
        for (living in victims) {
            val body = Vec3(living.x, living.y + living.bbHeight * 0.5, living.z)
            val offset = body.subtract(origin)
            val distanceTo = offset.length()
            if (distanceTo < 0.2 || distanceTo > radius) continue
            val ahead = offset.x * fx + offset.z * fz
            val below = origin.y + 0.4 - body.y
            if (ahead < -0.55 && below < 0.2) continue
            if (!clearAir(level, origin, body, phantom)) continue
            val wind = windDirection(dir, offset, verticalHit = false)
            val closeness = (1.0 - distanceTo / radius).coerceIn(0.35, 1.0)
            startCarry(living, level, wind, distance * closeness, strong = true)
            level.sendParticles(ParticleTypes.GUST, living.x, living.y + living.bbHeight * 0.5, living.z, 4, 0.3, 0.15, 0.3, 0.0)
            level.playSound(
                null,
                living.x,
                living.y,
                living.z,
                SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS,
                0.85f,
                0.85f + living.random.nextFloat() * 0.12f,
            )
        }
    }

    private fun startCarry(entity: Entity, level: ServerLevel, wind: Vec3, distance: Double, strong: Boolean = false) {
        val span = (4.0 + distance * 1.5).toInt().coerceIn(4, if (strong) 28 else 16)
        val peak = (distance * 2.0 / (span + 1.0)).coerceIn(0.15, if (strong) 2.2 else 0.9)
        val stepCap = if (strong) 1.6 else 0.7
        carried[entity.uuid] = Carry(level.dimension(), wind.x, wind.y, wind.z, peak, span, span, stepCap)
    }

    private fun windDirection(impact: Vec3, offset: Vec3, verticalHit: Boolean): Vec3 {
        if (verticalHit) {
            var flatX = offset.x
            var flatZ = offset.z
            if (flatX * flatX + flatZ * flatZ < 1.0E-4) {
                flatX = 1.0
                flatZ = 0.0
            }
            val flat = hypot(flatX, flatZ).coerceAtLeast(1.0E-4)
            val y = if (impact.y < 0.0) 0.16 else -0.04
            return Vec3(flatX / flat, y, flatZ / flat).normalize()
        }
        val alongLen = hypot(impact.x, impact.z).coerceAtLeast(1.0E-4)
        val alongX = impact.x / alongLen
        val alongZ = impact.z / alongLen
        val ahead = offset.x * alongX + offset.z * alongZ
        val sideX = offset.x - alongX * ahead
        val sideZ = offset.z - alongZ * ahead
        val sideLen = hypot(sideX, sideZ)
        val outX: Double
        val outZ: Double
        if (sideLen < 0.2) {
            outX = alongZ
            outZ = -alongX
        } else {
            outX = sideX / sideLen
            outZ = sideZ / sideLen
        }
        return Vec3(alongX * 0.9 + outX * 0.28, 0.07, alongZ * 0.9 + outZ * 0.28).normalize()
    }

    private fun clearAir(level: ServerLevel, from: Vec3, to: Vec3, phantom: Entity): Boolean {
        val hit = level.clip(ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, phantom))
        return hit.type == HitResult.Type.MISS || hit.location.distanceToSqr(to) < 0.4
    }
}
