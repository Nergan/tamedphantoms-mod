package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomHeldLook
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PhantomHeldLookTest {

    @Test
    @DisplayName("Прирученный смотрит на любую еду и не смотрит на пустые руки")
    fun tamedStaresAtFood() {
        assertTrue(PhantomHeldLook.shouldStare(tamed = true, holdsFood = true, holdsTameItem = false))
        assertTrue(PhantomHeldLook.shouldStare(tamed = true, holdsFood = true, holdsTameItem = true))
        assertFalse(PhantomHeldLook.shouldStare(tamed = true, holdsFood = false, holdsTameItem = true))
        assertFalse(PhantomHeldLook.shouldStare(tamed = true, holdsFood = false, holdsTameItem = false))
    }

    @Test
    @DisplayName("Освобождённый и дикий смотрят только на предмет приручения")
    fun releasedAndWildStareAtTameItem() {
        assertTrue(PhantomHeldLook.shouldStare(tamed = false, holdsFood = false, holdsTameItem = true))
        assertTrue(PhantomHeldLook.shouldStare(tamed = false, holdsFood = true, holdsTameItem = true))
        assertFalse(PhantomHeldLook.shouldStare(tamed = false, holdsFood = true, holdsTameItem = false))
        assertFalse(PhantomHeldLook.shouldStare(tamed = false, holdsFood = false, holdsTameItem = false))
    }
}
