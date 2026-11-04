package com.tamedphantoms.mod.mixin;

import com.tamedphantoms.mod.entity.TamedPhantomEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * У фантома два ряда сердец, и ваниль ставит нижний на место сытости.
 * Сытость остаётся в обычной правой ячейке нижнего ряда, сердца — на ряд выше.
 * Пузырьки воздуха поднимаются вместе с сердцами, чтобы не сесть на верхний ряд.
 */
@Mixin(Hud.class)
public abstract class RideHungerMixin {
    @Shadow
    private void extractFood(GuiGraphicsExtractor graphics, Player player, int y, int x) {
    }

    @ModifyConstant(
            method = "extractVehicleHealth(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
            constant = @Constant(intValue = 39)
    )
    private int tamedphantomsLiftHearts(int baseline) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.getVehicle() instanceof TamedPhantomEntity) {
            return baseline + 10;
        }
        return baseline;
    }

    @ModifyVariable(method = "getAirBubbleYLine(II)I", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private int tamedphantomsLiftAir(int y) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.getVehicle() instanceof TamedPhantomEntity) {
            return y - 10;
        }
        return y;
    }

    @Inject(method = "extractPlayerHealth(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V", at = @At("TAIL"))
    private void tamedphantomsHunger(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        Player player = Minecraft.getInstance().player;
        if (player == null || !(player.getVehicle() instanceof TamedPhantomEntity)) return;
        int y = graphics.guiHeight() - 39;
        int right = graphics.guiWidth() / 2 + 91;
        this.extractFood(graphics, player, y, right);
    }
}
