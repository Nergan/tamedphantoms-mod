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
     * Кровь для одного прохода слоя глаз. В 26.3 `RenderType.eyes` — обычная
     * прозрачность, не сложение, поэтому тёмный RGB выглядит чёрным.
     * Это уже итоговый цвет, а не заготовка под несколько аддитивных проходов.
     */
    private const val BLOOD_R = 0xD0
    private const val BLOOD_G = 0x12
    private const val BLOOD_B = 0x24
    /** Темнее крови: запас для ореола, если слой всё же складывает цвет. */
    private const val BLOOD_GLOW_R = 0x68
    private const val BLOOD_GLOW_G = 0x08
    private const val BLOOD_GLOW_B = 0x10

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

    /**
     * Палитра краски: тень, середина и свет шкуры со сдвигом оттенка, светлые брюхо и кости,
     * акцент для лиловых кромок. У тёмных красок на крыльях звёзды, как ночное небо.
     */
    private class DyeStyle(
        val shadow: Int,
        val mid: Int,
        val light: Int,
        val belly: Int,
        val accent: Int,
        val stars: Boolean = false,
    )

    /**
     * По номеру `DyeColor`: white, orange, magenta, light_blue, yellow, lime, pink, gray,
     * light_gray, cyan, purple, blue, brown, green, red, black.
     */
    private val STYLES = arrayOf(
        DyeStyle(0x66748F, 0xB8C3D6, 0xF4F7FC, 0xFFF6E8, 0x9AA8C2),
        DyeStyle(0x6E2410, 0xD8641C, 0xFFB35C, 0xFFE3B8, 0xA83A14),
        DyeStyle(0x4E0E48, 0xB8329F, 0xF28ADD, 0xFBD7F2, 0x7E2290),
        DyeStyle(0x24527E, 0x58ACE0, 0xB2E4FF, 0xE8F8FF, 0x3A7DB8),
        DyeStyle(0x6E4E0C, 0xDDB52C, 0xFFF27C, 0xFFF9DC, 0xB07E16),
        DyeStyle(0x2A5410, 0x74C02A, 0xCBF47C, 0xF1FFD6, 0x46901C),
        DyeStyle(0x7E3252, 0xE985AC, 0xFFD2E3, 0xFFF2F7, 0xB8527C),
        DyeStyle(0x24262C, 0x535862, 0x8E949F, 0xC8C2B6, 0x3E424B),
        DyeStyle(0x5E636B, 0x9EA3AB, 0xD6D9DE, 0xF0ECE4, 0x7D828B),
        DyeStyle(0x0C464C, 0x1C9098, 0x6ADCDC, 0xD8FAF6, 0x13707A),
        DyeStyle(0x26104A, 0x6430A6, 0xAE78E8, 0xE9DCFA, 0x421E78, stars = true),
        DyeStyle(0x0E1850, 0x2A44AE, 0x6E8AF0, 0xD8E2FF, 0x1A2C88, stars = true),
        DyeStyle(0x331E10, 0x704628, 0xB0845A, 0xEAD5B4, 0x52321C),
        DyeStyle(0x16320E, 0x3A7226, 0x80B456, 0xDEEBC6, 0x28521A),
        DyeStyle(0x440C0E, 0xA82226, 0xEE5E56, 0xFFDCCF, 0x701216),
        DyeStyle(0x08080C, 0x1A1A24, 0x383850, 0x8E8698, 0x2A2640, stars = true),
    )

    private const val STAR = 0xE6E2FF

    /** Сторона ванильной развёртки фантома, к ней приводятся координаты пикселя. */
    const val UV_GRID = 64

    /**
     * Пиксель тела в краске [dye] (номер `DyeColor`). Шкура идёт по палитре от тени к свету,
     * брюхо и кости светлеют в тон, кромки берут акцент. [x], [y] — место на развёртке [UV_GRID]:
     * концы крыльев светлее, у тёмных красок на крыльях звёзды. Глаза и прозрачность не трогаются.
     */
    fun dyeBodyPixel(argb: Int, dye: Int, x: Int, y: Int): Int {
        val alpha = (argb ushr 24) and 0xFF
        if (alpha == 0) return argb
        val style = STYLES.getOrNull(dye) ?: return argb
        val r = (argb ushr 16) and 0xFF
        val g = (argb ushr 8) and 0xFF
        val b = argb and 0xFF
        val hsb = Color.RGBtoHSB(r, g, b, null)
        val hue = hsb[0]
        val sat = hsb[1]
        val lum = ((0.299 * r + 0.587 * g + 0.114 * b) / 255.0).toFloat()
        if (hue in 0.22f..0.45f && sat > 0.5f && hsb[2] > 0.4f) return argb
        val rgb = when {
            lum < 0.07f -> mix(0x000000, style.shadow, 0.7f)
            sat < 0.2f && lum > 0.62f -> mix(style.mid, style.belly, 0.7f + (lum - 0.62f) * 0.8f)
            hue in 0.03f..0.17f && sat >= 0.12f ->
                mix(style.mid, style.belly, 0.45f + ((lum - 0.35f) / 0.45f).coerceIn(0f, 1f) * 0.55f)
            (hue >= 0.8f || hue < 0.03f) && sat >= 0.12f ->
                ramp(style.shadow, style.accent, style.light, 0.35f + ((lum - 0.2f) / 0.45f).coerceIn(0f, 1f) * 0.45f)
            style.stars && onWing(x, y) && isStar(x, y) -> STAR
            else -> ramp(style.shadow, style.mid, style.light, (lum - 0.1f) / 0.42f + if (onWingTip(x, y)) 0.12f else 0f)
        }
        return (alpha shl 24) or rgb
    }

    /** Седло: кожа, кант, блик и нитки. Без краски — обычная кожа, в краске — её палитра со светлыми нитками. */
    fun saddleColors(dye: Int): IntArray {
        val style = STYLES.getOrNull(dye)
            ?: return intArrayOf(0xFF6B4226.toInt(), 0xFF3A2416.toInt(), 0xFF8B5A2B.toInt(), 0xFF2A1810.toInt())
        return intArrayOf(
            opaque(style.mid),
            opaque(style.shadow),
            opaque(mix(style.mid, style.light, 0.5f)),
            opaque(style.belly),
        )
    }

    private fun onWingTip(x: Int, y: Int): Boolean = x in 16 until 60 && y in 24 until 34

    private fun onWing(x: Int, y: Int): Boolean = (x in 23 until 53 && y in 12 until 23) || onWingTip(x, y)

    private fun isStar(x: Int, y: Int): Boolean = (((x * 73856093) xor (y * 19349663)) and 0x7FFFFFFF) % 29 == 0

    private fun opaque(rgb: Int): Int = (0xFF shl 24) or (rgb and 0xFFFFFF)

    private fun ramp(shadow: Int, mid: Int, light: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        return if (k < 0.5f) mix(shadow, mid, k * 2f) else mix(mid, light, (k - 0.5f) * 2f)
    }

    private fun mix(from: Int, to: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        fun channel(shift: Int): Int {
            val a = (from ushr shift) and 0xFF
            val b = (to ushr shift) and 0xFF
            return Math.round(a + (b - a) * k).coerceIn(0, 255) shl shift
        }
        return channel(16) or channel(8) or channel(0)
    }

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
