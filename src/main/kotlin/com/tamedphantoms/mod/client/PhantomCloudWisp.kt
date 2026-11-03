package com.tamedphantoms.mod.client

import com.mojang.blaze3d.platform.NativeImage
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.mixin.CloudTickAccessor
import net.minecraft.client.CloudStatus
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Ванильные облака — клиентская картинка, не блоки. Слой берётся из той же высоты и той же
 * `textures/environment/clouds.png`, что рисует [net.minecraft.client.renderer.LevelRenderer]:
 * клетка 12 блоков, сдвиг по X на `ticks * 0.03`, низ на `cloudHeight + 0.33`, у красивых
 * облаков толщина 4 блока. Пар и шелест шерсти — на влёте и на вылете, и если слой пробит за один тик.
 */
object PhantomCloudWisp {

    private const val CELL = 12.0
    private const val SCROLL_PER_TICK = 0.03
    private const val Y_BIAS = 0.33
    private const val Z_BIAS = 0.33
    private const val FANCY_THICKNESS = 4.0
    private const val FAST_THICKNESS = 1.0
    private const val ALPHA_CUT = 16
    private const val COOLDOWN = 4
    private const val MAX_STEP = 48.0
    private const val PUFFS = 12

    private class Track(var inside: Boolean = false, var ready: Boolean = false, var cooldown: Int = 0)

    private val tracks = WeakHashMap<TamedPhantomEntity, Track>()
    private var image: NativeImage? = null
    private var failed = false

    fun forget() {
        image?.close()
        image = null
        failed = false
    }

    fun afterClientTick(phantom: TamedPhantomEntity) {
        val minecraft = Minecraft.getInstance()
        val level = phantom.level() as? ClientLevel ?: return
        val status = minecraft.options.cloudsType
        if (status == CloudStatus.OFF) return
        val height = level.effects().cloudHeight
        if (height.isNaN()) return
        val clouds = texture() ?: return

        val bottom = height + Y_BIAS
        val top = bottom + if (status == CloudStatus.FANCY) FANCY_THICKNESS else FAST_THICKNESS
        val scroll = (minecraft.levelRenderer as CloudTickAccessor).`tamedphantoms$ticks`() * SCROLL_PER_TICK
        val y = phantom.y + phantom.bbHeight * 0.5
        val py = phantom.yOld + phantom.bbHeight * 0.5
        val now = inside(phantom.x, y, phantom.z, bottom, top, scroll, clouds)
        val track = tracks.getOrPut(phantom) { Track() }
        if (track.cooldown > 0) track.cooldown--
        if (!track.ready) {
            track.ready = true
            track.inside = now
            return
        }
        val jumped = phantom.distanceToSqr(phantom.xOld, phantom.yOld, phantom.zOld) > MAX_STEP * MAX_STEP
        val crossed = !jumped && !track.inside && !now &&
            crosses(phantom.xOld, py, phantom.zOld, phantom.x, y, phantom.z, bottom, top, scroll, clouds)
        if (!jumped && track.cooldown <= 0 && (track.inside != now || crossed)) {
            puff(phantom, level)
            track.cooldown = COOLDOWN
        }
        track.inside = now
    }

    private fun inside(
        x: Double,
        y: Double,
        z: Double,
        bottom: Double,
        top: Double,
        scroll: Double,
        clouds: NativeImage,
    ): Boolean {
        if (y < bottom || y > top) return false
        return opaque(x, z, scroll, clouds)
    }

    private fun crosses(
        x0: Double,
        y0: Double,
        z0: Double,
        x1: Double,
        y1: Double,
        z1: Double,
        bottom: Double,
        top: Double,
        scroll: Double,
        clouds: NativeImage,
    ): Boolean {
        val lo = min(y0, y1)
        val hi = max(y0, y1)
        if (hi < bottom || lo > top) return false
        val sampleY = (max(lo, bottom) + min(hi, top)) * 0.5
        val dy = y1 - y0
        val t = if (abs(dy) < 1.0E-6) 0.0 else ((sampleY - y0) / dy).coerceIn(0.0, 1.0)
        return opaque(x0 + (x1 - x0) * t, z0 + (z1 - z0) * t, scroll, clouds)
    }

    private fun opaque(x: Double, z: Double, scroll: Double, clouds: NativeImage): Boolean {
        val width = clouds.width
        val height = clouds.height
        if (width <= 0 || height <= 0) return false
        val texX = Math.floorMod(Mth.floor((x + scroll) / CELL), width)
        val texZ = Math.floorMod(Mth.floor(z / CELL + Z_BIAS), height)
        return (clouds.getLuminanceOrAlpha(texX, texZ).toInt() and 0xFF) >= ALPHA_CUT
    }

    private fun puff(phantom: TamedPhantomEntity, level: ClientLevel) {
        val random = phantom.random
        repeat(PUFFS) {
            val ox = (random.nextDouble() - 0.5) * phantom.bbWidth
            val oy = random.nextDouble() * phantom.bbHeight
            val oz = (random.nextDouble() - 0.5) * phantom.bbWidth
            level.addParticle(
                ParticleTypes.POOF,
                phantom.x + ox,
                phantom.y + oy,
                phantom.z + oz,
                (random.nextDouble() - 0.5) * 0.04,
                0.02,
                (random.nextDouble() - 0.5) * 0.04,
            )
        }
        level.playLocalSound(
            phantom.x,
            phantom.y,
            phantom.z,
            SoundEvents.WOOL_BREAK,
            SoundSource.NEUTRAL,
            0.55f,
            0.9f + random.nextFloat() * 0.15f,
            false,
        )
    }

    private fun texture(): NativeImage? {
        image?.let { return it }
        if (failed) return null
        return try {
            val id = ResourceLocation.withDefaultNamespace("textures/environment/clouds.png")
            val resource = Minecraft.getInstance().resourceManager.getResource(id)
            if (resource.isEmpty) {
                failed = true
                null
            } else {
                resource.get().open().use { NativeImage.read(it) }.also { image = it }
            }
        } catch (_: Exception) {
            failed = true
            null
        }
    }
}
