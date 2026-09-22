package com.tamedphantoms.mod.entity.ai

import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.PhantomPerch
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import java.util.EnumSet
import kotlin.math.cos
import kotlin.math.sin

/**
 * Блуждание освобождённого фантома. Иногда садится, немного ползает и снова взлетает.
 * Если игрок держит предмет приручения — летит к нему.
 */
class TamedPhantomWanderGoal(private val phantom: TamedPhantomEntity) : Goal() {

    private enum class Mode {
        FLY,
        LAND,
        CRAWL,
        LAUNCH,
    }

    companion object {
        private const val TEMPT_RANGE = 16.0
    }

    private var heading = 0.0
    private var climb = 0.0
    private var recalcCooldown = 0
    private var mode = Mode.FLY
    private var modeTicks = 0
    private var landCooldown = 180
    private var land = Vec3.ZERO
    private var crawl = Vec3.ZERO

    init {
        flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse(): Boolean = !phantom.tamed && !phantom.isVehicle()

    override fun canContinueToUse(): Boolean = !phantom.tamed && !phantom.isVehicle()

    override fun requiresUpdateEveryTick(): Boolean = true

    override fun start() {
        heading = phantom.yRot * (Math.PI / 180.0)
        climb = 0.0
        recalcCooldown = 0
        mode = Mode.FLY
    }

    override fun tick() {
        val tempting = findTemptingPlayer()
        if (tempting != null) {
            mode = Mode.FLY
            phantom.moveControl.setWantedPosition(tempting.x, tempting.y + 1.4, tempting.z, 0.85)
            return
        }
        if (landCooldown > 0) landCooldown--
        if (phantom.isInWater) mode = Mode.FLY

        when (mode) {
            Mode.FLY -> tickFly()
            Mode.LAND -> tickLand()
            Mode.CRAWL -> tickCrawl()
            Mode.LAUNCH -> tickLaunch()
        }
    }

    private fun tickFly() {
        if (landCooldown <= 0 && phantom.y > phantom.level().minBuildHeight + 4 && phantom.random.nextInt(220) == 0) {
            val spot = PhantomPerch.spot(phantom.level(), phantom.random, phantom.x, phantom.z, phantom.y, 8.0, 1.5, 4.5)
            if (spot != null) {
                land = spot
                mode = Mode.LAND
                modeTicks = 80
                return
            }
        }
        if (this.recalcCooldown-- <= 0) {
            this.recalcCooldown = 70 + phantom.random.nextInt(70)
            heading += (phantom.random.nextDouble() - 0.5) * 1.1
            climb = (phantom.random.nextDouble() - 0.42) * 0.28
        }
        heading += 0.016 + (phantom.random.nextDouble() - 0.5) * 0.012
        val ahead = 7.0
        val targetX = phantom.x + cos(heading) * ahead
        val targetZ = phantom.z + sin(heading) * ahead
        val targetY = phantom.y + climb * 5.0
        phantom.moveControl.setWantedPosition(targetX, targetY, targetZ, 0.48)
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

    private fun findTemptingPlayer(): Player? {
        val tameItem = ServerConfig.CONFIG.resolveTameItem()
        return phantom.level().getNearestPlayer(phantom, TEMPT_RANGE)?.takeIf { player ->
            player.isAlive && (player.mainHandItem.`is`(tameItem) || player.offhandItem.`is`(tameItem))
        }
    }
}
