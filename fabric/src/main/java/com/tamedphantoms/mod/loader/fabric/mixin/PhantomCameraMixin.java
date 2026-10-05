package com.tamedphantoms.mod.loader.fabric.mixin;

import com.tamedphantoms.mod.client.PhantomRideBank;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * У ванильной камеры нет крена: его добавляет NeoForge. Здесь тот же поворот
 * дописывается в кватернион после [Camera.setRotation].
 */
@Mixin(Camera.class)
public abstract class PhantomCameraMixin {

    @Shadow private Entity entity;
    @Shadow private float xRot;
    @Shadow private float yRot;
    @Shadow private float partialTickTime;
    @Shadow private Quaternionf rotation;
    @Shadow private Vector3f forwards;
    @Shadow private Vector3f up;
    @Shadow private Vector3f left;

    @Shadow @Final private static Vector3f FORWARDS;
    @Shadow @Final private static Vector3f UP;
    @Shadow @Final private static Vector3f LEFT;

    private boolean tamedphantoms$baseSet;
    private boolean tamedphantoms$detached;
    private boolean tamedphantoms$reverse;
    private float tamedphantoms$roll;
    private float tamedphantoms$activeRoll;

    @Inject(method = "setup", at = @At("HEAD"))
    private void tamedphantoms$begin(BlockGetter level, Entity cameraEntity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo ci) {
        this.tamedphantoms$baseSet = false;
        this.tamedphantoms$detached = detached;
        this.tamedphantoms$reverse = thirdPersonReverse;
        this.tamedphantoms$roll = 0.0f;
        this.tamedphantoms$activeRoll = 0.0f;
    }

    @ModifyVariable(method = "setRotation", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private float tamedphantoms$pitch(float pitch) {
        if (!this.tamedphantoms$baseSet) {
            this.tamedphantoms$baseSet = true;
            PhantomRideBank.Angles angles = PhantomRideBank.INSTANCE.adjust(this.entity, this.partialTickTime, pitch, 0.0f);
            if (angles == null) {
                this.tamedphantoms$roll = 0.0f;
                this.tamedphantoms$activeRoll = 0.0f;
                return pitch;
            }
            this.tamedphantoms$roll = angles.getRoll();
            this.tamedphantoms$activeRoll = angles.getRoll();
            return angles.getPitch();
        }
        if (this.tamedphantoms$detached && this.tamedphantoms$reverse) {
            this.tamedphantoms$activeRoll = -this.tamedphantoms$roll;
        }
        return pitch;
    }

    @Inject(method = "setRotation", at = @At("RETURN"))
    private void tamedphantoms$roll(float yRot, float xRot, CallbackInfo ci) {
        if (this.tamedphantoms$activeRoll == 0.0f) return;
        float toRad = (float) (Math.PI / 180.0);
        this.rotation.rotationYXZ(
            (float) Math.PI - this.yRot * toRad,
            -this.xRot * toRad,
            -this.tamedphantoms$activeRoll * toRad
        );
        FORWARDS.rotate(this.rotation, this.forwards);
        UP.rotate(this.rotation, this.up);
        LEFT.rotate(this.rotation, this.left);
    }
}
