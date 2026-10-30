package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomPack
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

class PhantomPackTest {

    private val alice = UUID.randomUUID()
    private val bob = UUID.randomUUID()
    private val near = 10.0 * 10.0
    private val far = 200.0 * 200.0

    @Test
    @DisplayName("За освобождённого встают освобождённые рядом, ручные и дальние — нет")
    fun releasedPackIsNearby() {
        assertTrue(PhantomPack.joins(false, null, false, null, near))
        assertFalse(PhantomPack.joins(false, null, false, null, far))
        assertFalse(PhantomPack.joins(false, null, true, alice, near))
    }

    @Test
    @DisplayName("За ручного встают все фантомы его хозяина, где бы ни были, чужие и освобождённые — нет")
    fun tamedPackIsTheOwnersPhantoms() {
        assertTrue(PhantomPack.joins(true, alice, true, alice, near))
        assertTrue(PhantomPack.joins(true, alice, true, alice, far))
        assertFalse(PhantomPack.joins(true, alice, true, bob, near))
        assertFalse(PhantomPack.joins(true, alice, false, null, near))
        assertFalse(PhantomPack.joins(true, null, true, null, near))
    }
}
