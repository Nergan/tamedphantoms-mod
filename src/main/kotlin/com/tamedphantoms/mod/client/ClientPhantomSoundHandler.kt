package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.config.ClientConfig
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.Sound
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.client.sounds.SoundManager
import net.minecraft.client.sounds.WeighedSoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.phys.AABB
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent

/**
 * Громкость приручённого фантома — клиентская настройка.
 *
 * [net.minecraft.world.entity.Entity.playSound] играет звук по координатам,
 * а в сингле клиент слышит его из сетевого пакета. [PlaySoundEvent] —
 * единственная точка, через которую проходит уже готовый звук.
 */
object ClientPhantomSoundHandler {

    private const val MATCH_RANGE = 2.5

    @SubscribeEvent
    fun onPlaySound(event: PlaySoundEvent) {
        val sound = event.sound ?: return
        if (sound.source != SoundSource.HOSTILE) return
        if (!isFromTamedPhantom(sound)) return

        val mul = ClientConfig.CONFIG.tamedSoundVolume.get().toFloat().coerceIn(0.0f, 1.0f)
        when {
            mul <= 0.0f -> event.setSound(null)
            mul < 1.0f -> event.setSound(VolumeScaledSound(sound, mul))
        }
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
}
