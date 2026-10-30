package com.tamedphantoms.mod.entity.ai

import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.entity.PhantomManeuverPilot
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.PhantomManeuvers
import com.tamedphantoms.mod.util.PhantomPerch
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import java.util.EnumSet
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Жизнь освобождённого фантома. Высота и привычки зависят от характера
 * ([PhantomSurroundings.temperament]), времени суток, Луны, погоды и местности:
 * ночью летает выше и чаще крутит фигуры, в полнолуние особенно, днём больше отдыхает,
 * над водой идёт бреющим полётом, в жару парит широкими кругами, в грозу мечется.
 * Держится рядом с другими освобождёнными, прилетает играть с игроками, как дельфины.
 * Если игрок держит предмет приручения — летит к нему.
 */
class TamedPhantomWanderGoal(private val phantom: TamedPhantomEntity) : Goal() {

    private enum class Mode {
        FLY,
        VISIT,
        LAND,
        CRAWL,
        LAUNCH,
    }

    companion object {
        private const val TEMPT_RANGE = 16.0
        private const val FLOCK_RANGE = 24.0
        private const val VISIT_RANGE = 24.0

        /** С этой скорости игрока, блоки за тик, фантом идёт рядом, а не кружит. */
        private const val ESCORT_SPEED = 0.18

        private val PLAY_TRICKS = arrayOf(
            PhantomManeuvers.Kind.ROLL,
            PhantomManeuvers.Kind.ROLL,
            PhantomManeuvers.Kind.LOOP,
            PhantomManeuvers.Kind.WINGOVER,
        )
    }

    private var heading = 0.0
    private var recalcCooldown = 0
    private var mode = Mode.FLY
    private var modeTicks = 0
    private var landCooldown = 180
    private var land = Vec3.ZERO
    private var crawl = Vec3.ZERO
    private var stuckTicks = 0
    private var stuckX = Double.NaN
    private var stuckZ = Double.NaN
    private var visited: Player? = null
    private var visitAngle = 0.0
    private var visitCooldown = 600
    private var playTrick = 0
    private var dash = Vec3.ZERO
    private var dashTicks = 0
    private var soarAngle = 0.0
    private var flockCooldown = 0
    private var flockHeading: Double? = null

