package com.tamedphantoms.mod.entity.ai

import com.tamedphantoms.mod.entity.PhantomManeuverPilot
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.PhantomManeuvers
import com.tamedphantoms.mod.util.PhantomOwnerTeleport
import net.minecraft.world.entity.monster.Monster
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.levelgen.Heightmap
import java.util.EnumSet
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Следование за хозяином: издалека догоняет, рядом кружит, иногда
 * садится, подползает и снова взлетает.
 */
class TamedPhantomFollowOwnerGoal(private val phantom: TamedPhantomEntity) : Goal() {

    private enum class Mode {
        FLY,
        WATCH,
        LAND,
        CRAWL,
        LAUNCH,
    }

    companion object {
        private const val CATCH_UP_DISTANCE = 16.0
        private const val PERSONAL_SPACE = 2.6
        private const val GUARD_RANGE = 12.0
    }

    private var owner: Player? = null
    private var orbitAngle = 0.0
    private var orbitRadius = 2.4
    private var hoverHeight = 1.5
    private var radiusTicks = 0
    private var heightTicks = 0
    private var desiredRadius = 2.4
    private var desiredHeight = 1.5
    private var mode = Mode.FLY
    private var modeTicks = 0
    private var landCooldown = 240
    private var landX = 0.0
    private var landY = 0.0
    private var landZ = 0.0
    private var stuckTicks = 0
    private var stuckX = Double.NaN
    private var stuckZ = Double.NaN

