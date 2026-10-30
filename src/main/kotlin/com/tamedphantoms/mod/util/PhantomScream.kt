package com.tamedphantoms.mod.util

import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.entity.ModEntities
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.FlyingMob
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.math.hypot

/** Крик: громкий низкий ambient, свечение, разбег мобов. */
object PhantomScream {

    const val EFFECT_TICKS = 20

    /** Громкость слоя, по которой ваниль считает дистанцию слышимости: 16 × громкость. */
    private const val HEAR_VOLUME = 14f

    private class Hit(
        val x: Double,
        val z: Double,
        val distance: Double,
        val radius: Double,
    )

    private val playerHits = HashMap<UUID, MutableList<Hit>>()
    private val mobHits = HashMap<UUID, MutableList<Hit>>()

    fun isFullMoonNight(level: ServerLevel): Boolean = level.moonPhase == 0 && level.isNight

    fun hearDistance(): Double = 16.0 * HEAR_VOLUME

    fun listeners(level: ServerLevel, phantom: TamedPhantomEntity): List<ServerPlayer> {
        val reachSq = hearDistance() * hearDistance()
        return level.players().filter { player ->
            !player.isSpectator && player.distanceToSqr(phantom) <= reachSq
        }
    }

    fun play(level: ServerLevel, phantom: TamedPhantomEntity) {
        val pos = phantom.blockPosition()
        level.playSound(null, pos, SoundEvents.PHANTOM_AMBIENT, SoundSource.HOSTILE, HEAR_VOLUME, 0.46f)
        level.playSound(null, pos, SoundEvents.PHANTOM_AMBIENT, SoundSource.HOSTILE, 11f, 0.34f)
    }

    /**
     * Другие прирученные фантомы этого хозяина, которые слышат крик, кричат сами.
     * Их крик новый круг не поднимает.
     */
    fun echo(level: ServerLevel, source: TamedPhantomEntity, owner: Player) {
        val reach = hearDistance()
        val reachSq = reach * reach
        val others = level.getEntities(ModEntities.TAMED_PHANTOM.get(), source.boundingBox.inflate(reach)) { mate ->
            mate !== source && mate.isAlive && mate.tamed && mate.isOwnedBy(owner) && mate.distanceToSqr(source) <= reachSq
        }
        for (mate in others) {
            mate.answerScream()
        }
    }

    /** Один крик, один раз за жест. Повторные тики жеста сюда не заходят. */
    fun note(level: ServerLevel, phantom: TamedPhantomEntity, blindness: Boolean) {
        if (!blindness || !ServerConfig.stackedScreams()) return
        val radius = ServerConfig.screamRadius()
        val reach = radius * radius
        val box = phantom.boundingBox.inflate(radius)
        val players = level.getEntitiesOfClass(Player::class.java, box) { player ->
            player.isAlive && !immune(level, phantom, player) && player.distanceToSqr(phantom) <= reach
        }
        for (player in players) {
            hear(playerHits, player.uuid, kotlin.math.sqrt(player.distanceToSqr(phantom)), radius, phantom)
        }
        val mobs = level.getEntitiesOfClass(Mob::class.java, box) { mob ->
            mob !== phantom && mob !is TamedPhantomEntity &&
                mob.isAlive && mob.distanceToSqr(phantom) <= reach
        }
        for (mob in mobs) {
            hear(mobHits, mob.uuid, kotlin.math.sqrt(mob.distanceToSqr(phantom)), radius, phantom)
        }
    }

