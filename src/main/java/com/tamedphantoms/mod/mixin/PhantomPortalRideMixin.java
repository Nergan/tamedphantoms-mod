package com.tamedphantoms.mod.mixin;

import com.tamedphantoms.mod.entity.TamedPhantomEntity;
import com.tamedphantoms.mod.event.PhantomDismount;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Портал игрока идёт через {@code ServerPlayer.changeDimension}, а не через
 * {@code teleportTo}. Снятие там не вызывается, и фантом остаётся в старом измерении.
 * Если измерение меняет сам фантом, пассажир к этому моменту уже снят, и ваниль сажает его обратно.
 */
@Mixin(ServerPlayer.class)
public abstract class PhantomPortalRideMixin {
    @Inject(
            method = "changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;",
            at = @At("HEAD")
    )
    private void tamedphantomsCapturePortal(DimensionTransition transition, CallbackInfoReturnable<Entity> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.getVehicle() instanceof TamedPhantomEntity) {
            PhantomDismount.dismountForTeleport(player);
        }
    }

    @Inject(
            method = "changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;",
            at = @At("RETURN")
    )
    private void tamedphantomsFinishPortal(DimensionTransition transition, CallbackInfoReturnable<Entity> cir) {
        PhantomDismount.finishTeleport((ServerPlayer) (Object) this);
    }
}