    init {
        flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse(): Boolean = !phantom.tamed && !phantom.isVehicle()

    override fun canContinueToUse(): Boolean = !phantom.tamed && !phantom.isVehicle()

    override fun requiresUpdateEveryTick(): Boolean = true

    override fun start() {
        heading = phantom.yRot * (Math.PI / 180.0) + Math.PI / 2.0
        recalcCooldown = 0
        mode = Mode.FLY
    }

    override fun tick() {
        val tempting = findTemptingPlayer()
        if (tempting != null) {
            mode = Mode.FLY
            phantom.endManeuver()
            phantom.moveControl.setWantedPosition(tempting.x, tempting.y + 1.4, tempting.z, 0.85)
            return
        }
        if (phantom.maneuverKind() != null) return
        if (landCooldown > 0) landCooldown--
        if (visitCooldown > 0) visitCooldown--
        if (phantom.isInWater) mode = Mode.FLY

        when (mode) {
            Mode.FLY -> tickFly()
            Mode.VISIT -> tickVisit()
            Mode.LAND -> tickLand()
            Mode.CRAWL -> tickCrawl()
            Mode.LAUNCH -> tickLaunch()
        }
    }

    private fun tickFly() {
        val world = PhantomSurroundings.sample(phantom)
        val temper = PhantomSurroundings.temperament(phantom)
        val random = phantom.random
        if (landCooldown <= 0 && phantom.y > phantom.level().minBuildHeight + 4 &&
            random.nextInt(temper.restOdds(world.night, world.raining)) == 0
        ) {
            val spot = PhantomPerch.spot(phantom.level(), random, phantom.x, phantom.z, phantom.y, 8.0, 1.5, 4.5)
            if (spot != null) {
                land = spot
                mode = Mode.LAND
                modeTicks = 80
                return
            }
        }
        if (visitCooldown <= 0 && random.nextFloat() < 0.006f * (0.4f + temper.curiosity)) {
            val player = phantom.level().getNearestPlayer(phantom, VISIT_RANGE)
            if (player != null && player.isAlive && !player.isSpectator) {
                visited = player
                visitAngle = atan2(phantom.z - player.z, phantom.x - player.x)
                mode = Mode.VISIT
                modeTicks = 220 + random.nextInt(200)
                playTrick = 40 + random.nextInt(60)
                dashTicks = 0
                return
            }
        }
        if (random.nextInt(temper.maneuverOdds(world.night, world.fullMoon, world.newMoon)) == 0) {
            val kinds = PhantomManeuvers.Kind.entries
            if (PhantomManeuverPilot.tryStart(phantom, kinds[random.nextInt(kinds.size)])) return
        }

        if (this.recalcCooldown-- <= 0) {
            this.recalcCooldown = 70 + random.nextInt(70)
            val swing = if (world.thundering) 2.2 else 1.1
            heading += (random.nextDouble() - 0.5) * swing
        }
        if (courseIsStuck()) {
            heading += if (random.nextBoolean()) 2.4 else -2.4
            this.recalcCooldown = 25
        }
        heading += 0.016 + (random.nextDouble() - 0.5) * 0.012
        steerWithFlock()

        val hotDay = world.terrain == PhantomSurroundings.Terrain.HOT && !world.night && !world.raining
        val targetX: Double
        val targetZ: Double
        if (hotDay) {
            // Парение кругами в восходящем потоке над нагретой землёй.
            soarAngle += 0.035
            targetX = phantom.x + cos(soarAngle) * 6.0
            targetZ = phantom.z + sin(soarAngle) * 6.0
        } else {
            soarAngle = heading
            targetX = phantom.x + cos(heading) * 7.0
            targetZ = phantom.z + sin(heading) * 7.0
        }
        val surface = world.waterSurface
        val wantedHeight = when {
            world.terrain == PhantomSurroundings.Terrain.WATER && surface != null && !world.night -> surface + 2.5
            hotDay -> world.ground + temper.cruiseHeight(true, false) + 10.0
            else -> world.ground + temper.cruiseHeight(world.night, world.raining)
        }
        val targetY = phantom.y + ((wantedHeight - phantom.y) * 0.25).coerceIn(-2.5, 2.5)
        val pace = when {
            world.thundering -> 0.7
            hotDay -> 0.36
            world.night -> 0.52
            world.terrain == PhantomSurroundings.Terrain.COLD -> 0.38
            else -> 0.44
        }
        phantom.moveControl.setWantedPosition(targetX, targetY, targetZ, pace)
    }

    /**
     * Играет с игроком, как дельфин с лодкой: за летящим или бегущим держится сбоку и чуть впереди,
     * у стоящего кружит то ближе, то дальше, иногда проносится мимо и крутит фигуры у него на глазах.
     */
    private fun tickVisit() {
        modeTicks--
        val player = visited
        if (player == null || !player.isAlive || player.isSpectator || modeTicks <= 0 ||
            phantom.distanceToSqr(player) > VISIT_RANGE * VISIT_RANGE * 2.0
        ) {
            visited = null
            mode = Mode.FLY
            visitCooldown = 700 + phantom.random.nextInt(900)
            return
        }
        phantom.lookControl.setLookAt(player, 20f, 20f)
        val random = phantom.random
        val temper = PhantomSurroundings.temperament(phantom)
        val moveX = player.x - player.xo
        val moveZ = player.z - player.zo
        val speed = hypot(moveX, moveZ)
        val time = phantom.tickCount.toDouble()
        when {
            speed > ESCORT_SPEED -> {
                val dirX = moveX / speed
                val dirZ = moveZ / speed
                val side = if (phantom.id and 1 == 0) 1.0 else -1.0
                val ahead = 3.0 + speed * 12.0
                phantom.moveControl.setWantedPosition(
                    player.x + dirX * ahead - dirZ * side * 3.5,
                    player.y + 1.5 + sin(time * 0.12) * 1.2,
                    player.z + dirZ * ahead + dirX * side * 3.5,
                    (0.55 + speed).coerceAtMost(1.0),
                )
            }
            dashTicks > 0 -> {
                dashTicks--
                phantom.moveControl.setWantedPosition(dash.x, dash.y, dash.z, 0.75)
            }
            else -> {
                visitAngle += 0.06 + temper.playfulness * 0.04
                val radius = 4.0 + 1.6 * sin(time * 0.07)
                val lift = 2.2 + 1.4 * sin(time * 0.11)
                phantom.moveControl.setWantedPosition(
                    player.x + cos(visitAngle) * radius,
                    player.y + lift,
                    player.z + sin(visitAngle) * radius,
                    0.48,
                )
                if (random.nextInt(140) == 0) {
                    // Пролёт мимо: точка напротив, чуть в стороне от линии через игрока.
                    val across = atan2(phantom.z - player.z, phantom.x - player.x) + Math.PI + 0.35
                    dash = Vec3(player.x + cos(across) * 6.0, player.y + 1.2, player.z + sin(across) * 6.0)
                    dashTicks = 30
                }
            }
        }
        if (--playTrick <= 0) {
            playTrick = 70 + random.nextInt(90)
            PhantomManeuverPilot.tryStart(phantom, PLAY_TRICKS[random.nextInt(PLAY_TRICKS.size)])
        }
    }

    /** Свои рядом: подстраивает курс под соседей, не сбиваясь в кучу. */
    private fun steerWithFlock() {
        if (flockCooldown-- <= 0) {
            flockCooldown = 20
            val near = phantom.level().getEntitiesOfClass(TamedPhantomEntity::class.java, phantom.boundingBox.inflate(FLOCK_RANGE)) {
                it !== phantom && it.isAlive && !it.tamed && !it.isVehicle
            }
            flockHeading = if (near.isEmpty()) {
                null
            } else {
                var vx = 0.0
                var vz = 0.0
                for (other in near) {
                    val motion = other.deltaMovement
                    vx += motion.x
                    vz += motion.z
                    val dx = phantom.x - other.x
                    val dz = phantom.z - other.z
                    val gap = hypot(dx, dz)
                    if (gap in 0.01..4.0) {
                        vx += dx / gap * 0.3
                        vz += dz / gap * 0.3
                    }
                }
                if (hypot(vx, vz) < 1.0E-3) null else atan2(vz, vx)
            }
        }
        val flock = flockHeading ?: return
        val turn = Math.atan2(sin(flock - heading), cos(flock - heading))
        heading += turn * 0.04
    }

    private fun tickLand() {
        modeTicks--
        phantom.moveControl.setWantedPosition(land.x, land.y + 0.35, land.z, 0.45)
        val dx = phantom.x - land.x
        val dz = phantom.z - land.z
        val arrived = dx * dx + dz * dz < 2.4 && phantom.y < land.y + 1.5
        if (modeTicks <= 0 || arrived) {
            val angle = phantom.random.nextDouble() * Math.PI * 2.0
            crawl = Vec3(land.x + cos(angle) * 2.4, land.y, land.z + sin(angle) * 2.4)
            mode = Mode.CRAWL
            modeTicks = 36 + phantom.random.nextInt(40)
        }
    }

    private fun tickCrawl() {
        modeTicks--
        phantom.moveControl.setWantedPosition(crawl.x, land.y + 0.25, crawl.z, 0.2)
        if (modeTicks <= 0) {
            mode = Mode.LAUNCH
            modeTicks = 28
        }
    }

    private fun tickLaunch() {
        if (modeTicks == 28) phantom.beginSurfaceTakeoff()
        modeTicks--
        phantom.moveControl.setWantedPosition(phantom.x, land.y + 4.5, phantom.z, 0.55)
        if (modeTicks <= 0 || phantom.y > land.y + 2.4) {
            mode = Mode.FLY
            landCooldown = 400 + phantom.random.nextInt(280)
        }
    }

    /** Курс не меняется, пока фантом почти стоит: иначе он снова и снова тычется в ту же преграду. */
    private fun courseIsStuck(): Boolean {
        val moved = if (stuckX.isNaN()) 1.0 else hypot(phantom.x - stuckX, phantom.z - stuckZ)
        stuckX = phantom.x
        stuckZ = phantom.z
        if (phantom.horizontalCollision || moved < 0.03) stuckTicks++ else stuckTicks = 0
        if (stuckTicks < 12) return false
        stuckTicks = 0
        return true
    }

    private fun findTemptingPlayer(): Player? {
        val tameItem = ServerConfig.resolveTameItem()
        return phantom.level().getNearestPlayer(phantom, TEMPT_RANGE)?.takeIf { player ->
            player.isAlive && (player.mainHandItem.`is`(tameItem) || player.offhandItem.`is`(tameItem))
        }
    }
}
