package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomSeatAssignment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PhantomSeatAssignmentTest {

    @Test
    @DisplayName("Владелец, севший первым, получает пилотское место")
    fun ownerGetsPilotSeatWhenFree() {
        val seat = PhantomSeatAssignment.assignSeat(mountingIsOwner = true, pilotSeatTaken = false, passengerSeatTaken = false)
        assertEquals(PhantomSeatAssignment.PILOT_SEAT, seat)
        assertEquals(true, PhantomSeatAssignment.canControlFlight(seat))
    }

    @Test
    @DisplayName("Чужой игрок всегда получает заднее место, даже если пилотское свободно")
    fun strangerAlwaysGetsBackSeat() {
        val seat = PhantomSeatAssignment.assignSeat(mountingIsOwner = false, pilotSeatTaken = false, passengerSeatTaken = false)
        assertEquals(PhantomSeatAssignment.PASSENGER_SEAT, seat)
        assertEquals(false, PhantomSeatAssignment.canControlFlight(seat))
    }

    @Test
    @DisplayName("Второй желающий сесть на то же место получает отказ")
    fun secondOwnerAttemptIsRejected() {
        val seat = PhantomSeatAssignment.assignSeat(mountingIsOwner = true, pilotSeatTaken = true, passengerSeatTaken = false)
        assertNull(seat)
    }

    @Test
    @DisplayName("Второй чужой игрок не может сесть, если заднее место занято")
    fun secondStrangerIsRejected() {
        val seat = PhantomSeatAssignment.assignSeat(mountingIsOwner = false, pilotSeatTaken = false, passengerSeatTaken = true)
        assertNull(seat)
    }

    @Test
    @DisplayName("Оба места заняты — никто больше не сядет, независимо от роли")
    fun bothSeatsTakenRejectsEveryone() {
        assertNull(PhantomSeatAssignment.assignSeat(mountingIsOwner = true, pilotSeatTaken = true, passengerSeatTaken = true))
        assertNull(PhantomSeatAssignment.assignSeat(mountingIsOwner = false, pilotSeatTaken = true, passengerSeatTaken = true))
    }

    @Test
    @DisplayName("Управлять полётом можно только с пилотского места")
    fun onlyPilotSeatControls() {
        assertEquals(true, PhantomSeatAssignment.canControlFlight(PhantomSeatAssignment.PILOT_SEAT))
        assertEquals(false, PhantomSeatAssignment.canControlFlight(PhantomSeatAssignment.PASSENGER_SEAT))
        assertEquals(false, PhantomSeatAssignment.canControlFlight(null))
    }
}
