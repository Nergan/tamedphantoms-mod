package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.PhantomOwnerTeleport
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PhantomOwnerTeleportTest {

    @Test
    @DisplayName("Телепорт только если хозяин жив, далеко, и фантом не сидит, не на поводке и не везёт седока")
    fun teleportRules() {
        val far = (PhantomOwnerTeleport.DISTANCE + 1) * (PhantomOwnerTeleport.DISTANCE + 1)
        val near = 4.0
        assertTrue(PhantomOwnerTeleport.shouldTryTeleport(far, ownerAlive = true, sitting = false, leashed = false, beingRidden = false))
        assertFalse(PhantomOwnerTeleport.shouldTryTeleport(near, ownerAlive = true, sitting = false, leashed = false, beingRidden = false))
        assertFalse(PhantomOwnerTeleport.shouldTryTeleport(far, ownerAlive = false, sitting = false, leashed = false, beingRidden = false))
        assertFalse(PhantomOwnerTeleport.shouldTryTeleport(far, ownerAlive = true, sitting = true, leashed = false, beingRidden = false))
        assertFalse(PhantomOwnerTeleport.shouldTryTeleport(far, ownerAlive = true, sitting = false, leashed = true, beingRidden = false))
        assertFalse(PhantomOwnerTeleport.shouldTryTeleport(far, ownerAlive = true, sitting = false, leashed = false, beingRidden = true))
    }

    @Test
    @DisplayName("Первая точка — над хозяином, не в его ногах")
    fun firstCandidateIsAboveOwner() {
        val first = PhantomOwnerTeleport.candidateOffsets().first()
        assertEquals(Triple(0, 1, 0), first)
        assertTrue(PhantomOwnerTeleport.candidateOffsets().all { it.second >= 1 })
    }

    @Test
    @DisplayName("В блоки и лаву не телепортируем")
    fun unsafeSpaceRejected() {
        assertTrue(PhantomOwnerTeleport.isSafeSpace(inWorldBorder = true, noCollision = true, inLava = false))
        assertFalse(PhantomOwnerTeleport.isSafeSpace(inWorldBorder = true, noCollision = false, inLava = false))
        assertFalse(PhantomOwnerTeleport.isSafeSpace(inWorldBorder = false, noCollision = true, inLava = false))
        assertFalse(PhantomOwnerTeleport.isSafeSpace(inWorldBorder = true, noCollision = true, inLava = true))
    }
}
