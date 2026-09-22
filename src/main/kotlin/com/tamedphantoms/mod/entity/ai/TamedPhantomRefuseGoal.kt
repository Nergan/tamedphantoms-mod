package com.tamedphantoms.mod.entity.ai

import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.PhantomHeadLook
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import java.util.EnumSet
import kotlin.math.sqrt

/**
 * Ядовитая картошка (и предмет освобождения): фантом не подходит,
 * зависает и смотрит на игрока.
 */
class TamedPhantomRefuseGoal(private val phantom: TamedPhantomEntity) : Goal() {

    private var player: Player? = null
    private var linger = 0

    init {
        flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse(): Boolean {
        if (!phantom.tamed || phantom.isOrderedToSit() || phantom.isVehicle || phantom.isLeashed || phantom.isDefending()) {
            return false
        }
        player = find()
        if (player != null) linger = 45
        return player != null || linger > 0
    }

    override fun canContinueToUse(): Boolean = canUse()

    override fun requiresUpdateEveryTick(): Boolean = true

    override fun stop() {
        if (phantom.glanceTarget === player) {
            phantom.glanceTarget = null
        }
        player = null
        linger = 0
    }

    override fun tick() {
        val holder = find()
        if (holder != null) {
            player = holder
            linger = 45
        } else if (linger > 0) {
            linger--
        }
        val owner = ownerHolding()
        val look = owner ?: player
        if (look != null && look.isAlive) {
            phantom.glanceTarget = look
        }
        if (owner != null) {
            val yaw = PhantomHeadLook.yawDegrees(phantom.x, phantom.z, owner.x, owner.z)
            phantom.yRot = PhantomHeadLook.approachDegrees(phantom.yRot, yaw, 10f)
            phantom.yBodyRot = phantom.yRot
            phantom.yHeadRot = phantom.yRot
        }
        phantom.moveControl.setWantedPosition(phantom.x, phantom.y, phantom.z, 0.0)
        phantom.deltaMovement = phantom.deltaMovement.scale(0.7)
    }

    private fun ownerHolding(): Player? {
        val id = phantom.ownerUUID ?: return null
        val owner = phantom.level().players().firstOrNull { it.uuid == id } ?: return null
        if (!owner.isAlive || owner.isSpectator) return null
        if (phantom.distanceToSqr(owner) > 8.0 * 8.0) return null
        if (!holdsRefusal(owner)) return null
        return owner
    }

    private fun find(): Player? {
        val box = phantom.boundingBox.inflate(8.0)
        return phantom.level().getEntitiesOfClass(Player::class.java, box) { candidate ->
            candidate.isAlive && !candidate.isSpectator && holdsRefusal(candidate) &&
                sqrt(phantom.distanceToSqr(candidate)) <= 8.0
        }.minByOrNull { phantom.distanceToSqr(it) }
    }

    private fun holdsRefusal(player: Player): Boolean {
        val release = ServerConfig.CONFIG.resolveReleaseItem()
        val main = player.mainHandItem
        val off = player.offhandItem
        return main.`is`(Items.POISONOUS_POTATO) || off.`is`(Items.POISONOUS_POTATO) ||
            main.`is`(release) || off.`is`(release)
    }
}
