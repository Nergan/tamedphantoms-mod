package com.tamedphantoms.mod.client.texture

import com.mojang.blaze3d.platform.NativeImage
import com.tamedphantoms.mod.TamedPhantomsMod
import com.tamedphantoms.mod.util.PhantomColorMath
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.ResourceLocation

/**
 * Генерирует перекрашенные варианты ванильных текстур фантома ЦЕЛИКОМ в
 * рантайме, без единого нового файла-ассета:
 *  - `textures/entity/phantom.png` (тело) читается из ресурсов игры,
 *    сине-серые пиксели затемняются в сторону чёрного;
 *  - `textures/entity/phantom_eyes.png` (штатный "светящийся" слой глаз,
 *    которым уже пользуется ванильный [net.minecraft.client.renderer.entity.PhantomRenderer]
 *    через `EyesLayer`/`RenderType.eyes`) — становится ярче и зеленее.
 *
 * Результат кэшируется в [DynamicTexture] под собственным ResourceLocation
 * мода. Тело перекрашивается у ручного фантома; глаза — у ручного,
 * освобождённого и дикого (красные).
 *
 * Всё обёрнуто в try/catch: если по какой-то причине (другой resourcepack,
 * будущее изменение текстуры и т.п.) обработка не удалась — мод просто
 * молча использует ванильную текстуру вместо перекрашенной, а не крашит игру.
 */
object PhantomTextureProcessor {

