package com.tamedphantoms.mod.mixin

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.level.Level
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Redirect

/**
 * Ванильный `Phantom.tick` на клиенте машет звуком и частицами крыльев по номеру тика,
 * а не по позе модели. У нашего фантома свой такт ([TamedPhantomEntity.wingParticle]
 * и звук идут из его тика), поэтому ванильный здесь пропускается. Дикие фантомы не меняются.
 */
@Mixin(Phantom::class)
abstract class PhantomFlapMixin {

    @Redirect(
        method = ["tick"],
        at = [
            At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/level/Level;playLocalSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
            ),
        ],
    )
    private fun tamedphantomsFlapSound(
        level: Level,
        entity: Entity,
        sound: SoundEvent,
        source: SoundSource,
        volume: Float,
        pitch: Float,
    ) {
        if ((this as Any) is TamedPhantomEntity) return
        level.playLocalSound(entity, sound, source, volume, pitch)
    }

    @Redirect(
        method = ["tick"],
        at = [
            At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
            ),
        ],
    )
    private fun tamedphantomsFlapParticles(
        level: Level,
        particle: ParticleOptions,
        x: Double,
        y: Double,
        z: Double,
        xSpeed: Double,
        ySpeed: Double,
        zSpeed: Double,
    ) {
        if ((this as Any) is TamedPhantomEntity) return
        level.addParticle(particle, x, y, z, xSpeed, ySpeed, zSpeed)
    }
}
