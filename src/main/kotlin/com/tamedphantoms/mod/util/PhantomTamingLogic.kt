package com.tamedphantoms.mod.util

/**
 * Небольшие чистые расчёты, вынесенные из [com.tamedphantoms.mod.entity.TamedPhantomEntity],
 * чтобы их можно было протестировать без запуска Minecraft.
 */
object PhantomTamingLogic {

    /**
     * Определяет, срабатывает ли попытка приручения.
     *
     * @param chance вероятность успеха, 0.0..1.0 (см. [com.tamedphantoms.mod.config.ServerConfig.tameChance])
     * @param roll случайное число 0.0..1.0 (обычно из `RandomSource.nextFloat()`), передаётся
     *   снаружи, чтобы функция оставалась чистой и детерминированной для тестов
     */
    fun rollTameSuccess(chance: Double, roll: Double): Boolean = roll < chance

    /**
     * Сколько HP восстановит кормление едой с указанной питательностью (nutrition).
     * По ТЗ — "хп восстановится столько, сколько еда восстановила бы голода",
     * то есть 1 к 1 с nutrition, но не больше, чем не хватает до максимума здоровья.
     */
    fun healAmount(nutrition: Int, currentHealth: Float, maxHealth: Float): Float {
        if (nutrition <= 0) return 0f
        val missing = (maxHealth - currentHealth).coerceAtLeast(0f)
        return nutrition.toFloat().coerceAtMost(missing)
    }

    /** Стоит ли вообще запускать анимацию/звук лечения (не тратим еду, если фантом уже полон здоровья). */
    fun canHeal(currentHealth: Float, maxHealth: Float): Boolean = currentHealth < maxHealth
}
