package com.tamedphantoms.mod.util

/**
 * Когда телепортировать прирученного фантома к хозяину и куда пробовать
 * поставить его, чтобы не застрять в блоках. Без Minecraft — для тестов.
 */
object PhantomOwnerTeleport {
    const val DISTANCE = 16.0

    fun shouldTryTeleport(
        distanceSq: Double,
        ownerAlive: Boolean,
        sitting: Boolean,
        leashed: Boolean,
        beingRidden: Boolean,
    ): Boolean {
        if (!ownerAlive || sitting || leashed || beingRidden) return false
        return distanceSq > DISTANCE * DISTANCE
    }

    /**
     * Смещения от хозяина: сначала воздух над ним, потом кольца вокруг.
     * Y всегда выше ног, чтобы летающий фантом не попал в пол.
     */
    fun candidateOffsets(): List<Triple<Int, Int, Int>> {
        val result = ArrayList<Triple<Int, Int, Int>>(80)
        for (dy in 1..4) {
            result += Triple(0, dy, 0)
        }
        for (radius in 1..3) {
            for (dy in 1..4) {
                for (dx in -radius..radius) {
                    for (dz in -radius..radius) {
                        if (kotlin.math.abs(dx) != radius && kotlin.math.abs(dz) != radius) continue
                        result += Triple(dx, dy, dz)
                    }
                }
            }
        }
        return result
    }

    fun isSafeSpace(inWorldBorder: Boolean, noCollision: Boolean, inLava: Boolean): Boolean =
        inWorldBorder && noCollision && !inLava
}
