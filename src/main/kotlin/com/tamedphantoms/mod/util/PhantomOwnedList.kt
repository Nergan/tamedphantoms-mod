package com.tamedphantoms.mod.util

import java.util.UUID

/**
 * Список прирученных без Minecraft: один и тот же фантом не пишется дважды,
 * а пропавший из выгруженного чанка сначала ищется рядом, и только потом шире.
 */
object PhantomOwnedList {
    const val NEAR_RADIUS = 1
    const val WIDE_RADIUS = 6

    fun radiusFor(phase: Int): Int = if (phase <= 0) NEAR_RADIUS else WIDE_RADIUS

    /**
     * Область уже тикает, а сущности в ней нет.
     * Фаза 0 расширяет поиск. Фаза 1 — записи больше нет, фантом уничтожен.
     */
    fun afterMiss(phase: Int, areaTicking: Boolean): Pair<Int, Boolean> {
        if (!areaTicking) return phase to false
        if (phase <= 0) return 1 to false
        return 0 to true
    }

    /** Убирает пустые id, текущую сущность и повтор одного и того же id. */
    fun keptIds(stored: List<UUID?>, liveId: UUID): List<UUID> {
        val seen = LinkedHashSet<UUID>()
        for (id in stored) {
            if (id == null || id == liveId) continue
            seen.add(id)
        }
        return seen.toList()
    }

    fun uuidToIntArray(id: UUID): IntArray {
        val most = id.mostSignificantBits
        val least = id.leastSignificantBits
        return intArrayOf(
            (most shr 32).toInt(),
            most.toInt(),
            (least shr 32).toInt(),
            least.toInt(),
        )
    }

    fun uuidFromIntArray(ints: IntArray): UUID? {
        if (ints.size != 4) return null
        val most = (ints[0].toLong() shl 32) or (ints[1].toLong() and 0xffffffffL)
        val least = (ints[2].toLong() shl 32) or (ints[3].toLong() and 0xffffffffL)
        return UUID(most, least)
    }
}
