package com.tamedphantoms.mod.loader.fabric.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * У ванили нет Entity.getPersistentData(), его добавляет NeoForge.
 * Тег пишется в NBT сущности и читается обратно при загрузке.
 */
@Mixin(Entity.class)
public class EntityPersistentDataMixin implements PersistentDataCarrier {

    @Unique
    private CompoundTag tamedphantoms$data = new CompoundTag();

    @Override
    public CompoundTag tamedphantomsPersistentData() {
        return this.tamedphantoms$data;
    }

    @Inject(method = "saveWithoutId", at = @At("HEAD"))
    private void tamedphantoms$save(CompoundTag tag, CallbackInfoReturnable<CompoundTag> ci) {
        if (!this.tamedphantoms$data.isEmpty()) {
            tag.put("TamedPhantomsData", this.tamedphantoms$data);
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void tamedphantoms$load(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("TamedPhantomsData", Tag.TAG_COMPOUND)) {
            this.tamedphantoms$data = tag.getCompound("TamedPhantomsData");
        }
    }
}
