package com.tamedphantoms.mod.entity.ai

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.PhantomHeadLook
import com.tamedphantoms.mod.util.PhantomHeldLook
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.player.Player
import java.util.EnumSet

/**
 * Еда рядом: фантом замирает и поворачивается.
 * Ядовитая картошка и предмет освобождения — мотание головой.
 * Любая другая еда — кивок. Седок этого фантома не считается.
 */
class TamedPhantomRefuseGoal(private val phantom: TamedPhantomEntity) : Goal() {

    private var player: Player? = null
    private var linger = 0
    private var cached: PhantomHeldLook.OfferTarget? = null

    init {
        flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse(): Boolean {
        if (!phantom.tamed || phantom.isVehicle || phantom.isLeashed || phantom.isDefending()) {
            return false
        }
        val found = PhantomHeldLook.nearestOffer(phantom, phantom.ownerUUID)
        this.cached = found
        if (found != null) this.linger = 45
        return found != null || this.linger > 0
    }

    override fun canContinueToUse(): Boolean = canUse()

    override fun requiresUpdateEveryTick(): Boolean = true

    override fun stop() {
        if (phantom.glanceTarget === player) {
            phantom.glanceTarget = null
        }
        phantom.offeringLock = false
        player = null
        linger = 0
        cached = null
    }

    override fun tick() {
        val found = this.cached
        if (found != null) {
            this.player = found.player
            this.linger = 45
        } else if (this.linger > 0) {
            this.linger--
        }
        phantom.offeringLock = true
        val look = this.player
        if (look != null && look.isAlive) {
            phantom.glanceTarget = look
            val yaw = PhantomHeadLook.yawDegrees(phantom.x, phantom.z, look.x, look.z)
            phantom.yRot = PhantomHeadLook.approachDegrees(phantom.yRot, yaw, 10f)
            phantom.yBodyRot = phantom.yRot
            phantom.yHeadRot = phantom.yRot
        }
        phantom.moveControl.setWantedPosition(phantom.x, phantom.y, phantom.z, 0.0)
        phantom.deltaMovement = phantom.deltaMovement.scale(0.7)
    }
}
