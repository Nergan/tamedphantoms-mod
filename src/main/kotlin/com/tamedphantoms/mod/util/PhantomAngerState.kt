package com.tamedphantoms.mod.util

import java.util.UUID

/**
 * Состояние "режима самозащиты" прирученного фантома: он агрится на
 * конкретного обидчика [targetId] на [remainingTicks] тиков, после чего
 * снова становится полностью мирным.
 *
 * Чистая структура данных без зависимостей от Minecraft — используется
 * внутри `TamedPhantomEntity`, но лёгкая для юнит-тестирования отдельно.
 */
data class PhantomAngerState(val targetId: UUID?, val remainingTicks: Int) {
    val isActive: Boolean get() = targetId != null && remainingTicks > 0

    companion object {
        val NONE = PhantomAngerState(null, 0)
    }
}

object PhantomAngerLogic {

    /** Кто-то ударил фантом — начинаем/обновляем таймер самозащиты на этого обидчика. */
    fun onHurtBy(attackerId: UUID, durationTicks: Int): PhantomAngerState =
        PhantomAngerState(attackerId, durationTicks)

    /**
     * Тик таймера. Если время вышло — сбрасываем цель (фантом снова мирный).
     * Если цель отсутствует — состояние не меняется.
     */
    fun tick(state: PhantomAngerState): PhantomAngerState {
        if (state.targetId == null || state.remainingTicks <= 0) return PhantomAngerState.NONE
        val next = state.remainingTicks - 1
        return if (next <= 0) PhantomAngerState.NONE else state.copy(remainingTicks = next)
    }
}
