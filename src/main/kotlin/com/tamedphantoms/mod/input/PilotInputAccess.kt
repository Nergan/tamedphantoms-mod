package com.tamedphantoms.mod.input

/**
 * Клиентские клавиши взлёта/снижения читаются на клиенте (там
 * [net.minecraft.world.entity.Entity.isControlledByLocalInstance] = true
 * у наездника). На сервер они всё равно уходят пакетом — на случай
 * серверной логики и чтобы кнопки не "залипали".
 *
 * Сам объект не импортирует клиентские классы: лямбду выставляет
 * клиентский код при загрузке.
 */
object PilotInputAccess {

    @Volatile
    var clientReader: (() -> Pair<Boolean, Boolean>)? = null

    fun read(clientSide: Boolean, serverAscending: Boolean, serverDescending: Boolean): Pair<Boolean, Boolean> {
        if (clientSide) {
            return clientReader?.invoke() ?: (false to false)
        }
        return serverAscending to serverDescending
    }
}
