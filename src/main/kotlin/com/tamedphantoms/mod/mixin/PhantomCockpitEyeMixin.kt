package com.tamedphantoms.mod.mixin

import com.tamedphantoms.mod.util.PhantomRiderEye
import net.minecraft.client.Camera
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.phys.Vec3
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.ModifyArg
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

private const val SET_POSITION = "Lnet/minecraft/client/Camera;setPosition(DDD)V"

/**
 * Первое лицо верхом: глаза сидят на спине фантома ([PhantomRiderEye]), а не висят над ним.
 * Высоту глаз берёт у ванильной камеры: у неё своё сглаживание, когда седок пригибается.
 */
@Mixin(Camera::class)
abstract class PhantomCockpitEyeMixin {

    @Unique
    private var tamedphantomsSeatRider: Player? = null

    @Unique
    private var tamedphantomsSeatPartial = 0f

    @Unique
    private var tamedphantomsSeatDone = false

    @Unique
    private var tamedphantomsSeat: Vec3? = null

    @Inject(method = ["setup"], at = [At("HEAD")])
    private fun tamedphantomsSeatBegin(
        level: BlockGetter,
        entity: Entity,
        detached: Boolean,
        thirdPersonReverse: Boolean,
        partialTick: Float,
        ci: CallbackInfo,
    ) {
        tamedphantomsSeatRider = if (detached) null else entity as? Player
        tamedphantomsSeatPartial = partialTick
        tamedphantomsSeatDone = false
        tamedphantomsSeat = null
    }

    @ModifyArg(method = ["setup"], at = At(value = "INVOKE", target = SET_POSITION), index = 0)
    private fun tamedphantomsSeatX(x: Double, y: Double, z: Double): Double = tamedphantomsSeatAt(y)?.x ?: x

    @ModifyArg(method = ["setup"], at = At(value = "INVOKE", target = SET_POSITION), index = 1)
    private fun tamedphantomsSeatY(x: Double, y: Double, z: Double): Double = tamedphantomsSeatAt(y)?.y ?: y

    @ModifyArg(method = ["setup"], at = At(value = "INVOKE", target = SET_POSITION), index = 2)
    private fun tamedphantomsSeatZ(x: Double, y: Double, z: Double): Double = tamedphantomsSeatAt(y)?.z ?: z

    /** Считается один раз: следующие обработчики уже видят подменённые координаты. */
    @Unique
    private fun tamedphantomsSeatAt(cameraY: Double): Vec3? {
        if (tamedphantomsSeatDone) return tamedphantomsSeat
        tamedphantomsSeatDone = true
        val rider = tamedphantomsSeatRider ?: return null
        val partial = tamedphantomsSeatPartial
        val eyeHeight = cameraY - Mth.lerp(partial.toDouble(), rider.yo, rider.y)
        tamedphantomsSeat = PhantomRiderEye.of(rider, partial, eyeHeight)
        return tamedphantomsSeat
    }
}
