package com.tamedphantoms.mod.util

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.util.Mth
import net.minecraft.world.entity.monster.Phantom

/**
 * Хвост отстаёт от поворота: гнётся в сторону, противоположную рысканию.
 * Положительный прирост yRot (поворот влево) даёт отрицательный yRot хвоста.
 */
object PhantomTailBend {

    const val MAX_RADIANS = 0.28f

    private val bendById = HashMap<Int, Float>()

    fun targetRadians(yawDeltaDegrees: Float): Float =
        (-yawDeltaDegrees * 0.028f).coerceIn(-MAX_RADIANS, MAX_RADIANS)

    fun visual(phantom: Phantom): Float {
        val delta = if (phantom is TamedPhantomEntity) {
            phantom.turnYawDelta
        } else {
            Mth.wrapDegrees(phantom.yRot - phantom.yRotO)
        }
        val next = approach(bendById[phantom.id] ?: 0f, targetRadians(delta))
        if (bendById.size > 200) bendById.clear()
        bendById[phantom.id] = next
        return next
    }

    private fun approach(current: Float, target: Float): Float = current + (target - current) * 0.45f
}
