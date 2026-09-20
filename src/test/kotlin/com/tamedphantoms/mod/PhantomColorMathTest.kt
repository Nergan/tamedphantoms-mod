package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomColorMath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.awt.Color

class PhantomColorMathTest {

    private fun argb(a: Int, r: Int, g: Int, b: Int): Int =
        (a shl 24) or (r shl 16) or (g shl 8) or b

    @Test
    @DisplayName("Прозрачные пиксели не трогаем — ни в теле, ни в глазах")
    fun transparentPixelsAreUntouched() {
        val transparent = argb(0, 120, 130, 200)
        assertEquals(transparent, PhantomColorMath.recolorBodyPixel(transparent))
        assertEquals(transparent, PhantomColorMath.recolorEyePixel(transparent))
    }

    @Test
    @DisplayName("Явно синий пиксель тела становится заметно темнее")
    fun bluePixelGetsDarker() {
        // Насыщенный сине-голубой (hue ~ 0.58) — попадает в синий диапазон.
        val blueRgb = Color.HSBtoRGB(0.58f, 0.6f, 0.8f) and 0x00FFFFFF
        val original = (0xFF shl 24) or blueRgb
        val recolored = PhantomColorMath.recolorBodyPixel(original)

        val hsbOriginal = FloatArray(3)
        Color.RGBtoHSB((original ushr 16) and 0xFF, (original ushr 8) and 0xFF, original and 0xFF, hsbOriginal)
        val hsbNew = FloatArray(3)
        Color.RGBtoHSB((recolored ushr 16) and 0xFF, (recolored ushr 8) and 0xFF, recolored and 0xFF, hsbNew)

        assertTrue(hsbNew[2] < hsbOriginal[2], "Яркость синего пикселя должна снизиться")
    }

    @Test
    @DisplayName("Явно красный (не синий) пиксель тела не меняется")
    fun nonBluePixelIsUnchanged() {
        val red = argb(255, 200, 40, 30)
        assertEquals(red, PhantomColorMath.recolorBodyPixel(red))
    }

    @Test
    @DisplayName("Альфа-канал никогда не меняется при перекраске тела")
    fun alphaIsPreservedOnBody() {
        val original = argb(137, 90, 100, 180)
        val recolored = PhantomColorMath.recolorBodyPixel(original)
        assertEquals(137, (recolored ushr 24) and 0xFF)
    }

    @Test
    @DisplayName("Пиксель глаза после перекраски становится зелёным и ярким")
    fun eyePixelBecomesBrightGreen() {
        val originalEye = argb(255, 210, 210, 120) // блёкло-жёлтый, как у ряда мобов с глазами
        val recolored = PhantomColorMath.recolorEyePixel(originalEye)

        val hsb = FloatArray(3)
        Color.RGBtoHSB((recolored ushr 16) and 0xFF, (recolored ushr 8) and 0xFF, recolored and 0xFF, hsb)

        assertTrue(hsb[0] in 0.28f..0.38f, "Оттенок должен стать зелёным, был: ${hsb[0]}")
        assertTrue(hsb[2] >= 0.99f, "Яркость глаза должна быть максимальной")
        assertEquals(255, (recolored ushr 24) and 0xFF)
    }

    @Test
    @DisplayName("Пиксель глаза освобождённого фантома становится ярко-жёлтым")
    fun releasedEyePixelBecomesBrightYellow() {
        val originalEye = argb(255, 210, 210, 120)
        val recolored = PhantomColorMath.recolorEyePixelYellow(originalEye)

        val hsb = FloatArray(3)
        Color.RGBtoHSB((recolored ushr 16) and 0xFF, (recolored ushr 8) and 0xFF, recolored and 0xFF, hsb)

        assertTrue(hsb[0] in 0.08f..0.18f, "Оттенок должен стать жёлтым, был: ${hsb[0]}")
        assertTrue(hsb[2] >= 0.99f, "Яркость глаза должна быть максимальной")
        assertEquals(255, (recolored ushr 24) and 0xFF)
    }

    @Test
    @DisplayName("Пиксель глаза в режиме самозащиты становится кроваво-красным, не алым")
    fun defendingEyePixelBecomesBloodRed() {
        val originalEye = argb(255, 210, 210, 120)
        val recolored = PhantomColorMath.recolorEyePixelRed(originalEye)
        val r = (recolored ushr 16) and 0xFF
        val g = (recolored ushr 8) and 0xFF
        val b = recolored and 0xFF

        val hsb = FloatArray(3)
        Color.RGBtoHSB(r, g, b, hsb)

        assertTrue(r > g * 4 && r > b * 3, "Кровь должна быть красной, а не оранжевой: r=$r g=$g b=$b")
        assertTrue(g < 20 && b < 28, "Зелёный и синий должны остаться почти чёрными, были g=$g b=$b")
        assertTrue(hsb[2] in 0.20f..0.52f, "Кровавый красный темнее алого, был: ${hsb[2]}")
        assertEquals(255, (recolored ushr 24) and 0xFF)
    }
}
