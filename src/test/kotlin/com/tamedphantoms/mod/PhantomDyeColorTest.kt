package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomColorMath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PhantomDyeColorTest {

    private val skin = 0xFF3C4A6E.toInt()
    private val brightSkin = 0xFF6A7EB0.toInt()
    private val belly = 0xFFC8A27A.toInt()
    private val eye = 0xFF22CC22.toInt()

    private val white = 0
    private val blue = 11
    private val red = 14
    private val black = 15

    /** Тело, не крыло: здесь нет ни звёзд, ни посветления концов. */
    private val bodyX = 10
    private val bodyY = 10

    private fun channels(argb: Int) = Triple((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)

    private fun brightness(argb: Int): Int = channels(argb).let { it.first + it.second + it.third }

    @Test
    @DisplayName("Прозрачный пиксель краска не трогает")
    fun transparentStays() {
        assertEquals(0x00123456, PhantomColorMath.dyeBodyPixel(0x00123456, red, bodyX, bodyY))
    }

    @Test
    @DisplayName("Красная краска даёт красный оттенок, светлая шкура остаётся светлее тёмной")
    fun redKeepsPattern() {
        val dark = channels(PhantomColorMath.dyeBodyPixel(skin, red, bodyX, bodyY))
        val light = channels(PhantomColorMath.dyeBodyPixel(brightSkin, red, bodyX, bodyY))
        assertTrue(dark.first > dark.second && dark.first > dark.third, "$dark")
        assertTrue(light.first > dark.first, "$light vs $dark")
    }

    @Test
    @DisplayName("Белая краска светлее чёрной на той же шкуре")
    fun whiteBrighterThanBlack() {
        val whiteSkin = brightness(PhantomColorMath.dyeBodyPixel(skin, white, bodyX, bodyY))
        val blackSkin = brightness(PhantomColorMath.dyeBodyPixel(skin, black, bodyX, bodyY))
        assertTrue(whiteSkin > blackSkin + 240, "$whiteSkin vs $blackSkin")
    }

    @Test
    @DisplayName("Брюхо светлее шкуры той же краски: окраска в два тона, а не заливка")
    fun bellyLighterThanSkin() {
        for (dye in 0..15) {
            val skinShade = brightness(PhantomColorMath.dyeBodyPixel(skin, dye, bodyX, bodyY))
            val bellyShade = brightness(PhantomColorMath.dyeBodyPixel(belly, dye, bodyX, bodyY))
            assertTrue(bellyShade > skinShade, "dye $dye: $bellyShade vs $skinShade")
        }
    }

    @Test
    @DisplayName("Зелёные глаза на текстуре тела краска не трогает")
    fun eyesStay() {
        assertEquals(eye, PhantomColorMath.dyeBodyPixel(eye, red, 4, 5))
    }

    @Test
    @DisplayName("Звёзды на крыльях только у тёмных красок")
    fun starsOnlyOnDarkDyes() {
        fun stars(dye: Int): Int {
            var count = 0
            for (y in 12 until 34) {
                for (x in 16 until 60) {
                    if (brightness(PhantomColorMath.dyeBodyPixel(skin, dye, x, y)) > 600) count++
                }
            }
            return count
        }
        assertTrue(stars(black) > 5, "black ${stars(black)}")
        assertTrue(stars(blue) > 5, "blue ${stars(blue)}")
        assertEquals(0, stars(red))
    }

    @Test
    @DisplayName("Альфа пикселя сохраняется")
    fun alphaKept() {
        assertEquals(0x80, PhantomColorMath.dyeBodyPixel(0x80405060.toInt(), blue, bodyX, bodyY) ushr 24)
    }

    @Test
    @DisplayName("Седло без краски — кожа, в краске — палитра со светлыми нитками")
    fun saddleColors() {
        val plain = PhantomColorMath.saddleColors(-1)
        assertEquals(0xFF6B4226.toInt(), plain[0])
        val dyed = PhantomColorMath.saddleColors(red)
        val (leather, dark, _, stitch) = dyed
        assertTrue(channels(leather).first > channels(leather).second, "${channels(leather)}")
        assertTrue(brightness(stitch) > brightness(leather), "stitch ${channels(stitch)}")
        assertTrue(brightness(dark) < brightness(leather), "border ${channels(dark)}")
        assertTrue(dyed.all { it ushr 24 == 0xFF })
    }
}