    private val VANILLA_BODY = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/phantom.png")
    private val EYE_SOURCE = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "textures/entity/phantom_eyes_base.png")
    private val VANILLA_EYES = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/phantom_eyes.png")

    private val RECOLORED_BODY = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "generated/tamed_phantom_body")
    private val RECOLORED_EYES = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "generated/tamed_phantom_eyes")
    private val YELLOW_EYES = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "generated/released_phantom_eyes")
    private val RED_EYES = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "generated/angry_phantom_eyes")
    private val RED_EYES_GLOW = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "generated/angry_phantom_eyes_glow")
    private val SADDLE_TEX = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "generated/tamed_phantom_saddle")

    private var bodyReady = false
    private var eyesReady = false
    private var yellowEyesReady = false
    private var redEyesReady = false
    private var redEyesGlowReady = false
    private var saddleReady = false

    /** ResourceLocation основной (перекрашенной) текстуры тела ручного фантома. */
    fun bodyTexture(): ResourceLocation {
        ensureBody()
        return if (bodyReady) RECOLORED_BODY else VANILLA_BODY
    }

    fun resetGeneratedTextures() {
        bodyReady = false
        eyesReady = false
        yellowEyesReady = false
        redEyesReady = false
        redEyesGlowReady = false
        saddleReady = false
    }

    fun prepareEyeTextures() {
        ensureEyes()
        ensureYellowEyes()
        ensureRedEyes()
        ensureRedEyesGlow()
        suppressVanillaEyes()
    }

    fun redEyesGlowTexture(): ResourceLocation? {
        ensureRedEyesGlow()
        return if (redEyesGlowReady) RED_EYES_GLOW else null
    }

    /**
     * Ванильный EyesLayer рисует `minecraft:textures/entity/phantom_eyes.png`
     * аддитивно (жёлто-зелёный). Поверх наших красных глаз это даёт алый
     * и переход через оранжевый. Подменяем текстуру пустой, слой ничего не добавляет.
     */
    private fun suppressVanillaEyes() {
        try {
            val minecraft = Minecraft.getInstance()
            val sizeSource = minecraft.resourceManager.getResource(EYE_SOURCE).let { first ->
                if (first.isPresent) first else minecraft.resourceManager.getResource(VANILLA_EYES)
            }
            val (width, height) = if (sizeSource.isPresent) {
                sizeSource.get().open().use { stream ->
                    NativeImage.read(stream).use { image -> image.width to image.height }
                }
            } else {
                64 to 64
            }
            val blank = NativeImage(NativeImage.Format.RGBA, width, height, true)
            minecraft.textureManager.register(VANILLA_EYES, DynamicTexture(blank))
        } catch (t: Throwable) {
            TamedPhantomsMod.LOGGER.warn("Не удалось отключить ванильный слой глаз фантома.", t)
        }
    }

    /** Зелёные / жёлтые глаза или красные, пока фантом обороняется. */
    fun eyesTexture(tamed: Boolean, defending: Boolean = false): ResourceLocation {
        if (defending) {
            ensureRedEyes()
            return if (redEyesReady) RED_EYES else VANILLA_EYES
        }
        return if (tamed) {
            ensureEyes()
            if (eyesReady) RECOLORED_EYES else VANILLA_EYES
        } else {
            ensureYellowEyes()
            if (yellowEyesReady) YELLOW_EYES else VANILLA_EYES
        }
    }

    fun saddleTexture(): ResourceLocation {
        if (!saddleReady) {
            saddleReady = tryGenerateSaddle(SADDLE_TEX)
        }
        return SADDLE_TEX
    }

    private fun tryGenerateSaddle(destination: ResourceLocation): Boolean {
        return try {
            val width = 32
            val height = 16
            val image = NativeImage(NativeImage.Format.RGBA, width, height, false)
            val leather = 0xFF6B4226.toInt()
            val dark = 0xFF3A2416.toInt()
            val highlight = 0xFF8B5A2B.toInt()
            val stitch = 0xFF2A1810.toInt()
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val border = x == 0 || y == 0 || x == width - 1 || y == height - 1 || x == 15
                    val stitchDot = (x + y) % 5 == 0 && !border
                    val mid = x in 8..12 && y in 3..6
                    val color = when {
                        border -> dark
                        stitchDot -> stitch
                        mid -> highlight
                        else -> leather
                    }
                    image.setPixelRGBA(x, y, argbToNative(color))
                }
            }
            Minecraft.getInstance().textureManager.register(destination, DynamicTexture(image))
            true
        } catch (t: Throwable) {
            TamedPhantomsMod.LOGGER.warn("Не удалось сгенерировать текстуру седла фантома.", t)
            false
        }
    }

    private fun ensureBody() {
        if (bodyReady) return
        bodyReady = tryGenerate(VANILLA_BODY, RECOLORED_BODY) { argb -> PhantomColorMath.recolorBodyPixel(argb) }
    }

    private fun ensureEyes() {
        if (eyesReady) return
        eyesReady = tryGenerateFromEyeSource(RECOLORED_EYES) { argb -> PhantomColorMath.recolorEyePixel(argb) }
    }

    private fun ensureYellowEyes() {
        if (yellowEyesReady) return
        yellowEyesReady = tryGenerateFromEyeSource(YELLOW_EYES) { argb -> PhantomColorMath.recolorEyePixelYellow(argb) }
    }

    private fun ensureRedEyes() {
        if (redEyesReady) return
        redEyesReady = tryGenerateFromEyeSource(RED_EYES) { argb -> PhantomColorMath.recolorEyePixelRed(argb) }
    }

    private fun ensureRedEyesGlow() {
        if (redEyesGlowReady) return
        redEyesGlowReady = tryGenerateFromEyeSource(RED_EYES_GLOW) { argb -> PhantomColorMath.recolorEyePixelRedGlow(argb) }
    }

    private fun tryGenerateFromEyeSource(destination: ResourceLocation, transform: (Int) -> Int): Boolean {
        val custom = Minecraft.getInstance().resourceManager.getResource(EYE_SOURCE)
        if (custom.isPresent && tryGenerate(EYE_SOURCE, destination, transform)) {
            return true
        }
        return tryGenerate(VANILLA_EYES, destination, transform)
    }

    private fun tryGenerate(
        source: ResourceLocation,
        destination: ResourceLocation,
        transform: (Int) -> Int,
    ): Boolean {
        return try {
            val minecraft = Minecraft.getInstance()
            val resourceOpt = minecraft.resourceManager.getResource(source)
            if (resourceOpt.isEmpty) {
                TamedPhantomsMod.LOGGER.warn("Ванильная текстура {} не найдена — перекраска пропущена.", source)
                return false
            }
            resourceOpt.get().open().use { stream ->
                val image = NativeImage.read(stream)
                recolor(image, transform)
                minecraft.textureManager.register(destination, DynamicTexture(image))
            }
            true
        } catch (t: Throwable) {
            TamedPhantomsMod.LOGGER.warn(
                "Не удалось сгенерировать перекрашенную текстуру ручного фантома из {} — " +
                    "будет использована ванильная текстура без перекраски.",
                source,
                t,
            )
            false
        }
    }

    private fun recolor(image: NativeImage, transform: (Int) -> Int) {
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val native = image.getPixelRGBA(x, y)
                val argb = nativeToArgb(native)
                val newArgb = transform(argb)
                image.setPixelRGBA(x, y, argbToNative(newArgb))
            }
        }
    }

    /** NativeImage хранит пиксели в порядке байт ABGR (OpenGL), а не ARGB. */
    private fun nativeToArgb(native: Int): Int {
        val a = (native ushr 24) and 0xFF
        val b = (native ushr 16) and 0xFF
        val g = (native ushr 8) and 0xFF
        val r = native and 0xFF
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun argbToNative(argb: Int): Int {
        val a = (argb ushr 24) and 0xFF
        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF
        return (a shl 24) or (b shl 16) or (g shl 8) or r
    }
}
