package com.tamedphantoms.mod.mixin;

import com.tamedphantoms.mod.event.PhantomDismount;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Срабатывает до того, как ваниль решает, забирать ли ездовое с выходящим игроком.
 * Пассажир к этому моменту уже с зельями и уже не в седле, а хозяин всё ещё сидит.
 */
@Mixin(PlayerList.class)
public abstract class PhantomLogoutMixin {
    @Inject(method = "remove(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"))
    private void tamedphantomsLogout(ServerPlayer player, CallbackInfo ci) {
        PhantomDismount.onLogout(player);
    }
}
