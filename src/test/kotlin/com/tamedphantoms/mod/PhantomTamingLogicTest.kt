package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomTamingLogic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PhantomTamingLogicTest {

    @Test
    @DisplayName("При шансе 1.0 приручение срабатывает при любом броске меньше 1.0")
    fun fullChanceAlwaysSucceeds() {
        assertTrue(PhantomTamingLogic.rollTameSuccess(1.0, 0.0))
        assertTrue(PhantomTamingLogic.rollTameSuccess(1.0, 0.9999))
    }

    @Test
    @DisplayName("При шансе 0.0 приручение никогда не срабатывает")
    fun zeroChanceNeverSucceeds() {
        assertFalse(PhantomTamingLogic.rollTameSuccess(0.0, 0.0))
        assertFalse(PhantomTamingLogic.rollTameSuccess(0.0, 0.5))
    }

    @Test
    @DisplayName("Бросок ровно на границе шанса считается неудачей (roll >= chance)")
    fun rollEqualToChanceFails() {
        assertFalse(PhantomTamingLogic.rollTameSuccess(0.5, 0.5))
        assertTrue(PhantomTamingLogic.rollTameSuccess(0.5, 0.49))
    }

    @Test
    @DisplayName("Лечение восстанавливает ровно nutrition ХП, если хватает места до максимума")
    fun healRestoresExactNutrition() {
        val amount = PhantomTamingLogic.healAmount(nutrition = 4, currentHealth = 10f, maxHealth = 20f)
        assertEquals(4f, amount)
    }

    @Test
    @DisplayName("Лечение не может поднять здоровье выше максимума")
    fun healIsCappedAtMaxHealth() {
        val amount = PhantomTamingLogic.healAmount(nutrition = 10, currentHealth = 18f, maxHealth = 20f)
        assertEquals(2f, amount)
    }

    @Test
    @DisplayName("На полном здоровье лечить не нужно (и восстанавливается 0)")
    fun noHealNeededAtFullHealth() {
        assertFalse(PhantomTamingLogic.canHeal(currentHealth = 20f, maxHealth = 20f))
        assertEquals(0f, PhantomTamingLogic.healAmount(nutrition = 5, currentHealth = 20f, maxHealth = 20f))
    }

    @Test
    @DisplayName("Еда с нулевой питательностью ничего не лечит")
    fun zeroNutritionHealsNothing() {
        assertEquals(0f, PhantomTamingLogic.healAmount(nutrition = 0, currentHealth = 5f, maxHealth = 20f))
    }
}
