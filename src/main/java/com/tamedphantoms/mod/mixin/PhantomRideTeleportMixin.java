package com.tamedphantoms.mod.mixin;

import com.tamedphantoms.mod.event.PhantomDismount;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/** Телепорт седока забирает фантома с собой, а не бросает игрока сквозь блоки. */
@Mixin(ServerPlayer.class)
public abstract class PhantomRideTeleportMixin {
    @Redirect(
            method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;stopRiding()V")
    )
    private void tamedphantomsReleaseForTeleport(ServerPlayer player) {
        PhantomDismount.dismountForTeleport(player);
    }

    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z", at = @At("RETURN"))
    private void tamedphantomsCarryMount(
            ServerLevel level,
            double x,
            double y,
            double z,
            Set<?> relatives,
            float yRot,
            float xRot,
            CallbackInfoReturnable<Boolean> ci
    ) {
        PhantomDismount.finishTeleport((ServerPlayer) (Object) this);
    }
}
