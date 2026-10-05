package com.tamedphantoms.mod.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.tamedphantoms.mod.client.ModKeyMappings;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ваниль держит в {@code MAP} одну привязку на физическую клавишу: кто
 * попал туда последним, тот и получает {@code set}/{@code click}.
 * «Снижение» по умолчанию сидит на левом Ctrl, «Взлёт» — на пробеле,
 * поэтому спринт или прыжок остаются false, пока фантом забрал слот.
 *
 * Пока одна из наших привязок использует эту клавишу, событие доходит
 * до каждой привязки с тем же кодом. На земле Ctrl снова включает бег,
 * пробел — прыжок. Верхом те же клавиши по-прежнему читает полёт.
 */
@Mixin(KeyMapping.class)
public class SharedPhysicalKeyMixin {

    @Unique
    private static final Map<KeyMapping, Integer> tamedphantoms$clicksBefore = new IdentityHashMap<>();

    @Inject(method = "set", at = @At("RETURN"))
    private static void tamedphantoms$shareHold(InputConstants.Key key, boolean held, CallbackInfo ci) {
        if (!tamedphantoms$claims(key)) {
            return;
        }
        for (KeyMapping mapping : tamedphantoms$registered()) {
            if (tamedphantoms$boundTo(mapping, key)) {
                mapping.setDown(held);
            }
        }
    }

    @Inject(method = "click", at = @At("HEAD"))
    private static void tamedphantoms$rememberClicks(InputConstants.Key key, CallbackInfo ci) {
        tamedphantoms$clicksBefore.clear();
        if (!tamedphantoms$claims(key)) {
            return;
        }
        for (KeyMapping mapping : tamedphantoms$registered()) {
            if (tamedphantoms$boundTo(mapping, key)) {
                tamedphantoms$clicksBefore.put(mapping, ((KeyMappingFields) mapping).tamedphantoms$clickCount());
            }
        }
    }

    @Inject(method = "click", at = @At("RETURN"))
    private static void tamedphantoms$shareClick(InputConstants.Key key, CallbackInfo ci) {
        if (!tamedphantoms$claims(key)) {
            return;
        }
        for (Map.Entry<KeyMapping, Integer> entry : tamedphantoms$clicksBefore.entrySet()) {
            KeyMappingFields fields = (KeyMappingFields) entry.getKey();
            if (fields.tamedphantoms$clickCount() == entry.getValue()) {
                fields.tamedphantoms$clickCount(entry.getValue() + 1);
            }
        }
    }

    @Unique
    private static KeyMapping[] tamedphantoms$registered() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.options == null || minecraft.options.keyMappings == null) {
            return new KeyMapping[0];
        }
        return minecraft.options.keyMappings;
    }

    @Unique
    private static boolean tamedphantoms$claims(InputConstants.Key key) {
        return tamedphantoms$boundTo(ModKeyMappings.INSTANCE.getFLY_UP(), key)
            || tamedphantoms$boundTo(ModKeyMappings.INSTANCE.getFLY_DOWN(), key);
    }

    @Unique
    private static boolean tamedphantoms$boundTo(KeyMapping mapping, InputConstants.Key key) {
        return ((KeyMappingFields) mapping).tamedphantoms$key().equals(key);
    }
}
