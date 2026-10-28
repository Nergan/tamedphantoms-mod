package com.tamedphantoms.mod.client

import com.mojang.blaze3d.audio.SoundBuffer
import com.tamedphantoms.mod.util.VanillaPhantomSoundPaths
import net.minecraft.client.Minecraft
import net.minecraft.client.sounds.AudioStream
import net.minecraft.client.sounds.JOrbisAudioStream
import net.minecraft.client.sounds.LoopingAudioStream
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.Util
import java.io.InputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Файл `tamedphantoms:sounds/vanilla/mob/phantom/` читается из ванильного набора,
 * а не из общего менеджера, куда другой мод кладёт свою замену.
 */
object VanillaPhantomBuffers {
    private val cache = ConcurrentHashMap<ResourceLocation, CompletableFuture<SoundBuffer>>()

    fun completeBuffer(location: ResourceLocation): CompletableFuture<SoundBuffer>? {
        val vanilla = vanillaFile(location) ?: return null
        cache[vanilla]?.let { return it }
        val future = CompletableFuture.supplyAsync({ decode(vanilla) }, Util.nonCriticalIoPool())
        cache[vanilla] = future
        future.whenComplete { _, error ->
            if (error != null) cache.remove(vanilla, future)
        }
        return future
    }

    /** Номер буфера OpenAL живёт только до перезапуска звукового движка. */
    fun clear() {
        cache.values.forEach { future -> future.thenAccept { it.discardAlBuffer() } }
        cache.clear()
    }

    fun stream(location: ResourceLocation, looping: Boolean): CompletableFuture<AudioStream>? {
        val vanilla = vanillaFile(location) ?: return null
        return CompletableFuture.supplyAsync({
            val input = openVanilla(vanilla) ?: error("Ванильный звук не найден: $vanilla")
            if (looping) {
                LoopingAudioStream({ stream -> JOrbisAudioStream(stream) }, input)
            } else {
                JOrbisAudioStream(input)
            }
        }, Util.nonCriticalIoPool())
    }

    private fun vanillaFile(location: ResourceLocation): ResourceLocation? {
        val path = VanillaPhantomSoundPaths.bufferToVanillaFile(location.namespace, location.path) ?: return null
        return ResourceLocation.parse(path)
    }

    private fun decode(location: ResourceLocation): SoundBuffer {
        val input = openVanilla(location) ?: error("Ванильный звук не найден: $location")
        input.use { stream ->
            JOrbisAudioStream(stream).use { audio ->
                return SoundBuffer(audio.readAll(), audio.format)
            }
        }
    }

    private fun openVanilla(location: ResourceLocation): InputStream? {
        val supplier = Minecraft.getInstance().vanillaPackResources.getResource(PackType.CLIENT_RESOURCES, location)
            ?: return null
        return supplier.get()
    }
}
