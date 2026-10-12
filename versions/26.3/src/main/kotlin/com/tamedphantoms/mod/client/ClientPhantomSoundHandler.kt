package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.config.ClientConfig
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.sounds.SoundSource
import net.minecraft.world.phys.AABB

object ClientPhantomSoundHandler {

    private const val MATCH_RANGE = 2.5

    private val rawVolume: java.lang.reflect.Field? = declared("volume")
    private val rawPitch: java.lang.reflect.Field? = declared("pitch")

    fun adjust(sound: SoundInstance?): SoundInstance? {
        if (sound == null) return sound
        if (sound.source != SoundSource.HOSTILE) return sound
        val volume = raw(sound, rawVolume)
        val pitch = raw(sound, rawPitch)
        if (volume != null && pitch != null && volume >= 2.0f && pitch <= 0.7f) return sound
        if (!isFromTamedPhantom(sound)) return sound

        val mul = ClientConfig.tamedSoundVolume().toFloat()
        if (mul <= 0.0f) return null
        if (mul < 1.0f && volume != null && rawVolume != null) {
            rawVolume.setFloat(sound, volume * mul)
        }
        return sound
    }

    private fun isFromTamedPhantom(sound: SoundInstance): Boolean {
        val level = Minecraft.getInstance().level ?: return false
        val box = AABB(
            sound.x - MATCH_RANGE,
            sound.y - MATCH_RANGE,
            sound.z - MATCH_RANGE,
            sound.x + MATCH_RANGE,
            sound.y + MATCH_RANGE,
            sound.z + MATCH_RANGE,
        )
        return level.getEntitiesOfClass(TamedPhantomEntity::class.java, box) { phantom ->
            phantom.tamed && phantom.isAlive
        }.isNotEmpty()
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
        } catch (_: ReflectiveOperationException) {
            null
        }
    }
}