    init {
        flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse(): Boolean {
        if (!phantom.tamed || phantom.isOrderedToSit() || phantom.isVehicle() || phantom.isLeashed) return false
        val owner = findOwner() ?: return false
        if (!owner.isAlive) return false
        this.owner = owner
        return true
    }

    override fun canContinueToUse(): Boolean {
        if (!phantom.tamed || phantom.isOrderedToSit() || phantom.isVehicle() || phantom.isLeashed) return false
        val owner = this.owner ?: return false
        return owner.isAlive
    }

    override fun requiresUpdateEveryTick(): Boolean = true

    override fun start() {
        orbitAngle = phantom.random.nextDouble() * Math.PI * 2.0
        orbitRadius = 2.2
        hoverHeight = 1.4
        desiredRadius = orbitRadius
        desiredHeight = hoverHeight
        radiusTicks = 0
        heightTicks = 0
        mode = Mode.FLY
        modeTicks = 0
    }

    override fun stop() {
        owner = null
        mode = Mode.FLY
    }

    override fun tick() {
        val owner = this.owner ?: return
        if (phantom.level() !== owner.level()) {
            return
        }
        val dist = sqrt(phantom.distanceToSqr(owner))
        if (dist > PhantomOwnerTeleport.DISTANCE) {
            return
        }
        if (phantom.isDefending() || dist > 22.0) {
            mode = Mode.FLY
        }
        if (landCooldown > 0) landCooldown--

        when (mode) {
            Mode.FLY -> tickFly(owner, dist)
            Mode.WATCH -> tickWatch(owner, dist)
            Mode.LAND -> tickLand(owner, dist)
            Mode.CRAWL -> tickCrawl(owner, dist)
            Mode.LAUNCH -> tickLaunch(owner)
        }
    }

    private fun tickFly(owner: Player, dist: Double) {
        val threat = threatTo(owner)
        if (threat != null && !phantom.isDefending()) {
            guard(owner, threat)
            return
        }
        val ownerStill = hypot(owner.deltaMovement.x, owner.deltaMovement.z) < 0.04
        if (ownerStill && dist < 12.0 && phantom.maneuverKind() == null) {
            val world = PhantomSurroundings.sample(phantom)
            val odds = PhantomSurroundings.temperament(phantom).maneuverOdds(world.night, world.fullMoon, world.newMoon) * 2
            if (phantom.random.nextInt(odds) == 0) {
                val kinds = PhantomManeuvers.Kind.entries
                if (PhantomManeuverPilot.tryStart(phantom, kinds[phantom.random.nextInt(kinds.size)])) return
            }
        }
        if (phantom.maneuverKind() != null) return
        if (dist < 11.0 && landCooldown <= 0 && !owner.isInWater && phantom.random.nextInt(280) == 0) {
            val spot = landingSpot(owner)
            if (spot != null) {
                landX = spot[0]
                landY = spot[1]
                landZ = spot[2]
                mode = Mode.LAND
                modeTicks = 90
                return
            }
        }
        val ownerSpeed = hypot(owner.deltaMovement.x, owner.deltaMovement.z)
        if (dist < 14.0 && ownerSpeed < 0.04 && phantom.random.nextInt(180) == 0) {
            mode = Mode.WATCH
            modeTicks = 45 + phantom.random.nextInt(50)
            return
        }
        if (ownerSpeed > 0.07) {
            escort(owner, dist, ownerSpeed)
            return
        }
        flyOrbit(owner, dist)
    }

    private fun tickWatch(owner: Player, dist: Double) {
        modeTicks--
        val dx = phantom.x - owner.x
        val dz = phantom.z - owner.z
        val len = hypot(dx, dz).coerceAtLeast(0.8)
        phantom.moveControl.setWantedPosition(owner.x + dx / len * 7.0, owner.y + 2.2, owner.z + dz / len * 7.0, 0.2)
        phantom.lookControl.setLookAt(owner, 24f, 24f)
        if (modeTicks <= 0 || dist > 18.0) mode = Mode.FLY
    }

    private fun escort(owner: Player, dist: Double, ownerSpeed: Double) {
        val ox = owner.deltaMovement.x / ownerSpeed
        val oz = owner.deltaMovement.z / ownerSpeed
        val side = if (phantom.id and 1 == 0) 1.0 else -1.0
        val targetX = owner.x + ox * 8.0 + -oz * 3.4 * side
        val targetZ = owner.z + oz * 8.0 + ox * 3.4 * side
        val speed = if (dist > 12.0) 0.95 else 0.42
        phantom.moveControl.setWantedPosition(targetX, owner.y + 2.1, targetZ, speed)
    }

    private fun tickLand(owner: Player, dist: Double) {
        modeTicks--
        phantom.moveControl.setWantedPosition(landX, landY + 0.35, landZ, 0.5)
        phantom.lookControl.setLookAt(owner, 12f, 12f)
        val dx = phantom.x - landX
        val dz = phantom.z - landZ
        val arrived = dx * dx + dz * dz < 2.2 && phantom.y < landY + 1.4
        if (modeTicks <= 0 || arrived) {
            mode = Mode.CRAWL
            modeTicks = 50 + phantom.random.nextInt(70)
        }
        if (dist > 20.0) mode = Mode.FLY
    }

    private fun tickCrawl(owner: Player, dist: Double) {
        modeTicks--
        val ground = surfaceY(Mth.floor(phantom.x), Mth.floor(phantom.z)) ?: owner.y
        phantom.moveControl.setWantedPosition(owner.x, ground + 0.25, owner.z, 0.22)
        phantom.lookControl.setLookAt(owner, 16f, 16f)
        if (modeTicks <= 0 || dist < 1.8) {
            mode = Mode.LAUNCH
            modeTicks = 36
            landCooldown = 520 + phantom.random.nextInt(220)
        }
        if (dist > 16.0) {
            mode = Mode.FLY
            landCooldown = 200
        }
    }

    private fun tickLaunch(owner: Player) {
        if (modeTicks == 36) phantom.beginSurfaceTakeoff()
        modeTicks--
        phantom.moveControl.setWantedPosition(owner.x, owner.y + 2.4, owner.z, 0.72)
        if (modeTicks <= 0 || phantom.y > owner.y + 1.3) {
            mode = Mode.FLY
        }
    }

    private fun flyOrbit(owner: Player, dist: Double) {
        if (radiusTicks-- <= 0) {
            radiusTicks = 36 + phantom.random.nextInt(48)
            desiredRadius = if (dist > CATCH_UP_DISTANCE) {
                5.0 + phantom.random.nextDouble() * 2.0
            } else {
                4.5 + phantom.random.nextDouble() * 5.0
            }
        }
        if (heightTicks-- <= 0) {
            heightTicks = 28 + phantom.random.nextInt(40)
            desiredHeight = 0.9 + phantom.random.nextDouble() * 1.8
        }

        val world = PhantomSurroundings.sample(phantom)
        var radiusGoal = desiredRadius
        if (owner.isShiftKeyDown) radiusGoal = radiusGoal.coerceIn(3.2, 4.5)
        if (world.thundering) radiusGoal = radiusGoal.coerceAtMost(5.5)
        if (dist < PERSONAL_SPACE) radiusGoal = radiusGoal.coerceAtLeast(5.0)
        orbitRadius += (radiusGoal - orbitRadius) * 0.06
        hoverHeight += (desiredHeight + sightLift(owner) - hoverHeight) * 0.05

        val catchingUp = dist > CATCH_UP_DISTANCE
        if (courseIsStuck()) {
            orbitAngle += if (phantom.random.nextBoolean()) 2.1 else -2.1
        }
        orbitAngle += if (catchingUp) {
            (phantom.random.nextDouble() - 0.5) * 0.03
        } else {
            0.042 + (phantom.random.nextDouble() - 0.5) * 0.018
        }

        val leadX = owner.deltaMovement.x * 10.0
        val leadZ = owner.deltaMovement.z * 10.0
        val targetX = owner.x + leadX + cos(orbitAngle) * orbitRadius
        val targetZ = owner.z + leadZ + sin(orbitAngle) * orbitRadius
        val targetY = owner.y + hoverHeight

        var speed = when {
            dist > CATCH_UP_DISTANCE -> 1.05
            dist > 4.5 -> 0.40
            else -> 0.26
        }
        if (owner.isShiftKeyDown && !catchingUp) speed *= 0.7
        phantom.moveControl.setWantedPosition(targetX, targetY, targetZ, speed)
        if (!catchingUp) {
            phantom.lookControl.setLookAt(owner, 12.0f, 12.0f)
        }
    }

    /** На уровне глаз прямо перед хозяином фантом закрывал бы обзор и клики: там он поднимается выше. */
    private fun sightLift(owner: Player): Double {
        val toPhantom = phantom.position().subtract(owner.eyePosition)
        val distance = toPhantom.length()
        if (distance < 0.5 || distance > 8.0) return 0.0
        val facing = toPhantom.scale(1.0 / distance).dot(owner.getViewVector(1f))
        return if (facing > 0.8) 2.2 else 0.0
    }

    private var guardTarget: Monster? = null
    private var guardScan = 0

    /** Ближайший монстр, который сейчас охотится на хозяина. */
    private fun threatTo(owner: Player): Monster? {
        if (guardScan-- > 0) {
            return guardTarget?.takeIf { it.isAlive && it.target === owner && it.distanceToSqr(owner) < GUARD_RANGE * GUARD_RANGE }
        }
        guardScan = 10
        guardTarget = phantom.level().getEntitiesOfClass(Monster::class.java, owner.boundingBox.inflate(GUARD_RANGE)) {
            it.isAlive && it.target === owner
        }.minByOrNull { it.distanceToSqr(owner) }
        return guardTarget
    }

    /** Встаёт между хозяином и монстром и смотрит на него. Кусает, только если ударят его самого. */
    private fun guard(owner: Player, threat: Monster) {
        val x = owner.x + (threat.x - owner.x) * 0.6
        val z = owner.z + (threat.z - owner.z) * 0.6
        val y = maxOf(owner.y, threat.y) + threat.bbHeight + 0.8
        phantom.moveControl.setWantedPosition(x, y, z, 0.8)
        phantom.lookControl.setLookAt(threat, 30f, 30f)
    }

    private fun courseIsStuck(): Boolean {
        val moved = if (stuckX.isNaN()) 1.0 else hypot(phantom.x - stuckX, phantom.z - stuckZ)
        stuckX = phantom.x
        stuckZ = phantom.z
        if (phantom.horizontalCollision || moved < 0.03) stuckTicks++ else stuckTicks = 0
        if (stuckTicks < 12) return false
        stuckTicks = 0
        return true
    }

    private fun landingSpot(owner: Player): DoubleArray? {
        val level = phantom.level()
        val angle = phantom.random.nextDouble() * Mth.TWO_PI
        val radius = 1.4 + phantom.random.nextDouble() * 2.4
        val x = Mth.floor(owner.x + cos(angle) * radius)
        val z = Mth.floor(owner.z + sin(angle) * radius)
        val surface = surfaceY(x, z) ?: return null
        if (owner.y - surface > 5.5) return null
        if (owner.isInWater) return null
        val groundPos = BlockPos(x, Mth.floor(surface) - 1, z)
        val ground = level.getBlockState(groundPos)
        if (!ground.fluidState.isEmpty || ground.getCollisionShape(level, groundPos).isEmpty) return null
        val above = level.getBlockState(BlockPos(x, Mth.floor(surface), z))
        if (!above.fluidState.isEmpty) return null
        return doubleArrayOf(x + 0.5, surface, z + 0.5)
    }

    private fun surfaceY(x: Int, z: Int): Double? {
        val level = phantom.level()
        val surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)
        if (surface <= level.minBuildHeight + 1) return null
        return surface.toDouble()
    }

    private fun findOwner(): Player? {
        val id = phantom.ownerUUID ?: return null
        val level = phantom.level()
        if (level is ServerLevel) {
            return level.server.playerList.getPlayer(id)
        }
        return level.players().firstOrNull { it.uuid == id }
    }
}