    fun flush(server: MinecraftServer) {
        if (playerHits.isEmpty() && mobHits.isEmpty()) return
        if (!ServerConfig.stackedScreams()) {
            playerHits.clear()
            mobHits.clear()
            return
        }
        for ((id, hits) in playerHits) {
            val player = server.playerList.getPlayer(id) ?: continue
            if (!player.isAlive || hits.isEmpty()) continue
            val ticks = PhantomScreamStack.blindnessTicks(hits.size)
            player.addEffect(MobEffectInstance(MobEffects.BLINDNESS, ticks, 0, false, false, false))
            val closest = hits.maxOf { PhantomScreamStack.closeness(it.distance, it.radius) }
            val amount = PhantomScreamStack.playerDamage(hits.size, closest)
            val world = player.level() as? ServerLevel ?: continue
            strike(world, player, amount.toFloat())
        }
        for (level in server.allLevels) {
            for ((id, hits) in mobHits) {
                val mob = level.getEntity(id) as? Mob ?: continue
                if (!mob.isAlive || mob is TamedPhantomEntity || hits.isEmpty()) continue
                val ticks = PhantomScreamStack.blindnessTicks(hits.size)
                mob.addEffect(MobEffectInstance(MobEffects.BLINDNESS, ticks, 0, false, false, false))
                val from = hits.minBy { it.distance }
                shove(mob, from.x, from.z, PhantomScreamStack.fleeBlocks(hits.size))
                val closest = hits.maxOf { PhantomScreamStack.closeness(it.distance, it.radius) }
                strike(level, mob, PhantomScreamStack.playerDamage(hits.size, closest).toFloat())
            }
        }
        playerHits.clear()
        mobHits.clear()
    }

    fun frighten(level: ServerLevel, phantom: TamedPhantomEntity, blindness: Boolean) {
        if (ServerConfig.stackedScreams()) return
        val radius = ServerConfig.screamRadius()
        val reach = radius * radius
        val box = phantom.boundingBox.inflate(radius)
        val mobs = level.getEntitiesOfClass(Mob::class.java, box) { mob ->
            mob !== phantom && mob !is TamedPhantomEntity && mob.isAlive && mob.distanceToSqr(phantom) <= reach
        }
        for (mob in mobs) {
            if (blindness && mob !is Player) {
                blindOnce(mob)
            }
            shove(mob, phantom.x, phantom.z, 10.0)
        }
        if (blindness) {
            val players = level.getEntitiesOfClass(Player::class.java, box) { player ->
                player.isAlive && !immune(level, phantom, player) && player.distanceToSqr(phantom) <= reach
            }
            for (player in players) {
                blindOnce(player)
            }
        }
    }

    /**
     * Седок этого фантома не слышит его.
     * Хозяин, сидящий хотя бы на одном своём фантоме, и его попутчики не слышат ни одного его фантома.
     */
    private fun immune(level: ServerLevel, phantom: TamedPhantomEntity, player: Player): Boolean {
        if (phantom.passengers.any { it.uuid == player.uuid }) return true
        val ownerId = phantom.ownerUUID ?: return false
        val owner = level.players().firstOrNull { it.uuid == ownerId } ?: return false
        val mount = owner.vehicle as? TamedPhantomEntity ?: return false
        if (!mount.tamed || mount.ownerUUID != ownerId) return false
        return player.uuid == ownerId || mount.passengers.any { it.uuid == player.uuid }
    }

    /** Урон крика не режется бронёй и защитой и не пропадает из-за недавнего удара. */
    private fun strike(level: ServerLevel, living: LivingEntity, amount: Float) {
        if (amount <= 0f) return
        val id = ResourceLocation.fromNamespaceAndPath("tamedphantoms", "scream")
        val key = ResourceKey.create(Registries.DAMAGE_TYPE, id)
        val holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key)
        living.invulnerableTime = 0
        living.hurt(DamageSource(holder), amount)
    }

    private fun hear(into: HashMap<UUID, MutableList<Hit>>, id: UUID, distance: Double, radius: Double, phantom: TamedPhantomEntity) {
        into.getOrPut(id) { ArrayList() }.add(Hit(phantom.x, phantom.z, distance, radius))
    }

    private fun blindOnce(target: LivingEntity) {
        if (target.getEffect(MobEffects.BLINDNESS) != null) return
        target.addEffect(MobEffectInstance(MobEffects.BLINDNESS, EFFECT_TICKS, 0, false, false, false))
    }

    private fun shove(mob: Mob, fromX: Double, fromZ: Double, blocks: Double) {
        val dx = mob.x - fromX
        val dz = mob.z - fromZ
        val len = hypot(dx, dz).coerceAtLeast(0.1)
        if (mob is PathfinderMob) {
            mob.navigation.moveTo(mob.x + dx / len * blocks, mob.y, mob.z + dz / len * blocks, 1.5)
        }
        if (mob is FlyingMob || mob !is PathfinderMob) {
            mob.deltaMovement = Vec3(dx / len * 0.55, mob.deltaMovement.y, dz / len * 0.55)
        }
    }
}
