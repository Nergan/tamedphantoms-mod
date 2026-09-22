package com.tamedphantoms.mod.util

import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.entity.TamedPhantomEntity

/**
 * Множитель скорости относительно дикого фантома.
 *
 * Числа в коде полёта — это прежнее поведение питомца, оно ощущается как ×2
 * от дикого. Поэтому игровой множитель 2.0 ничего не меняет, а 1.25
 * (значение по умолчанию) берёт 0.625 от этих чисел.
 * Под водой скорость ещё умножается на 0.8.
 *
 * Клиентская настройка хозяина может только убавить скорость его
 * прирученных фантомов. Освобождённые смотрят только на серверный множитель.
 */
object PhantomFlightPace {

    const val BASELINE = 2.0
    const val MIN = 0.25
    const val MAX = 4.0
    const val WATER_SPEED = 0.8

    /**
     * Подставляется клиентом: его собственная настройка.
     * На выделенном сервере остаётся null.
     */
    @Volatile
    var clientPreference: (() -> Double)? = null

    fun multiple(tamed: Boolean, server: Double, ownerPreference: Double?): Double {
        val cap = server.coerceIn(MIN, MAX)
        if (!tamed) return cap
        val preference = (ownerPreference ?: cap).coerceIn(MIN, MAX)
        return minOf(preference, cap)
    }

    fun paceOf(multiple: Double): Float = (multiple / BASELINE).toFloat()

    fun waterScale(underwater: Boolean): Double = if (underwater) WATER_SPEED else 1.0

    fun pace(phantom: TamedPhantomEntity): Float {
        val server = ServerConfig.CONFIG.flightSpeed.get()
        val preference = if (phantom.level().isClientSide) {
            clientPreference?.invoke()
        } else {
            OwnerFlightSpeed.get(phantom.ownerUUID)
        }
        return paceOf(multiple(phantom.tamed, server, preference))
    }
}
