package com.tamedphantoms.mod.loader.fabric.mixin;

import com.mojang.serialization.Codec;
import com.tamedphantoms.mod.loader.fabric.PersistentDataCarrier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityPersistentDataMixin implements PersistentDataCarrier {

    @Unique
    private static final Codec<CompoundTag> TAMEDPHANTOMS_TAG = CompoundTag.CODEC;

    @Unique
    private CompoundTag tamedphantoms$data = new CompoundTag();

    @Override
    public CompoundTag tamedphantomsPersistentData() {
        return this.tamedphantoms$data;
    }

    @Inject(method = "saveWithoutId", at = @At("HEAD"))
    private void tamedphantoms$save(ValueOutput output, CallbackInfo ci) {
        if (!this.tamedphantoms$data.isEmpty()) {
            output.store("TamedPhantomsData", TAMEDPHANTOMS_TAG, this.tamedphantoms$data);
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void tamedphantoms$load(ValueInput input, CallbackInfo ci) {
        input.read("TamedPhantomsData", TAMEDPHANTOMS_TAG).ifPresent(tag -> this.tamedphantoms$data = tag);
    }
}
