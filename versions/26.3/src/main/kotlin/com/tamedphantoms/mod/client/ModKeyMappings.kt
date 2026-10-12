package com.tamedphantoms.mod.client

import com.mojang.blaze3d.platform.InputConstants
import com.tamedphantoms.mod.TamedPhantomsMod
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier

object ModKeyMappings {

    private val CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "flight"),
    )

    val FLY_UP: KeyMapping = KeyMapping(
        "key.tamedphantoms.fly_up",
        InputConstants.KEY_SPACE,
        CATEGORY,
    )

    val FLY_DOWN: KeyMapping = KeyMapping(
        "key.tamedphantoms.fly_down",
        InputConstants.KEY_LCONTROL,
        CATEGORY,
    )

    val SCREAM: KeyMapping = KeyMapping(
        "key.tamedphantoms.scream",
        InputConstants.KEY_R,
        CATEGORY,
    )

    fun init() {
        TamedPhantomsMod.LOGGER.debug("Регистрирую клавиши управления полётом Tamed Phantoms")
    }
}
