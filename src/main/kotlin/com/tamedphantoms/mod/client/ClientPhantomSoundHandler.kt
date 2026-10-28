package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.config.ClientConfig
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.VanillaPhantomSoundPaths
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractSoundInstance
import net.minecraft.client.resources.sounds.Sound
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.client.sounds.SoundManager
import net.minecraft.client.sounds.WeighedSoundEvents
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.phys.AABB

/**
 * Громкость приручённого фантома — клиентская настройка.
 *
 * [net.minecraft.world.entity.Entity.playSound] играет звук по координатам,
 * а в сингле клиент слышит его из сетевого пакета. Подмена идёт до resolve(),
 * поэтому громкость читается из сырых полей, а не через [SoundInstance.getVolume].
 */
object ClientPhantomSoundHandler {

    private const val MATCH_RANGE = 2.5

    private val rawVolume: java.lang.reflect.Field? = declared("volume")
    private val rawPitch: java.lang.reflect.Field? = declared("pitch")

    /**
     * @return null, если звук надо отменить; тот же экземпляр, если менять нечего;
     * другой экземпляр — подмена.
     */
    fun adjust(sound: SoundInstance?): SoundInstance? {
        if (sound == null || sound is VolumeScaledSound || sound is VanillaFileSound) return sound
        val phantom = nearestPhantom(sound)
        var next: SoundInstance = sound
        if (phantom != null && isPhantomEvent(sound)) {
            next = VanillaFileSound(sound)
        }
        if (sound.source != SoundSource.HOSTILE) return next
        val volume = raw(sound, rawVolume)
        val pitch = raw(sound, rawPitch)
        if (volume != null && pitch != null && volume >= 2.0f && pitch <= 0.7f) return next
        if (phantom == null || !phantom.tamed) return next

        val mul = ClientConfig.tamedSoundVolume().toFloat()
        return when {
            mul <= 0.0f -> null
            mul < 1.0f -> VolumeScaledSound(next, mul)
            else -> next
        }
    }

    private fun isPhantomEvent(sound: SoundInstance): Boolean {
        val id = sound.location
        return id.namespace == "minecraft" && id.path.startsWith("entity.phantom.")
    }

    /** Ближайший живой фантом. Подмена звука — только если это наша сущность, в том числе освобождённая. */
    private fun nearestPhantom(sound: SoundInstance): TamedPhantomEntity? {
        val level = Minecraft.getInstance().level ?: return null
        val box = AABB(
            sound.x - MATCH_RANGE,
            sound.y - MATCH_RANGE,
            sound.z - MATCH_RANGE,
            sound.x + MATCH_RANGE,
            sound.y + MATCH_RANGE,
            sound.z + MATCH_RANGE,
        )
        val closest = level.getEntitiesOfClass(Phantom::class.java, box) { it.isAlive }
            .minByOrNull { it.distanceToSqr(sound.x, sound.y, sound.z) }
        return closest as? TamedPhantomEntity
    }

    private class VolumeScaledSound(
        private val inner: SoundInstance,
        private val mul: Float,
    ) : SoundInstance {
        override fun getLocation() = inner.location
        override fun resolve(manager: SoundManager): WeighedSoundEvents? = inner.resolve(manager)
        override fun getSound(): Sound = inner.sound
        override fun getSource() = inner.source
        override fun isLooping() = inner.isLooping
        override fun isRelative() = inner.isRelative
        override fun getDelay() = inner.delay
        override fun getVolume() = inner.volume * mul
        override fun getPitch() = inner.pitch
        override fun getX() = inner.x
        override fun getY() = inner.y
        override fun getZ() = inner.z
        override fun getAttenuation() = inner.attenuation
        override fun canStartSilent() = inner.canStartSilent()
        override fun canPlaySound() = inner.canPlaySound()
    }

    /**
     * После resolve подставляет файл из ванильного набора.
     * Дикий фантом этот класс не получает и играет звук чужого мода.
     */
    private class VanillaFileSound(private val inner: SoundInstance) : SoundInstance {
        override fun getLocation() = inner.location
        override fun resolve(manager: SoundManager): WeighedSoundEvents? = inner.resolve(manager)
        override fun getSound(): Sound = redirect(inner.sound)
        override fun getSource() = inner.source
        override fun isLooping() = inner.isLooping
        override fun isRelative() = inner.isRelative
        override fun getDelay() = inner.delay
        override fun getVolume() = inner.volume
        override fun getPitch() = inner.pitch
        override fun getX() = inner.x
        override fun getY() = inner.y
        override fun getZ() = inner.z
        override fun getAttenuation() = inner.attenuation
        override fun canStartSilent() = inner.canStartSilent()
        override fun canPlaySound() = inner.canPlaySound()

        private fun redirect(sound: Sound): Sound {
            val id = VanillaPhantomSoundPaths.redirectedLocation(sound.location.namespace, sound.location.path)
                ?: return sound
            return Sound(
                ResourceLocation.parse(id),
                sound.volume,
                sound.pitch,
                sound.weight,
                sound.type,
                sound.shouldStream(),
                sound.shouldPreload(),
                sound.attenuationDistance,
            )
        }
    }

    private fun declared(name: String): java.lang.reflect.Field? = try {
        AbstractSoundInstance::class.java.getDeclaredField(name).apply { isAccessible = true }
    } catch (_: ReflectiveOperationException) {
        null
    }

    private fun raw(sound: SoundInstance, field: java.lang.reflect.Field?): Float? {
        if (field == null || sound !is AbstractSoundInstance) return null
        return try {
            field.getFloat(sound)
        } catch (_: IllegalAccessException) {
            null
        }
    }
}
