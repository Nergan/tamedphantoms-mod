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

    /** По умолчанию пробел: двойное нажатие — мёртвая петля, как раньше двойной взлёт. */
    val LOOP: KeyMapping = KeyMapping(
        "key.tamedphantoms.loop",
        InputConstants.KEY_SPACE,
        CATEGORY,
    )

    /** По умолчанию A и D: двойное нажатие — бочка, как раньше двойной шаг в сторону. */
    val ROLL_LEFT: KeyMapping = KeyMapping(
        "key.tamedphantoms.roll_left",
        InputConstants.KEY_A,
        CATEGORY,
    )

    val ROLL_RIGHT: KeyMapping = KeyMapping(
        "key.tamedphantoms.roll_right",
        InputConstants.KEY_D,
        CATEGORY,
    )

    fun init() {
        TamedPhantomsMod.LOGGER.debug("Регистрирую клавиши управления полётом Tamed Phantoms")
    }
}
