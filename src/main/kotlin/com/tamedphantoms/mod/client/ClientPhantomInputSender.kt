package com.tamedphantoms.mod.client

import com.mojang.blaze3d.platform.InputConstants
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.event.PhantomDismount
import com.tamedphantoms.mod.input.PilotInputAccess
import com.tamedphantoms.mod.network.PhantomDismountTapPayload
import com.tamedphantoms.mod.network.PhantomInputPayload
import com.tamedphantoms.mod.network.PhantomLoopPayload
import com.tamedphantoms.mod.network.PhantomScreamPayload
import com.tamedphantoms.mod.network.PhantomZoomPayload
import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.platform.ModNetwork
import com.tamedphantoms.mod.util.PhantomFlightKeys
import com.tamedphantoms.mod.util.PhantomFlightModel
import net.minecraft.ChatFormatting
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
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
        val screamPressed = screamDown && !screamWasDown
        screamWasDown = screamDown
        PhantomDismount.heal(player)
        val phantom = player.vehicle as? TamedPhantomEntity
        if (shiftPressed && minecraft.screen == null && phantom != null) {
            PhantomDismount.noteLocalDismountRequest()
            ModNetwork.sendToServer(PhantomDismountTapPayload)
        }
        val pilot = phantom != null && phantom.isOwnedBy(player) && phantom.controllingPassenger === player
        // В конце петли клавиша крика рвёт фантома вверх. В остальное время она кричит, как раньше.
        val zoomOpen = phantom != null && pilot && phantom.zoomWindowOpen && ServerConfig.zoomBoost() > 0.0
        val rollOpen = phantom != null && pilot && phantom.rollBoostReady
        val rollCharging = phantom != null && pilot && phantom.rollBoostCharging
        if (phantom != null && screamPressed && phantom.isOwnedBy(player)) {
            when {
                zoomOpen -> phantom.clientZoomRequest = true
                rollOpen || rollCharging -> phantom.clientRollBoostRequest = true
                else -> ModNetwork.sendToServer(PhantomScreamPayload)
            }
            if (zoomOpen || rollOpen || rollCharging) promptFlash = 5
        }
        var prompt = when {
            zoomOpen && phantom?.clientZoomRequest != true -> "zoom"
            rollOpen -> "roll"
            else -> ""
        }
        if (prompt.isEmpty() && promptFlash > 0 && (promptKey == "roll" || promptKey == "zoom")) prompt = promptKey
        showManeuverPrompt(player, prompt, promptFlash > 0)
        if (promptFlash > 0) promptFlash--
        if (phantom != null && phantom.loopReadyToReport && phantom.isOwnedBy(player)) {
            phantom.loopReadyToReport = false
            ModNetwork.sendToServer(PhantomLoopPayload)
        }
        if (phantom != null && phantom.zoomReadyToReport && phantom.isOwnedBy(player)) {
            phantom.zoomReadyToReport = false
            ModNetwork.sendToServer(PhantomZoomPayload)
        }
        if (phantom == null) {
            flightKeys.stop()
            return
        }

        val options = minecraft.options
        val ascending = held(ModKeyMappings.FLY_UP)
        val descending = held(ModKeyMappings.FLY_DOWN)
        val keys = PhantomFlightKeys.Keys(
            forward = options.keyUp.isDown,
            back = options.keyDown.isDown,
            left = options.keyLeft.isDown,
            right = options.keyRight.isDown,
            up = ascending,
            down = descending,
            sneak = shiftDown,
            loop = held(ModKeyMappings.LOOP),
            rollLeft = held(ModKeyMappings.ROLL_LEFT),
            rollRight = held(ModKeyMappings.ROLL_RIGHT),
        )
        // Чат, пауза и потеря фокуса отпускают клавиши. Это не посадка и не новый ввод:
        // круиз остаётся, пока окно без экрана и фантом в воздухе не получит настоящую кнопку.
        val suspended = minecraft.screen != null || !minecraft.isWindowActive
        val result = if (suspended) {
            PhantomFlightKeys.Result(PhantomFlightKeys.CruiseChange.NONE, false, 0)
        } else {
            flightKeys.tick(player.tickCount, keys, pilot && phantom.airborne(), pilot && phantom.canFigure())
        }
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
                boost = phantom.rollBoostAmount(),
                impactGust = phantom.clientImpactGust != null,
                impactX = phantom.clientImpactGust?.x?.toFloat() ?: 0f,
                impactY = phantom.clientImpactGust?.y?.toFloat() ?: 0f,
                impactZ = phantom.clientImpactGust?.z?.toFloat() ?: 0f,
                sharpStop = phantom.clientSharpStop > 0.0,
                sharpSpeed = phantom.clientSharpStop.toFloat(),
                rollBurst = phantom.clientRollBurst,
            ),
        )
        phantom.clientImpactGust = null
        phantom.clientSharpStop = 0.0
        phantom.clientRollBurst = false
    }

    private val flightKeys = PhantomFlightKeys()

    private var promptKey = ""
    private var promptFlash = 0
    private var promptShownFlash = false

    /** Одна подсказка над хотбаром. Нажатие R на мгновение красит букву клавиши. */
    private fun showManeuverPrompt(player: Player, key: String, flash: Boolean) {
        if (key == promptKey && flash == promptShownFlash) return
        promptKey = key
        promptShownFlash = flash
        val keyName = ModKeyMappings.SCREAM.translatedKeyMessage
        val shown = if (flash) keyName.copy().withStyle(ChatFormatting.RED) else keyName
        val text = when (key) {
            "zoom" -> Component.translatable("tamedphantoms.zoom.prompt", shown)
            "roll" -> Component.translatable("tamedphantoms.roll.prompt", shown)
            else -> Component.empty()
        }
        player.displayClientMessage(text, true)
    }

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
