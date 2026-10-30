package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player

/**
 * Как в кадре рисовать седока ручного фантома.
 *
 * Сдвиг ставит модель на седло: интерполяция игрока отстаёт от маунта на скорости.
 * Поворот — тот же, что у корпуса фантома ([TamedPhantomEntity.seatVisualShift]),
 * вокруг седла: в петле, бочке и крене седок движется с фантомом как одно целое.
 * Повернуть стек: Y(−[yaw]), X(−[pitch]), Z([bank]), Y([yaw]).
 */
class PhantomRiderPose(
    val dx: Double,
    val dy: Double,
    val dz: Double,
    val yaw: Float,
    val pitch: Float,
    val bank: Float,
) {
    val turned: Boolean get() = pitch != 0f || bank != 0f

    companion object {
        @JvmStatic
        fun of(entity: Entity, partial: Float): PhantomRiderPose? {
            val player = entity as? Player ?: return null
            val phantom = player.vehicle as? TamedPhantomEntity ?: return null
            if (phantom.isOrderedToSit()) return null
            val p = partial.toDouble()
            val shift = phantom.seatVisualShift(player, partial)
            return PhantomRiderPose(
                Mth.lerp(p, phantom.xo, phantom.x) + (player.x - phantom.x) + shift.x - Mth.lerp(p, player.xo, player.x),
                Mth.lerp(p, phantom.yo, phantom.y) + (player.y - phantom.y) + shift.y - Mth.lerp(p, player.yo, player.y),
                Mth.lerp(p, phantom.zo, phantom.z) + (player.z - phantom.z) + shift.z - Mth.lerp(p, player.zo, player.z),
                phantom.renderedBodyYaw(partial),
                phantom.renderedBodyPitch(partial),
                phantom.bankVisual(partial),
            )
        }
    }
}
