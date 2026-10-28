package com.tamedphantoms.mod

import com.tamedphantoms.mod.util.VanillaPhantomSoundPaths
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class VanillaPhantomSoundPathsTest {
    @Test
    fun phantomFileMovesToTheModNamespace() {
        assertEquals(
            "tamedphantoms:vanilla/mob/phantom/idle1",
            VanillaPhantomSoundPaths.redirectedLocation("minecraft", "mob/phantom/idle1"),
        )
        assertNull(VanillaPhantomSoundPaths.redirectedLocation("minecraft", "mob/bee/loop"))
        assertNull(VanillaPhantomSoundPaths.redirectedLocation("tamedphantoms", "vanilla/mob/phantom/idle1"))
    }

    @Test
    fun bufferPathPointsAtTheVanillaOgg() {
        assertEquals(
            "minecraft:sounds/mob/phantom/idle1.ogg",
            VanillaPhantomSoundPaths.bufferToVanillaFile(
                "tamedphantoms",
                "sounds/vanilla/mob/phantom/idle1.ogg",
            ),
        )
        assertNull(
            VanillaPhantomSoundPaths.bufferToVanillaFile("minecraft", "sounds/mob/phantom/idle1.ogg"),
        )
    }
}
