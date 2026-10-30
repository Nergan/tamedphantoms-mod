package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomTemptPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.hypot

class PhantomTemptPathTest {

    /** Множитель горизонтали в ванильном `PhantomMoveControl`. */
    private fun vanillaFactor(fromX: Double, fromY: Double, fromZ: Double, aim: PhantomTemptPath.Point): Double {
        val horiz = hypot(aim.x - fromX, aim.z - fromZ)
        return 1.0 - abs((aim.y - fromY) * 0.7) / horiz
    }

    @Test
    @DisplayName("Высоко над игроком фантом спускается по спирали, не разворачиваясь задом")
    fun steepDescentStaysForward() {
        val aim = PhantomTemptPath.aim(0.5, 40.0, 0.5, 0.0, 1.4, 0.0)
        assertTrue(vanillaFactor(0.5, 40.0, 0.5, aim) > 0.3)
        assertTrue(aim.y < 40.0)
        assertEquals(PhantomTemptPath.ORBIT, hypot(aim.x, aim.z), 1.0E-6)
    }

    @Test
    @DisplayName("Издалека фантом летит прямо к игроку")
    fun farAwayGoesStraight() {
        val aim = PhantomTemptPath.aim(30.0, 12.0, 0.0, 0.0, 1.4, 0.0)
        assertEquals(0.0, aim.x, 1.0E-9)
        assertEquals(0.0, aim.z, 1.0E-9)
        assertEquals(1.4, aim.y, 1.0E-9)
    }

    @Test
    @DisplayName("Подъём к игроку тоже не круче предела")
    fun climbIsLimited() {
        val aim = PhantomTemptPath.aim(4.0, -20.0, 0.0, 0.0, 1.4, 0.0)
        assertTrue(vanillaFactor(4.0, -20.0, 0.0, aim) > 0.3)
    }
}
