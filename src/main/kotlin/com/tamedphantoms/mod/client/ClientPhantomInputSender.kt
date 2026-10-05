package com.tamedphantoms.mod.client

import com.mojang.blaze3d.platform.InputConstants
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.event.PhantomDismount
import com.tamedphantoms.mod.input.PilotInputAccess
import com.tamedphantoms.mod.network.PhantomDismountTapPayload
import com.tamedphantoms.mod.network.PhantomInputPayload
import com.tamedphantoms.mod.network.PhantomLoopPayload
import com.tamedphantoms.mod.network.PhantomScreamPayload
import com.tamedphantoms.mod.platform.ModNetwork
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

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
            held(ModKeyMappings.FLY_UP) to held(ModKeyMappings.FLY_DOWN)
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
                ascending = held(ModKeyMappings.FLY_UP),
                descending = held(ModKeyMappings.FLY_DOWN),
                forward = player.zza,
                strafe = player.xxa,
            ),
        )
    }

    /**
     * Ваниль хранит состояние только одной привязки на физическую клавишу.
     * Пробел уже занят прыжком, левый Ctrl — бегом, поэтому `KeyMapping.isDown`
     * у «Взлёта» на Fabric остаётся false. Если привязка не получила событие,
     * читаем клавишу напрямую, пока нет открытого экрана.
     */
    private fun held(mapping: KeyMapping): Boolean {
        if (mapping.isDown) return true
        val minecraft = Minecraft.getInstance()
        if (minecraft.screen != null) return false
        val key = InputConstants.getKey(mapping.saveString())
        val window = minecraft.window.window
        return when (key.type) {
            InputConstants.Type.KEYSYM -> InputConstants.isKeyDown(window, key.value)
            InputConstants.Type.MOUSE -> GLFW.glfwGetMouseButton(window, key.value) == GLFW.GLFW_PRESS
            else -> false
        }
    }
}
