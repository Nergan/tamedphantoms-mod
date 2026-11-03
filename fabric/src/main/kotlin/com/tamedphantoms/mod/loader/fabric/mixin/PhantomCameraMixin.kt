package com.tamedphantoms.mod.loader.fabric.mixin

import com.tamedphantoms.mod.client.PhantomCockpitCamera
import com.tamedphantoms.mod.client.PhantomRideBank
import net.minecraft.client.Camera
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.BlockGetter
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Unique
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.ModifyArg
import org.spongepowered.asm.mixin.injection.ModifyVariable
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

private const val ROTATION_YXZ = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;"
private const val DEGREES_TO_RADIANS = 0.017453292f

/**
 * У ванильной камеры нет крена: его добавляет NeoForge. Здесь первый `setRotation` в `setup`
 * получает углы седока, а крен дописывается третьим углом кватерниона.
 * В первом лице — взгляд из седла ([PhantomCockpitCamera]), в третьем — [PhantomRideBank].
 */
@Mixin(Camera::class)
abstract class PhantomCameraMixin {

    @Unique
    private var tamedphantomsActive = false

    @Unique
    private var tamedphantomsMirrored = false

    @Unique
    private var tamedphantomsCalls = 0

    @Unique
    private var tamedphantomsYaw = 0f

    @Unique
    private var tamedphantomsPitch = 0f

    @Unique
    private var tamedphantomsRoll = 0f

    @Inject(method = ["setup"], at = [At("HEAD")])
    private fun tamedphantomsBegin(
        level: BlockGetter,
        entity: Entity,
        detached: Boolean,
        thirdPersonReverse: Boolean,
        partialTick: Float,
        ci: CallbackInfo,
    ) {
        tamedphantomsActive = false
        tamedphantomsMirrored = detached && thirdPersonReverse
        tamedphantomsCalls = 0
        val cockpit = PhantomCockpitCamera.update(entity, partialTick)
        if (cockpit != null && !detached) {
            tamedphantomsYaw = cockpit.yaw
            tamedphantomsPitch = cockpit.pitch
            tamedphantomsRoll = cockpit.roll
            tamedphantomsActive = true
            return
        }
        val angles = PhantomRideBank.adjust(entity, partialTick, entity.getViewXRot(partialTick), 0f) ?: return
        tamedphantomsYaw = entity.getViewYRot(partialTick)
        tamedphantomsPitch = angles.pitch
        tamedphantomsRoll = angles.roll
        tamedphantomsActive = true
    }

    @ModifyVariable(method = ["setRotation"], at = At("HEAD"), ordinal = 0, argsOnly = true)
    private fun tamedphantomsYaw(yaw: Float): Float =
        if (tamedphantomsActive && tamedphantomsCalls == 0) tamedphantomsYaw else yaw

    @ModifyVariable(method = ["setRotation"], at = At("HEAD"), ordinal = 1, argsOnly = true)
    private fun tamedphantomsPitch(pitch: Float): Float =
        if (tamedphantomsActive && tamedphantomsCalls == 0) tamedphantomsPitch else pitch

    /** Вид спереди в третьем лице разворачивает камеру: крен меняет знак. */
    @ModifyArg(method = ["setRotation"], at = At(value = "INVOKE", target = ROTATION_YXZ), index = 2)
    private fun tamedphantomsRoll(roll: Float): Float {
        if (!tamedphantomsActive) return roll
        val applied = if (tamedphantomsCalls > 0 && tamedphantomsMirrored) -tamedphantomsRoll else tamedphantomsRoll
        return roll - applied * DEGREES_TO_RADIANS
    }

    @Inject(method = ["setRotation"], at = [At("RETURN")])
    private fun tamedphantomsCount(yaw: Float, pitch: Float, ci: CallbackInfo) {
        tamedphantomsCalls++
    }
}
