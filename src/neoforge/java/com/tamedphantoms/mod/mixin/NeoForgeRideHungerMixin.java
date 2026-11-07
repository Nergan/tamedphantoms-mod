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
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * NeoForge splits the health bar into its own methods and stacks rows with rightHeight.
 * Hearts and air move up one row while riding, and hunger is drawn back into the lower slot.
 */
@Mixin(Gui.class)
public abstract class NeoForgeRideHungerMixin {
    @Shadow
    private void renderFood(GuiGraphics graphics, Player player, int y, int x) {
    }

    @ModifyArg(
            method = {
                    "renderVehicleHealth(Lnet/minecraft/client/gui/GuiGraphics;)V",
                    "renderAirLevel(Lnet/minecraft/client/gui/GuiGraphics;)V"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V",
                    remap = false
            ),
            index = 2
    )
    private int tamedphantomsLiftRow(int y) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.getVehicle() instanceof TamedPhantomEntity) {
            return y - 10;
        }
        return y;
    }

    @Inject(method = "renderPlayerHealth(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("TAIL"))
    private void tamedphantomsHunger(GuiGraphics graphics, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !(player.getVehicle() instanceof TamedPhantomEntity)) return;
        int y = graphics.guiHeight() - 39;
        int right = graphics.guiWidth() / 2 + 91;
        this.renderFood(graphics, player, y, right);
    }
}
