package com.tamedphantoms.mod.mixin

import com.tamedphantoms.mod.util.PhantomRiderEye
import net.minecraft.client.Camera
import net.minecraft.client.DeltaTracker
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.ModifyArg
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

private const val SET_POSITION = "Lnet/minecraft/client/Camera;setPosition(DDD)V"
private const val SET_POSITION_VEC = "Lnet/minecraft/client/Camera;setPosition(Lnet/minecraft/world/phys/Vec3;)V"

/**
 * Первое лицо верхом: глаза на спине фантома. `setPosition` ищут и в `update`, и в `alignWithEntity`:
 * какой из них зовёт постановку, зависит от сборки. Нет цели — миксин молчит.
 */
@Mixin(Camera::class)
abstract class PhantomCockpitSeatMixin {

    @Shadow
    private lateinit var entity: Entity

    @Shadow
    abstract fun isDetached(): Boolean

    @Unique
    private var tamedphantomsSeatPartial = 0f

    @Unique
    private var tamedphantomsSeatDone = false

    @Unique
    private var tamedphantomsSeat: Vec3? = null

    @Inject(method = ["update"], at = [At("HEAD")])
    private fun tamedphantomsSeatBegin(delta: DeltaTracker, ci: CallbackInfo) {
        tamedphantomsSeatPartial = delta.getGameTimeDeltaPartialTick(true)
        tamedphantomsSeatDone = false
        tamedphantomsSeat = null
    }

    @ModifyArg(method = ["update"], at = [At(value = "INVOKE", target = SET_POSITION)], index = 0, require = 0)
    private fun tamedphantomsSeatX(x: Double, y: Double, z: Double): Double = seatAt(y)?.x ?: x

    @ModifyArg(method = ["update"], at = [At(value = "INVOKE", target = SET_POSITION)], index = 1, require = 0)
    private fun tamedphantomsSeatY(x: Double, y: Double, z: Double): Double = seatAt(y)?.y ?: y

    @ModifyArg(method = ["update"], at = [At(value = "INVOKE", target = SET_POSITION)], index = 2, require = 0)
    private fun tamedphantomsSeatZ(x: Double, y: Double, z: Double): Double = seatAt(y)?.z ?: z

    @ModifyArg(method = ["alignWithEntity"], at = [At(value = "INVOKE", target = SET_POSITION)], index = 0, require = 0)
    private fun tamedphantomsSeatAlignX(x: Double, y: Double, z: Double): Double = seatAt(y)?.x ?: x

    @ModifyArg(method = ["alignWithEntity"], at = [At(value = "INVOKE", target = SET_POSITION)], index = 1, require = 0)
    private fun tamedphantomsSeatAlignY(x: Double, y: Double, z: Double): Double = seatAt(y)?.y ?: y

    @ModifyArg(method = ["alignWithEntity"], at = [At(value = "INVOKE", target = SET_POSITION)], index = 2, require = 0)
    private fun tamedphantomsSeatAlignZ(x: Double, y: Double, z: Double): Double = seatAt(y)?.z ?: z

    @ModifyArg(method = ["update"], at = [At(value = "INVOKE", target = SET_POSITION_VEC)], index = 0, require = 0)
    private fun tamedphantomsSeatVec(position: Vec3): Vec3 = seatAt(position.y) ?: position

    @ModifyArg(method = ["alignWithEntity"], at = [At(value = "INVOKE", target = SET_POSITION_VEC)], index = 0, require = 0)
    private fun tamedphantomsSeatAlignVec(position: Vec3): Vec3 = seatAt(position.y) ?: position

    @Unique
    private fun seatAt(cameraY: Double): Vec3? {
        if (tamedphantomsSeatDone) return tamedphantomsSeat
        tamedphantomsSeatDone = true
        if (isDetached()) return null
        val rider = entity as? Player ?: return null
        val partial = tamedphantomsSeatPartial
        val eyeHeight = cameraY - Mth.lerp(partial.toDouble(), rider.yo, rider.y)
        tamedphantomsSeat = PhantomRiderEye.of(rider, partial, eyeHeight)
        return tamedphantomsSeat
    }
}
