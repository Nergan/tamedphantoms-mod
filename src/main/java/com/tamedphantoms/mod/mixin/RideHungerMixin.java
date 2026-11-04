package com.tamedphantoms.mod.mixin;

import com.tamedphantoms.mod.entity.TamedPhantomEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Верхом ваниль рисует сердца фантома на месте сытости и сытость пропускает.
 * Сердца остаются, сытость рисуется строкой выше.
 */
@Mixin(Gui.class)
public abstract class RideHungerMixin {
    @Shadow
    private void renderFood(GuiGraphics graphics, Player player, int y, int x) {
    }

    @Inject(method = "renderPlayerHealth(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("TAIL"))
    private void tamedphantomsHunger(GuiGraphics graphics, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !(player.getVehicle() instanceof TamedPhantomEntity)) return;
        int lift = player.getAirSupply() < player.getMaxAirSupply() ? 20 : 10;
        int y = graphics.guiHeight() - 39 - lift;
        int right = graphics.guiWidth() / 2 + 91;
        this.renderFood(graphics, player, y, right);
    }
}
