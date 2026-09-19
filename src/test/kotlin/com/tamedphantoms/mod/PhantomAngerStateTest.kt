package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomAngerLogic
import com.tamedphantoms.mod.util.PhantomAngerState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

class PhantomAngerStateTest {

    private val attacker: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")

    @Test
    @DisplayName("В состоянии NONE фантом не активен и цели нет")
    fun defaultStateIsInactive() {
        assertFalse(PhantomAngerState.NONE.isActive)
        assertNull(PhantomAngerState.NONE.targetId)
    }

    @Test
    @DisplayName("Удар запускает таймер на конкретного обидчика")
    fun hurtStartsAnger() {
        val state = PhantomAngerLogic.onHurtBy(attacker, 2400)
        assertTrue(state.isActive)
        assertEquals(attacker, state.targetId)
        assertEquals(2400, state.remainingTicks)
    }

    @Test
    @DisplayName("Каждый тик уменьшает оставшееся время на 1")
    fun tickDecrementsByOne() {
        var state = PhantomAngerLogic.onHurtBy(attacker, 10)
        state = PhantomAngerLogic.tick(state)
        assertEquals(9, state.remainingTicks)
        assertTrue(state.isActive)
    }

    @Test
    @DisplayName("Ровно через 2400 тиков (2 минуты) фантом снова мирный")
    fun angerExpiresAfterExactlyTwoMinutes() {
        var state = PhantomAngerLogic.onHurtBy(attacker, 20 * 60 * 2)
        repeat(20 * 60 * 2) {
            state = PhantomAngerLogic.tick(state)
        }
        assertFalse(state.isActive)
        assertNull(state.targetId)
        assertEquals(PhantomAngerState.NONE, state)
    }

    @Test
    @DisplayName("За один тик до истечения фантом всё ещё активен")
    fun stillActiveOneTickBeforeExpiry() {
        var state = PhantomAngerLogic.onHurtBy(attacker, 2)
        state = PhantomAngerLogic.tick(state) // remaining = 1
        assertTrue(state.isActive)
        state = PhantomAngerLogic.tick(state) // remaining = 0 -> сброс
        assertFalse(state.isActive)
    }

    @Test
    @DisplayName("Повторный удар другой сущностью переключает цель на неё")
    fun secondHitBySomeoneElseSwitchesTarget() {
        val other = UUID.fromString("22222222-2222-2222-2222-222222222222")
        var state = PhantomAngerLogic.onHurtBy(attacker, 100)
        state = PhantomAngerLogic.onHurtBy(other, 2400)
        assertEquals(other, state.targetId)
        assertEquals(2400, state.remainingTicks)
    }

    @Test
    @DisplayName("Тик пустого состояния остаётся пустым")
    fun tickingNoneStaysNone() {
        val result = PhantomAngerLogic.tick(PhantomAngerState.NONE)
        assertEquals(PhantomAngerState.NONE, result)
    }
}
