package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomColorMath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PhantomDyeColorTest {

    private val skin = 0xFF3C4A6E.toInt()
    private val membrane = 0xFF8FA0B5.toInt()

    private fun channels(argb: Int) = Triple((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)

    @Test
    @DisplayName("Прозрачный пиксель краска не трогает")
    fun transparentStays() {
        assertEquals(0x00123456, PhantomColorMath.dyePixel(0x00123456, 0xB02E26))
    }

    @Test
    @DisplayName("Красный краситель даёт красный оттенок, рисунок шкуры светлее и темнее остаётся")
    fun redKeepsPattern() {
        val dark = channels(PhantomColorMath.dyePixel(skin, 0xB02E26))
        val light = channels(PhantomColorMath.dyePixel(membrane, 0xB02E26))
        assertTrue(dark.first > dark.second && dark.first > dark.third, "$dark")
        assertTrue(light.first > dark.first, "$light vs $dark")
    }

    @Test
    @DisplayName("Белая краска светлее чёрной на той же шкуре")
    fun whiteBrighterThanBlack() {
        val white = channels(PhantomColorMath.dyePixel(skin, 0xF9FFFE))
        val black = channels(PhantomColorMath.dyePixel(skin, 0x1D1D21))
        assertTrue(white.first > black.first + 80, "$white vs $black")
    }

    @Test
    @DisplayName("Альфа пикселя сохраняется")
    fun alphaKept() {
        assertEquals(0x80, PhantomColorMath.dyePixel(0x80405060.toInt(), 0x3C44AA) ushr 24)
    }
}
