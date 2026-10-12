package com.tamedphantoms.mod.mixin;

import com.tamedphantoms.mod.entity.TamedPhantomEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Phantom.class)
public abstract class PhantomSitFlapMixin {

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playLocalSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"
        )
    )
    private void tamedphantoms$skipSitFlapSound(
        Level level,
        Entity entity,
        SoundEvent sound,
        SoundSource source,
        float volume,
        float pitch
    ) {
        if (((Object) this) instanceof TamedPhantomEntity pet && pet.isOrderedToSit()) {
            return;
        }
        level.playLocalSound(entity, sound, source, volume, pitch);
    }

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
        )
    )
    private void tamedphantoms$skipSitFlapParticles(
        Level level,
        ParticleOptions particle,
        double x,
        double y,
        double z,
        double xSpeed,
        double ySpeed,
        double zSpeed
    ) {
        if (((Object) this) instanceof TamedPhantomEntity pet && pet.isOrderedToSit()) {
            return;
        }
        level.addParticle(particle, x, y, z, xSpeed, ySpeed, zSpeed);
    }
}
