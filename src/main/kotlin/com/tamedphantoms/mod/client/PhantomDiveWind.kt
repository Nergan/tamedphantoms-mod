package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource

/** Петля ветра элитр, пока пикирование ещё идёт. Под водой и после выравнивания обрывается. */
object PhantomDiveWind {

    private val active = HashMap<Int, PhantomDiveWindSound>()

    fun update(phantom: TamedPhantomEntity) {
        val id = phantom.id
        val current = active[id]
        if (phantom.diveWindActive()) {
            if (current == null || current.isStopped) {
                val sound = PhantomDiveWindSound(phantom)
                active[id] = sound
                Minecraft.getInstance().soundManager.play(sound)
            }
        } else if (current != null) {
            current.end()
            active.remove(id)
        }
    }
}

private class PhantomDiveWindSound(private val phantom: TamedPhantomEntity) : AbstractTickableSoundInstance(
    SoundEvents.ELYTRA_FLYING,
    SoundSource.PLAYERS,
    phantom.random,
) {
    init {
        this.looping = true
        this.volume = 0.15f
        this.pitch = 0.95f
        this.x = phantom.x
        this.y = phantom.y
        this.z = phantom.z
    }

    override fun tick() {
        if (phantom.isRemoved || !phantom.diveWindActive()) {
            this.stop()
            return
        }
        this.x = phantom.x
        this.y = phantom.y
        this.z = phantom.z
        val drop = (phantom.yo - phantom.y).toFloat()
        this.volume = (drop * 1.15f).coerceIn(0.12f, 0.72f)
    }

    fun end() {
        this.stop()
    }
}
