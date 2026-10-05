package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.event.PhantomDismount
import com.tamedphantoms.mod.input.PilotInputAccess
import com.tamedphantoms.mod.network.PhantomDismountTapPayload
import com.tamedphantoms.mod.network.PhantomInputPayload
import com.tamedphantoms.mod.network.PhantomLoopPayload
import com.tamedphantoms.mod.network.PhantomScreamPayload
import com.tamedphantoms.mod.platform.ModNetwork
import net.minecraft.client.Minecraft

/**
 * Пока игрок верхом на ручном фантоме, каждый клиентский тик отправляет на
 * сервер состояние клавиш "Взлёт"/"Снижение" (см. PhantomInputPayload).
 * Отправка только при смене состояния была бы эффективнее, но при потере
 * пакета могла бы "залипнуть" — раз в тик проще и надёжнее для такого
 * маленького пакета (2 бита полезной нагрузки).
 */
object ClientPhantomInputSender {

    private var screamWasDown = false
    private var dismountShiftWasDown = false

    fun init() {
        PilotInputAccess.clientReader = {
            ModKeyMappings.FLY_UP.isDown to ModKeyMappings.FLY_DOWN.isDown
        }
        PhantomDismount.isLocalPlayer = { rider -> rider === Minecraft.getInstance().player }
    }

    fun onClientTick() {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        val shiftDown = minecraft.options.keyShift.isDown
        val shiftPressed = shiftDown && !dismountShiftWasDown
        dismountShiftWasDown = shiftDown
        val screamDown = ModKeyMappings.SCREAM.isDown
        val phantom = player.vehicle as? TamedPhantomEntity
        if (shiftPressed && minecraft.screen == null && phantom != null) {
            ModNetwork.sendToServer(PhantomDismountTapPayload)
        }
        if (phantom != null && screamDown && !screamWasDown && phantom.isOwnedBy(player)) {
            ModNetwork.sendToServer(PhantomScreamPayload)
        }
        screamWasDown = screamDown
        if (phantom != null && phantom.loopReadyToReport && phantom.isOwnedBy(player)) {
            phantom.loopReadyToReport = false
            ModNetwork.sendToServer(PhantomLoopPayload)
        }
        if (phantom == null) return

        ModNetwork.sendToServer(
            PhantomInputPayload(
                ascending = ModKeyMappings.FLY_UP.isDown,
                descending = ModKeyMappings.FLY_DOWN.isDown,
                forward = player.zza,
                strafe = player.xxa,
            ),
        )
    }
}
