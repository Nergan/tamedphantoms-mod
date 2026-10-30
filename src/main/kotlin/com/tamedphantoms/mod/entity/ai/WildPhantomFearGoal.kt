package com.tamedphantoms.mod.entity.ai

import com.tamedphantoms.mod.entity.PhantomFear
import com.tamedphantoms.mod.entity.PhantomMoveTargetAccess
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.phys.Vec3
import java.util.EnumSet

/**
 * Дикий фантом уходит от прирученного или освобождённого.
 * Приоритет 1 с флагом MOVE перебивает ванильные пике и кружение,
 * а приманка предметом приручения (приоритет 0) перебивает этот страх.
 */
class WildPhantomFearGoal(private val phantom: Phantom) : Goal() {

    private var flee: Vec3 = Vec3.ZERO
    private var fleeTicks = 0

    init {
        flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse(): Boolean {
        if (phantom is TamedPhantomEntity) return false
        if (phantom.tickCount % SCAN_TICKS != phantom.id % SCAN_TICKS) return false
        return PhantomFear.protector(phantom) != null
    }

    override fun canContinueToUse(): Boolean = fleeTicks > 0

    override fun requiresUpdateEveryTick(): Boolean = true

    override fun start() {
        fleeTicks = FLEE_TICKS + phantom.random.nextInt(40)
        retarget()
    }

    override fun tick() {
        fleeTicks--
        if (fleeTicks % SCAN_TICKS == 0 && PhantomFear.protector(phantom) != null) {
            fleeTicks = maxOf(fleeTicks, FLEE_TICKS / 2)
            retarget()
        }
        phantom.target = null
        PhantomMoveTargetAccess.set(phantom, flee)
    }

    override fun stop() {
        PhantomFear.settle(phantom, flee)
    }

    private fun retarget() {
        val protector = PhantomFear.protector(phantom) ?: return
        flee = PhantomFear.fleePoint(phantom, protector)
        phantom.target = null
        PhantomFear.settle(phantom, flee)
    }

    private companion object {
        const val SCAN_TICKS = 10
        const val FLEE_TICKS = 80
    }
}
