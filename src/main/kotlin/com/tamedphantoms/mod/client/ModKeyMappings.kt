package com.tamedphantoms.mod.client

import com.mojang.blaze3d.platform.InputConstants
import com.tamedphantoms.mod.TamedPhantomsMod
import net.minecraft.client.KeyMapping

/**
 * Клавиши "Взлёт" и "Снижение" для полёта на ручном фантоме.
 *
 * Регистрируются, ЧТОБЫ клавиши были видны и переназначаемы в "Настройки"
 * -> "Управление" (это была прямая жалоба — раньше их там не было).
 *
 * Снижение по умолчанию — левый Ctrl, взлёт — пробел. Это те же физические
 * клавиши, что бег и прыжок. Ваниль отдаёт событие только одной привязке,
 * поэтому `SharedPhysicalKeyMixin` доставляет его обеим: на земле Ctrl
 * включает бег, верхом та же клавиша снижает фантома. Состояние полёта
 * уходит на сервер пакетом `PhantomInputPayload`, отдельно от прыжка и
 * крадущегося шага.
 */
object ModKeyMappings {

    val FLY_UP: KeyMapping = KeyMapping(
        "key.tamedphantoms.fly_up",
        InputConstants.KEY_SPACE,
        "key.categories.tamedphantoms",
    )

    val FLY_DOWN: KeyMapping = KeyMapping(
        "key.tamedphantoms.fly_down",
        InputConstants.KEY_LCONTROL,
        "key.categories.tamedphantoms",
    )

    val SCREAM: KeyMapping = KeyMapping(
        "key.tamedphantoms.scream",
        InputConstants.KEY_R,
        "key.categories.tamedphantoms",
    )

    fun init() {
        TamedPhantomsMod.LOGGER.debug("Регистрирую клавиши управления полётом Tamed Phantoms")
    }
}
