package com.tamedphantoms.mod.platform

import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

/**
 * Отправка пакетов и проверка «мод установлен».
 * Точку подставляет загрузчик в своём инициализаторе.
 */
object ModNetwork {

    var sendToServer: (CustomPacketPayload) -> Unit = {}
    var sendToPlayer: (ServerPlayer, CustomPacketPayload) -> Unit = { _, _ -> }
    var sendToDimension: (ServerLevel, CustomPacketPayload) -> Unit = { _, _ -> }
    var isModLoaded: (String) -> Boolean = { false }
}
