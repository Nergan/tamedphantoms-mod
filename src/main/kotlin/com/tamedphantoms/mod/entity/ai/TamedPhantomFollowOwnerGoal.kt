package com.tamedphantoms.mod.entity.ai

import com.tamedphantoms.mod.entity.PhantomOwnerRecall
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.PhantomOwnerTeleport
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.player.Player
import java.util.EnumSet
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Следование за хозяином: издалека догоняет, рядом медленно кружит
 * вокруг него, плавно меняя радиус и высоту.
 */
class TamedPhantomFollowOwnerGoal(private val phantom: TamedPhantomEntity) : Goal() {

    companion object {
        private const val CATCH_UP_DISTANCE = 8.0
    }

    private var owner: Player? = null
    private var orbitAngle = 0.0
    private var orbitRadius = 2.4
    private var hoverHeight = 1.5
    private var radiusTicks = 0
    private var heightTicks = 0
    private var desiredRadius = 2.4
    private var desiredHeight = 1.5

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
    }

    override fun stop() {
        owner = null
    }

    override fun tick() {
        val owner = this.owner ?: return
        if (PhantomOwnerRecall.tryTeleportToOwner(phantom, owner)) {
            return
        }
        if (phantom.level() !== owner.level()) {
            return
        }
        val dist = sqrt(phantom.distanceToSqr(owner))
        if (dist > PhantomOwnerTeleport.DISTANCE) {
            return
        }

        if (radiusTicks-- <= 0) {
            radiusTicks = 36 + phantom.random.nextInt(48)
            desiredRadius = if (dist > CATCH_UP_DISTANCE) {
                0.7 + phantom.random.nextDouble() * 0.6
            } else {
                1.8 + phantom.random.nextDouble() * 2.0
            }
        }
        if (heightTicks-- <= 0) {
            heightTicks = 28 + phantom.random.nextInt(40)
            desiredHeight = 0.9 + phantom.random.nextDouble() * 1.8
        }

        orbitRadius += (desiredRadius - orbitRadius) * 0.06
        hoverHeight += (desiredHeight - hoverHeight) * 0.05

        val catchingUp = dist > CATCH_UP_DISTANCE
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

        val speed = when {
            dist > CATCH_UP_DISTANCE -> 1.05
            dist > 4.5 -> 0.40
            else -> 0.26
        }
        phantom.moveControl.setWantedPosition(targetX, targetY, targetZ, speed)
        if (!catchingUp) {
            phantom.lookControl.setLookAt(owner, 12.0f, 12.0f)
        }
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
