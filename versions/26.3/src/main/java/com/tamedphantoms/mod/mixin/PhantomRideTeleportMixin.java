package com.tamedphantoms.mod.mixin;

import com.tamedphantoms.mod.event.PhantomDismount;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Телепорт седока забирает фантома с собой, а не бросает игрока сквозь блоки. */
@Mixin(ServerPlayer.class)
public abstract class PhantomRideTeleportMixin {
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
        PhantomDismount.finishTeleport((ServerPlayer) (Object) this);
    }
}
