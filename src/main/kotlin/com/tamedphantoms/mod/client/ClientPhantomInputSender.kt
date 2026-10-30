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
import com.tamedphantoms.mod.util.PhantomFlightKeys
import com.tamedphantoms.mod.util.PhantomFlightModel
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
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
        if (phantom == null) {
            flightKeys.stop()
            return
        }

        val options = minecraft.options
        val ascending = held(ModKeyMappings.FLY_UP)
        val descending = held(ModKeyMappings.FLY_DOWN)
        val pilot = phantom.isOwnedBy(player) && phantom.controllingPassenger === player
        val keys = PhantomFlightKeys.Keys(
            forward = options.keyUp.isDown,
            back = options.keyDown.isDown,
            left = options.keyLeft.isDown,
            right = options.keyRight.isDown,
            up = ascending,
            down = descending,
            sneak = shiftDown,
        )
        val result = flightKeys.tick(player.tickCount, keys, pilot && minecraft.screen == null && phantom.airborne())
        phantom.clientCruise = flightKeys.cruising
        if (result.loop) phantom.clientLoopRequest = true
        if (result.roll != 0) phantom.clientRollRequest = result.roll
        when (result.cruise) {
            PhantomFlightKeys.CruiseChange.STARTED ->
                player.displayClientMessage(Component.translatable("tamedphantoms.cruise.on"), true)
            PhantomFlightKeys.CruiseChange.STOPPED ->
                player.displayClientMessage(Component.translatable("tamedphantoms.cruise.off"), true)
            PhantomFlightKeys.CruiseChange.NONE -> Unit
        }

        ModNetwork.sendToServer(
            PhantomInputPayload(
                ascending = ascending,
                descending = descending,
                forward = if (flightKeys.cruising) 1f else player.zza,
                strafe = player.xxa,
                pitch = PhantomFlightModel.wrap(phantom.acroPitch),
                roll = PhantomFlightModel.wrap(phantom.bank),
                tuck = phantom.flightTuck,
                effort = phantom.flightEffort,
                acrobatic = phantom.acrobatic,
            ),
        )
    }

    private val flightKeys = PhantomFlightKeys()

    /**
     * Ваниль хранит в карте только одну привязку на физическую клавишу.
     * `SharedPhysicalKeyMixin` раздаёт событие и спринту, и полёту, но
     * если слот всё же достался ванили, `isDown` у полёта остаётся false.
     * Тогда читаем клавишу напрямую, пока нет открытого экрана.
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
