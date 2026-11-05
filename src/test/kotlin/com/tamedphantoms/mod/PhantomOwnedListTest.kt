package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomOwnedList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

class PhantomOwnedListTest {

    @Test
    @DisplayName("Один фантом — одна запись, даже если id лежит дважды или рядом пустой")
    fun onePhantomOneId() {
        val live = UUID.randomUUID()
        val other = UUID.randomUUID()
        val kept = PhantomOwnedList.keptIds(listOf(live, null, live, other, other), live)
        assertEquals(listOf(other), kept)
    }

    @Test
    @DisplayName("Пока чанк не тикает, запись не стираем. Сначала ищем рядом, потом шире")
    fun missingPhantomIsNotDroppedEarly() {
        assertEquals(Triple(0, 0, false), PhantomOwnedList.afterMiss(0, 0, areaTicking = false))
        assertEquals(Triple(1, 0, false), PhantomOwnedList.afterMiss(0, 0, areaTicking = true))
        assertEquals(Triple(1, 0, false), PhantomOwnedList.afterMiss(1, 12, areaTicking = false))
        assertEquals(Triple(1, 1, false), PhantomOwnedList.afterMiss(1, 0, areaTicking = true))
        assertEquals(Triple(1, 39, false), PhantomOwnedList.afterMiss(1, 38, areaTicking = true))
        assertEquals(Triple(0, 0, true), PhantomOwnedList.afterMiss(1, 39, areaTicking = true))
        assertEquals(PhantomOwnedList.NEAR_RADIUS, PhantomOwnedList.radiusFor(0))
        assertEquals(PhantomOwnedList.WIDE_RADIUS, PhantomOwnedList.radiusFor(1))
    }

    @Test
    @DisplayName("Старый int-массив UUID читается в тот же id")
    fun intArrayUuidRoundTrip() {
        val id = UUID.fromString("f81d4fae-7dec-11d0-a765-00a0c91e6bf6")
        val back = PhantomOwnedList.uuidFromIntArray(PhantomOwnedList.uuidToIntArray(id))
        assertEquals(id, back)
        assertNull(PhantomOwnedList.uuidFromIntArray(intArrayOf(1, 2)))
        assertTrue(PhantomOwnedList.uuidToIntArray(id).size == 4)
        assertFalse(PhantomOwnedList.keptIds(listOf(id, id), UUID.randomUUID()).size == 2)
    }
}
