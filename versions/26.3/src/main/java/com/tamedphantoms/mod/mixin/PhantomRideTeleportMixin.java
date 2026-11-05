package com.tamedphantoms.mod.mixin;

import com.tamedphantoms.mod.event.PhantomDismount;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Телепорт седока забирает фантома с собой, а не бросает игрока сквозь блоки. */
@Mixin(ServerPlayer.class)
public abstract class PhantomRideTeleportMixin {
    @Unique
    private boolean tamedphantoms$carry;
    @Unique
    private double tamedphantoms$x;
    @Unique
    private double tamedphantoms$y;
    @Unique
    private double tamedphantoms$z;

    @Redirect(
            method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;removeVehicle()V")
    )
    private void tamedphantomsReleaseForTeleport(ServerPlayer player) {
        PhantomDismount.dismountForTeleport(player);
    }

    @Inject(
            method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;",
            at = @At("RETURN")
    )
    private void tamedphantomsCarryMount(TeleportTransition transition, CallbackInfoReturnable<ServerPlayer> ci) {
        ServerPlayer arrived = ci.getReturnValue();
        if (arrived == null) return;
        Vec3 pos = transition.position();
        PhantomDismount.finishTeleportAt(arrived, transition.newLevel(), pos.x, pos.y, pos.z);
    }

    @Inject(method = "teleportTo(DDD)V", at = @At("HEAD"))
    private void tamedphantomsBeforeAbsolute(double x, double y, double z, CallbackInfo ci) {
        this.tamedphantoms$x = x;
        this.tamedphantoms$y = y;
        this.tamedphantoms$z = z;
        this.tamedphantoms$carry = true;
        PhantomDismount.dismountForTeleport((ServerPlayer) (Object) this);
    }

    @Inject(method = "teleportTo(DDD)V", at = @At("RETURN"))
    private void tamedphantomsAfterAbsolute(double x, double y, double z, CallbackInfo ci) {
        if (!this.tamedphantoms$carry) return;
        this.tamedphantoms$carry = false;
        ServerPlayer player = (ServerPlayer) (Object) this;
        PhantomDismount.finishTeleportAt(player, (ServerLevel) player.level(), this.tamedphantoms$x, this.tamedphantoms$y, this.tamedphantoms$z);
    }

    @Inject(method = "teleportRelative(DDD)V", at = @At("HEAD"))
    private void tamedphantomsBeforeRelative(double x, double y, double z, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        this.tamedphantoms$x = player.getX() + x;
        this.tamedphantoms$y = player.getY() + y;
        this.tamedphantoms$z = player.getZ() + z;
        this.tamedphantoms$carry = true;
        PhantomDismount.dismountForTeleport(player);
    }

    @Inject(method = "teleportRelative(DDD)V", at = @At("RETURN"))
    private void tamedphantomsAfterRelative(double x, double y, double z, CallbackInfo ci) {
        if (!this.tamedphantoms$carry) return;
        this.tamedphantoms$carry = false;
        ServerPlayer player = (ServerPlayer) (Object) this;
        PhantomDismount.finishTeleportAt(player, (ServerLevel) player.level(), this.tamedphantoms$x, this.tamedphantoms$y, this.tamedphantoms$z);
    }
}
