package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource

/**
 * Ветер элитр верхом на скорости выше крейсерской, в том числе в пике.
 * Громкость и высота следуют за скоростью, а при торможении звук затихает
 * примерно за полторы секунды, под водой — быстрее, но тоже без обрыва.
 */
object PhantomDiveWind {

    private val active = HashMap<Int, PhantomWindSound>()

    fun update(phantom: TamedPhantomEntity) {
        val id = phantom.id
        val current = active[id]
        if (current != null && current.isStopped) {
            active.remove(id)
        }
        if (phantom.windStrength() > 0f && active[id] == null) {
            val sound = PhantomWindSound(phantom)
            active[id] = sound
            Minecraft.getInstance().soundManager.play(sound)
        }
    }
}

private class PhantomWindSound(private val phantom: TamedPhantomEntity) : AbstractTickableSoundInstance(
    SoundEvents.ELYTRA_FLYING,
    SoundSource.PLAYERS,
    phantom.random,
) {
    init {
        this.looping = true
        this.volume = 0.01f
        this.pitch = 0.9f
        this.x = phantom.x
        this.y = phantom.y
        this.z = phantom.z
    }

    override fun canStartSilent(): Boolean = true

    override fun tick() {
        if (phantom.isRemoved) {
            this.stop()
            return
        }
        this.x = phantom.x
        this.y = phantom.y
        this.z = phantom.z
        val strength = phantom.windStrength()
        val target = strength * MAX_VOLUME
        val rate = when {
            target > this.volume -> RISE
            phantom.isUnderWater -> SINK_WATER
            else -> SINK
        }
        this.volume = if (target > this.volume) {
            (this.volume + rate).coerceAtMost(target)
        } else {
            (this.volume - rate).coerceAtLeast(target)
        }
        this.pitch = 0.85f + 0.35f * strength
        if (target <= 0f && this.volume <= 0.005f) {
            this.stop()
        }
    }

    private companion object {
        const val MAX_VOLUME = 0.75f
        const val RISE = 0.05f
        const val SINK = 0.025f
        const val SINK_WATER = 0.08f
    }
}
