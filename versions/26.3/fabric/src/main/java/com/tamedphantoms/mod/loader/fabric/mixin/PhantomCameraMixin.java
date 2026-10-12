package com.tamedphantoms.mod.loader.fabric.mixin;

import com.tamedphantoms.mod.client.PhantomRideBank;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * У ванильной камеры нет крена. Поворот дописывается в кватернион после setRotation.
 */
@Mixin(Camera.class)
public abstract class PhantomCameraMixin {

    @Shadow private Entity entity;
    @Shadow private float xRot;
    @Shadow private float yRot;
    @Shadow private Quaternionf rotation;
    @Shadow private Vector3f forwards;
    @Shadow private Vector3f up;
    @Shadow private Vector3f left;

    @Shadow @Final private static Vector3fc FORWARDS;
    @Shadow @Final private static Vector3fc UP;
    @Shadow @Final private static Vector3fc LEFT;

    @Unique private boolean tamedphantoms$baseSet;
    @Unique private float tamedphantoms$roll;
    @Unique private float tamedphantoms$activeRoll;
    @Unique private float tamedphantoms$partial;

    @Inject(method = "update", at = @At("HEAD"))
    private void tamedphantoms$begin(DeltaTracker delta, CallbackInfo ci) {
        this.tamedphantoms$baseSet = false;
        this.tamedphantoms$partial = delta.getGameTimeDeltaPartialTick(true);
        this.tamedphantoms$roll = 0.0f;
        this.tamedphantoms$activeRoll = 0.0f;
    }

    @ModifyVariable(method = "setRotation", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private float tamedphantoms$pitch(float pitch) {
        if (!this.tamedphantoms$baseSet) {
            this.tamedphantoms$baseSet = true;
            PhantomRideBank.Angles angles = PhantomRideBank.INSTANCE.adjust(
                this.entity,
                this.tamedphantoms$partial,
                pitch,
                0.0f
            );
            if (angles == null) {
                this.tamedphantoms$roll = 0.0f;
                this.tamedphantoms$activeRoll = 0.0f;
                return pitch;
            }
            this.tamedphantoms$roll = angles.getRoll();
            this.tamedphantoms$activeRoll = angles.getRoll();
            return angles.getPitch();
        }
        return pitch;
    }

    @Inject(method = "setRotation", at = @At("RETURN"))
    private void tamedphantoms$roll(float yRot, float xRot, CallbackInfo ci) {
        if (this.tamedphantoms$activeRoll == 0.0f) {
            return;
        }
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
