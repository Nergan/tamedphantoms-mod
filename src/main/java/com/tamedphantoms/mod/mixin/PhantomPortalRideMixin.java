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
 * Портал седока проходит через фантома. Ваниль везёт пассажиров вместе с ездовым,
 * как свинью, и сажает их на копию в новом измерении.
 */
@Mixin(ServerPlayer.class)
public abstract class PhantomPortalRideMixin {
    @Inject(
            method = "changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void tamedphantomsCarryPortal(DimensionTransition transition, CallbackInfoReturnable<Entity> cir) {
        if (PhantomDismount.traveling()) return;
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!(player.getVehicle() instanceof TamedPhantomEntity phantom)) return;
        if (!phantom.isAlive() || phantom.isRemoved()) return;
        PhantomDismount.beginTravel();
        try {
            phantom.changeDimension(transition);
            cir.setReturnValue(player);
        } finally {
            PhantomDismount.endTravel();
        }
    }
}
