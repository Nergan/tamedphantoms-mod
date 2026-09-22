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
 * ВАЖНО про клавишу снижения: по умолчанию это именно Left Ctrl (буквально
 * то, что и просили изначально — "Ctrl — снижение"), А НЕ Shift/"красться".
 * Раньше снижение читало состояние стандартной клавиши "красться"
 * (`Player.isShiftKeyDown`) — и именно это, судя по всему, было причиной
 * бага "Ctrl не работает совсем": в ванильной механике верховой езды
 * нажатие "красться" на многих ездовых существах спешивает игрока (или как
 * минимум конфликтует с обработкой ввода), так что нажатие толком не
 * доходило до логики снижения. Обе клавиши здесь теперь полностью
 * независимы от "Прыжка"/"Красться" и передаются на сервер отдельным
 * сетевым пакетом (см. `network/PhantomInputPayload.kt`), поэтому такого
 * конфликта больше нет в принципе — что и было целью этой переделки.
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
