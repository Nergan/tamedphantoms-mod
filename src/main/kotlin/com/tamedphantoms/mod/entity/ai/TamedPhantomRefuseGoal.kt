package com.tamedphantoms.mod.entity.ai

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.PhantomHeadLook
import com.tamedphantoms.mod.util.PhantomHeldLook
import com.tamedphantoms.mod.util.PhantomTamingLogic
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.player.Player
import java.util.EnumSet

/**
 * Еда рядом: фантом замирает и поворачивается, пока предмет в руках.
 * Ядовитая картошка и предмет освобождения — мотание головой.
 * Любая другая еда — кивок, и только если фантому не хватает здоровья.
 * Седок этого фантома не считается.
 */
class TamedPhantomRefuseGoal(private val phantom: TamedPhantomEntity) : Goal() {

    private var player: Player? = null
    private var cached: PhantomHeldLook.OfferTarget? = null

    init {
        flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse(): Boolean {
        if (!this.eligible()) return false
        this.cached = this.find()
        return this.cached != null
    }

    override fun canContinueToUse(): Boolean = canUse()

    override fun requiresUpdateEveryTick(): Boolean = true

    override fun stop() {
        if (phantom.glanceTarget === player) {
            phantom.glanceTarget = null
        }
        phantom.offeringLock = false
        player = null
        cached = null
    }

    override fun tick() {
        val found = this.cached ?: return
        this.player = found.player
        phantom.offeringLock = true
        val look = found.player
        if (!look.isAlive) return
        phantom.glanceTarget = look
        val yaw = PhantomHeadLook.yawDegrees(phantom.x, phantom.z, look.x, look.z)
        phantom.yRot = PhantomHeadLook.approachDegrees(phantom.yRot, yaw, 10f)
        phantom.yBodyRot = phantom.yRot
        phantom.yHeadRot = yaw
        phantom.headLookPitch = PhantomHeadLook.pitchDegrees(
            phantom.x, phantom.eyeY, phantom.z, look.x, look.eyeY, look.z,
        )
        phantom.trackingLook = true
        phantom.moveControl.setWantedPosition(phantom.x, phantom.y, phantom.z, 0.0)
        phantom.deltaMovement = phantom.deltaMovement.scale(0.7)
    }

    private fun eligible(): Boolean =
        phantom.tamed && !phantom.isVehicle && !phantom.isLeashed && !phantom.isDefending()

    private fun find(): PhantomHeldLook.OfferTarget? = PhantomHeldLook.nearestOffer(
        phantom,
        phantom.ownerUUID,
        PhantomTamingLogic.canHeal(phantom.health, phantom.maxHealth),
    )
}
