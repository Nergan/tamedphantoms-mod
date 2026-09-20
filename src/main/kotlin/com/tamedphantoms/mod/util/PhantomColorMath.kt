package com.tamedphantoms.mod.util

import java.awt.Color
import kotlin.math.max

/**
 * Математика перекраски пикселей текстуры фантома. Никаких зависимостей от
 * Minecraft/LWJGL — работает с обычными ARGB-int (формат `java.awt.image.BufferedImage.TYPE_INT_ARGB`,
 * он же формат, который возвращает `NativeImage.getPixelRGBA` после перестановки байт,
 * см. [com.tamedphantoms.mod.client.texture.PhantomTextureProcessor]).
 *
 * Вынесено в отдельный класс без ссылок на классы Minecraft специально для того,
 * чтобы эту логику можно было проверить обычными JUnit-тестами без запуска игры
 * (см. `src/test/kotlin/.../PhantomColorMathTest.kt`).
 */
object PhantomColorMath {

    /** Начало и конец диапазона Hue (0..1), который мы считаем "синим" у фантома. */
    private const val BLUE_HUE_MIN = 0.50f
    private const val BLUE_HUE_MAX = 0.80f
    private const val BLUE_MIN_SATURATION = 0.08f

    /** Насколько сильно затемняем яркость синих пикселей (0 = не трогаем, 1 = делаем чёрным). */
    private const val BLUE_DARKEN_STRENGTH = 0.62f
    private const val BLUE_DESATURATE_FACTOR = 0.55f

    /** Ярко-зелёный, почти лаймовый — лучше читается на светящемся слое. */
    private const val EYE_TARGET_HUE = 0.34f
    /** Ярко-жёлтый для освобождённого фантома. */
    private const val EYE_YELLOW_HUE = 0.13f
    private const val EYE_MIN_SATURATION = 0.95f
    private const val EYE_GLOW_BRIGHTNESS = 1.0f
    /**
     * Кровь для слоя глаз. Тот же оттенок, что уже одобрен в игре.
     * Свечение рисуется отдельной более тёмной текстурой, не более ярким RGB.
     */
    private const val BLOOD_R = 0x5C
    private const val BLOOD_G = 0x08
    private const val BLOOD_B = 0x10
    /** Темнее крови: несколько аддитивных проходов дают «неон», не уходя в алый. */
    private const val BLOOD_GLOW_R = 0x28
    private const val BLOOD_GLOW_G = 0x04
    private const val BLOOD_GLOW_B = 0x08

    /**
     * Перекрашивает один пиксель основной текстуры тела: синевато-серые участки
     * уходят в более тёмный/чёрный, всё остальное остаётся как было.
     *
     * @param argb исходный пиксель в формате 0xAARRGGBB
     * @return новый пиксель в том же формате
     */
    fun recolorBodyPixel(argb: Int): Int {
        val alpha = (argb ushr 24) and 0xFF
        if (alpha == 0) return argb // прозрачные пиксели не трогаем

        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF

        val hsb = FloatArray(3)
        Color.RGBtoHSB(r, g, b, hsb)
        val (hue, sat, bri) = Triple(hsb[0], hsb[1], hsb[2])

        val isBlueish = hue in BLUE_HUE_MIN..BLUE_HUE_MAX && sat >= BLUE_MIN_SATURATION
        if (!isBlueish) return argb

        val newBrightness = (bri * (1f - BLUE_DARKEN_STRENGTH)).coerceIn(0f, 1f)
        val newSaturation = (sat * BLUE_DESATURATE_FACTOR).coerceIn(0f, 1f)
        val newRgb = Color.HSBtoRGB(hue, newSaturation, newBrightness)

        return (alpha shl 24) or (newRgb and 0x00FFFFFF)
    }

    /**
     * Перекрашивает один пиксель текстуры глаз (`phantom_eyes.png`) в более яркий,
     * насыщенный, светящийся зелёный. Непрозрачные исходные пиксели этой текстуры —
     * это и есть глаза (у ванильного фантома они уже рендерятся отдельным
     * "светящимся" слоем поверх тела, см. README), поэтому никакой отдельной
     * логики "распознавания, где именно глаза" не требуется.
     */
    fun recolorEyePixel(argb: Int): Int = recolorEye(argb, EYE_TARGET_HUE, EYE_GLOW_BRIGHTNESS)

    /** Глаза освобождённого фантома — ярко-жёлтые. */
    fun recolorEyePixelYellow(argb: Int): Int = recolorEye(argb, EYE_YELLOW_HUE, EYE_GLOW_BRIGHTNESS)

    /** Кроваво-красные глаза дикого фантома и режима самозащиты. */
    fun recolorEyePixelRed(argb: Int): Int = recolorEyeRgb(argb, BLOOD_R, BLOOD_G, BLOOD_B)

    /** Внешнее свечение крови — темнее [recolorEyePixelRed], для многослойного RenderType.eyes. */
    fun recolorEyePixelRedGlow(argb: Int): Int = recolorEyeRgb(argb, BLOOD_GLOW_R, BLOOD_GLOW_G, BLOOD_GLOW_B)

    private fun recolorEyeRgb(argb: Int, red: Int, green: Int, blue: Int): Int {
        val alpha = (argb ushr 24) and 0xFF
        if (alpha == 0) return argb
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }

    private fun recolorEye(argb: Int, hue: Float, brightness: Float): Int {
        val alpha = (argb ushr 24) and 0xFF
        if (alpha == 0) return argb

        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF

        val hsb = FloatArray(3)
        Color.RGBtoHSB(r, g, b, hsb)
        val saturation = max(hsb[1], EYE_MIN_SATURATION)
        val newRgb = Color.HSBtoRGB(hue, saturation, brightness)

        return (alpha shl 24) or (newRgb and 0x00FFFFFF)
    }
}
