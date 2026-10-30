package com.tamedphantoms.mod.entity

import com.tamedphantoms.mod.util.PhantomManeuvers
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ведёт фантома без всадника по траектории фигуры. Вызывают обе сборки управления полётом.
 * Фигура начинается только в свободном воздухе и гаснет при первом касании.
 */
object PhantomManeuverPilot {

    /** true — в этом тике летит фигура, обычное управление молчит. */
    fun tick(phantom: TamedPhantomEntity): Boolean {
        val kind = phantom.maneuverKind() ?: return false
        val elapsed = phantom.maneuverElapsed()
        if (!PhantomManeuvers.active(kind, elapsed) || phantom.isVehicle || phantom.isOrderedToSit() ||
            phantom.horizontalCollision || phantom.verticalCollision || phantom.isInWater
        ) {
            phantom.endManeuver()
            return false
        }
        val motion = PhantomManeuvers.motion(kind, elapsed, phantom.maneuverSpeed, phantom.maneuverSide)
        phantom.yRot += motion.yawStep
        phantom.yBodyRot = phantom.yRot
        phantom.xRot = 0f
        val yaw = Math.toRadians(phantom.yRot.toDouble())
        val forwardX = -sin(yaw)
        val forwardZ = cos(yaw)
        val leftX = cos(yaw)
        val leftZ = sin(yaw)
        phantom.deltaMovement = Vec3(
            forwardX * motion.forward + leftX * motion.left,
            motion.up,
            forwardZ * motion.forward + leftZ * motion.left,
        )
        return true
    }

    /** Хватит ли воздуха вокруг: вверх и вниз на высоту фигуры, вперёд на её путь. */
    fun hasRoom(phantom: TamedPhantomEntity, kind: PhantomManeuvers.Kind, speed: Double): Boolean {
        val (up, down) = PhantomManeuvers.clearance(kind, speed)
        val yaw = Math.toRadians(phantom.yRot.toDouble())
        val reach = speed * kind.ticks * 0.7
        val box = phantom.boundingBox.inflate(1.5)
            .expandTowards(0.0, up + 1.5, 0.0)
            .expandTowards(0.0, -(down + 1.5), 0.0)
            .expandTowards(-sin(yaw) * reach, 0.0, cos(yaw) * reach)
        val level = phantom.level()
        return level.noCollision(phantom, box) && !level.containsAnyLiquid(box)
    }

    /** Начать фигуру, если вокруг свободно. Скорость входа не ниже половины блока за тик. */
    fun tryStart(phantom: TamedPhantomEntity, kind: PhantomManeuvers.Kind): Boolean {
        if (phantom.level().isClientSide || phantom.isVehicle || phantom.isOrderedToSit() || phantom.isInWater) return false
        if (phantom.maneuverKind() != null) return false
        val motion = phantom.deltaMovement
        val speed = Math.sqrt(motion.x * motion.x + motion.z * motion.z).coerceIn(0.32, 0.7)
        if (!hasRoom(phantom, kind, speed)) return false
        return phantom.startManeuver(kind, speed)
    }
}
