package com.tamedphantoms.mod.mixin;

import com.tamedphantoms.mod.entity.TamedPhantomEntity;
import com.tamedphantoms.mod.event.PhantomDismount;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * Телепорт седока переносит фантома. Игрок из седла не выходит:
 * /tp двигает пару вместе, портал выпускает её так же, как свинью.
 */
@Mixin(ServerPlayer.class)
public abstract class PhantomRideTeleportMixin {
    @Inject(
            method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void tamedphantomsCarryTransition(TeleportTransition transition, CallbackInfoReturnable<ServerPlayer> ci) {
        if (PhantomDismount.traveling() || transition.asPassenger()) return;
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!(player.getVehicle() instanceof TamedPhantomEntity phantom)) return;
        if (!phantom.isAlive() || phantom.isRemoved()) return;
        PhantomDismount.beginTravel();
        try {
            phantom.teleport(transition);
            ci.setReturnValue(player);
        } finally {
            PhantomDismount.endTravel();
        }
    }

    @Inject(
            method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void tamedphantomsCarryCommand(
            ServerLevel level,
            double x,
            double y,
            double z,
            Set<Relative> relatives,
            float yRot,
            float xRot,
            boolean setCamera,
            CallbackInfoReturnable<Boolean> ci
    ) {
        if (PhantomDismount.traveling()) return;
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!(player.getVehicle() instanceof TamedPhantomEntity phantom)) return;
        if (!phantom.isAlive() || phantom.isRemoved()) return;
        PhantomDismount.beginTravel();
        try {
            ci.setReturnValue(PhantomDismount.carryCommand(player, phantom, level, x, y, z, relatives, yRot, xRot));
        } finally {
            PhantomDismount.endTravel();
        }
    }

    @Inject(method = "teleportTo(DDD)V", at = @At("HEAD"), cancellable = true)
    private void tamedphantomsCarryAbsolute(double x, double y, double z, CallbackInfo ci) {
        if (PhantomDismount.traveling()) return;
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!(player.getVehicle() instanceof TamedPhantomEntity phantom)) return;
        if (!phantom.isAlive() || phantom.isRemoved()) return;
        PhantomDismount.beginTravel();
        try {
            phantom.teleportTo(x, y, z);
            ci.cancel();
        } finally {
            PhantomDismount.endTravel();
        }
    }

    @Inject(method = "teleportRelative(DDD)V", at = @At("HEAD"), cancellable = true)
    private void tamedphantomsCarryRelative(double x, double y, double z, CallbackInfo ci) {
        if (PhantomDismount.traveling()) return;
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!(player.getVehicle() instanceof TamedPhantomEntity phantom)) return;
        if (!phantom.isAlive() || phantom.isRemoved()) return;
        PhantomDismount.beginTravel();
        try {
            phantom.teleportTo(phantom.getX() + x, phantom.getY() + y, phantom.getZ() + z);
            ci.cancel();
        } finally {
            PhantomDismount.endTravel();
        }
    }
}
