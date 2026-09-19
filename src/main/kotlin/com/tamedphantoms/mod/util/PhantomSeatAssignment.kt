package com.tamedphantoms.mod.util

/**
 * Логика распределения двух мест на прирученном фантоме:
 *  - Место 0 ("пилотское") — только для владельца, только оно даёт управление полётом.
 *  - Место 1 ("заднее") — для любого другого игрока.
 *
 * Никакой привязки к Minecraft нет — чистая функция на индексах/булевых
 * значениях, что позволяет протестировать все комбинации без запуска игры.
 */
object PhantomSeatAssignment {
    const val PILOT_SEAT = 0
    const val PASSENGER_SEAT = 1

    /**
     * @param mountingIsOwner садится ли именно владелец фантома
     * @param pilotSeatTaken занято ли пилотское место
     * @param passengerSeatTaken занято ли заднее место
     * @return индекс места, на которое сядет игрок, либо `null`, если сесть некуда
     */
    fun assignSeat(
        mountingIsOwner: Boolean,
        pilotSeatTaken: Boolean,
        passengerSeatTaken: Boolean,
    ): Int? = when {
        mountingIsOwner && !pilotSeatTaken -> PILOT_SEAT
        !mountingIsOwner && !passengerSeatTaken -> PASSENGER_SEAT
        else -> null
    }

    /** Только пассажир на пилотском месте управляет полётом. */
    fun canControlFlight(seatIndex: Int?): Boolean = seatIndex == PILOT_SEAT
}
