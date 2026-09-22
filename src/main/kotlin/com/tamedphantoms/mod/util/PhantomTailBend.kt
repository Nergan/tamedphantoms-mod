package com.tamedphantoms.mod.util

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.util.Mth
import net.minecraft.world.entity.monster.Phantom

/**
 * Хвост отстаёт от поворота и возвращается медленно.
 * Считается один раз за тик: setupAnim зовут по разу на каждый слой,
 * и сглаживание на каждый вызов дёргало кончик, как антенну.
 */
object PhantomTailBend {

    const val MAX_RADIANS = 0.3f

    /** Кончик повторяет основание, с совсем небольшим запасом. */
    const val TIP_FOLLOW = 1.12f

    private class State(
        var bend: Float = 0f,
        var previous: Float = 0f,
        var yaw: Float = 0f,
        var tick: Int = Int.MIN_VALUE,
    )

    private val bendById = HashMap<Int, State>()

    fun targetRadians(yawDeltaDegrees: Float): Float =
        (-yawDeltaDegrees * 0.015f).coerceIn(-MAX_RADIANS, MAX_RADIANS)

    fun visual(phantom: Phantom, partial: Float): Float {
        val state = bendById.getOrPut(phantom.id) { State() }
        val tick = phantom.tickCount
        if (state.tick != tick) {
            state.previous = state.bend
            val raw = if (phantom is TamedPhantomEntity) {
                phantom.turnYawDelta
            } else {
                Mth.wrapDegrees(phantom.yRot - phantom.yRotO)
            }
            state.yaw += (raw - state.yaw) * 0.22f
            val target = targetRadians(state.yaw)
            state.bend += (target - state.bend) * 0.1f
            state.tick = tick
            if (bendById.size > 200) bendById.clear()
        }
        return Mth.lerp(partial.coerceIn(0f, 1f), state.previous, state.bend)
    }
}
