package com.tamedphantoms.mod.entity.ai

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.goal.Goal
import java.util.EnumSet

/**
 * "Режим самозащиты": пока у фантома активен таймер гнева (выставляется в
 * [TamedPhantomEntity.startDefending], вызывается из [com.tamedphantoms.mod.event.PhantomDamageHandler]),
 * фантом подлетает к обидчику и кусает его обычной ближней атакой — точно
 * так же, как это делал бы дикий фантом. Владелец фантома НЕ исключается
 * из потенциальных целей: если ударить своего же ручного фантома, он
 * укусит в ответ (см. ТЗ).
 *
 * Специально НЕ переиспользует ванильный внутренний класс атаки Phantom
 * (он package-private и недоступен для наследников) — реализует простую,
 * но функционально эквивалентную атаку "подлететь и укусить".
 */
class TamedPhantomDefendGoal(private val phantom: TamedPhantomEntity) : Goal() {

    private var target: LivingEntity? = null
    private var attackCooldown = 0

    init {
        this.flags = EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)
    }

    override fun canUse(): Boolean {
        if (phantom.isVehicle() || phantom.isOrderedToSit()) return false
        val target = phantom.getDefendTarget() ?: return false
        if (!target.isAlive) return false
        this.target = target
        return true
    }

    override fun canContinueToUse(): Boolean {
        if (phantom.isVehicle() || phantom.isOrderedToSit()) return false
        val current = phantom.getDefendTarget()
        if (current == null || !current.isAlive) return false
        this.target = current
        return true
    }

    override fun stop() {
        if (phantom.glanceTarget === this.target) {
            phantom.glanceTarget = null
        }
        this.target = null
        this.attackCooldown = 0
    }

    override fun tick() {
        val target = this.target ?: return
        phantom.glanceTarget = target

        val distSq = phantom.distanceToSqr(target)
        // Подлетаем к цели — переиспользуем штатный moveControl фантома (тот же,
        // которым он пользуется в ванильном кружении), просто указывая ему точку цели.
        val hover = if (distSq > 36.0) target.bbHeight * 0.9 + 1.1 else target.bbHeight * 0.45
        phantom.moveControl.setWantedPosition(target.x, target.y + hover, target.z, 1.35)

        if (this.attackCooldown > 0) {
            this.attackCooldown--
        }

        val attackRangeSq = (phantom.bbWidth + target.bbWidth).let { it * it * 0.6 + 1.0 }
        if (distSq <= attackRangeSq && this.attackCooldown <= 0) {
            val level = phantom.level()
            if (level is ServerLevel) {
                val damage = phantom.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).toFloat()
                target.hurt(phantom.damageSources().mobAttack(phantom), damage)
                phantom.playSound(net.minecraft.sounds.SoundEvents.PHANTOM_BITE, 1.0f, 1.0f)
                phantom.swing(net.minecraft.world.InteractionHand.MAIN_HAND)
            }
            this.attackCooldown = 20
        }
    }
}
