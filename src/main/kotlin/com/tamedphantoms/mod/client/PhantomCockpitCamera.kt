package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.config.ClientConfig
import com.tamedphantoms.mod.util.PhantomCockpit
import com.tamedphantoms.mod.util.PhantomQuat
import com.tamedphantoms.mod.util.PhantomRiderEye
import com.tamedphantoms.mod.util.PhantomRiderNeck
import com.tamedphantoms.mod.util.PhantomRiderSight
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

/**
 * Камера первого лица верхом на ручном фантоме: глаза на спине, взгляд держит шея ([PhantomRiderNeck]).
 * Шея шагает раз в кадр из настройки камеры. Прицел берёт тот же взгляд, чтобы перекрестие
 * показывало то, во что попадёт клик.
 */
object PhantomCockpitCamera {

    class View(val yaw: Float, val pitch: Float, val roll: Float)

    private val neck = PhantomRiderNeck()
    private var vehicleId = NO_VEHICLE
    private var lastTime = Double.NaN

    /** Двигает шею и отдаёт углы камеры. null — камера ванильная. */
    fun update(entity: Entity?, partial: Float): View? {
        val player = entity as? Player
        val vehicle = player?.vehicle
        val attitude = player?.let { attitude(it, partial) }
        if (player == null || vehicle == null || attitude == null) {
            vehicleId = NO_VEHICLE
            return null
        }
        if (vehicle.id != vehicleId) {
            vehicleId = vehicle.id
            neck.reset()
            lastTime = Double.NaN
        }
        val now = player.tickCount + partial.toDouble()
        val seconds = if (lastTime.isNaN()) 0.0 else ((now - lastTime) / TICKS_PER_SECOND).coerceIn(0.0, MAX_STEP)
        lastTime = now
        neck.step(
            PhantomRiderSight.bodyTilt(attitude.pitch, attitude.bank),
            seconds,
            ClientConfig.cameraHorizonHold().toDouble(),
            ClientConfig.cameraSmoothingSeconds(),
        )
        val yaw = player.getViewYRot(partial)
        val pitch = player.getViewXRot(partial)
        if (neck.shown.angle() < STILL) return View(yaw, pitch, 0f)
        val angles = PhantomRiderSight.angles(camera(attitude.yaw, yaw, pitch), yaw)
        return View(angles[0], angles[1], angles[2])
    }

    /** Глаза своего игрока в первом лице верхом. null — ванильные. */
    fun aimEye(entity: Any, partial: Float): Vec3? {
        val player = firstPersonRider(entity) ?: return null
        return PhantomRiderEye.of(player, partial)
    }

    /** Направление прицела своего игрока в первом лице верхом, по шее последнего кадра. null — ванильное. */
    fun aimLook(entity: Any, partial: Float): Vec3? {
        val player = firstPersonRider(entity) ?: return null
        if (player.vehicle?.id != vehicleId || neck.shown.angle() < STILL) return null
        val attitude = attitude(player, partial) ?: return null
        val forward = PhantomRiderSight.forward(camera(attitude.yaw, player.getViewYRot(partial), player.getViewXRot(partial)))
        return Vec3(forward[0], forward[1], forward[2])
    }

    private fun camera(bodyYaw: Float, yaw: Float, pitch: Float): PhantomQuat =
        PhantomRiderSight.worldTilt(bodyYaw, neck.shown) * PhantomRiderSight.look(yaw, pitch)

    private fun attitude(player: Player, partial: Float): PhantomCockpit.Attitude? =
        (player.vehicle as? PhantomCockpit)?.cockpitAttitude(partial)

    private fun firstPersonRider(entity: Any): Player? {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return null
        if (entity !== player || minecraft.cameraEntity !== player) return null
        if (!minecraft.options.cameraType.isFirstPerson) return null
        return player
    }

    private const val NO_VEHICLE = Int.MIN_VALUE
    private const val TICKS_PER_SECOND = 20.0
    private const val MAX_STEP = 0.25
    private const val STILL = 1.0e-5
}
