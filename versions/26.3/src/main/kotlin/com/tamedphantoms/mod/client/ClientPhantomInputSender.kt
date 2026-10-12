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
import net.minecraft.client.gui.screens.Screen

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
        if (shiftPressed && openScreen(minecraft) == null && phantom != null) {
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

    private fun held(mapping: KeyMapping): Boolean {
        if (mapping.isDown) return true
        val minecraft = Minecraft.getInstance()
        if (openScreen(minecraft) != null) return false
        val key = InputConstants.getKey(mapping.saveString())
        return when (key.type) {
            InputConstants.Type.KEYBOARD -> InputConstants.isKeyDown(key.value)
            InputConstants.Type.MOUSE -> mouseDown(key.value)
            else -> false
        }
    }

    private fun mouseDown(button: Int): Boolean {
        val handler = Minecraft.getInstance().mouseHandler
        return when (button) {
            0 -> handler.isLeftPressed
            1 -> handler.isRightPressed
            else -> false
        }
    }

    private fun openScreen(minecraft: Minecraft): Screen? {
        val method = Minecraft::class.java.methods.firstOrNull {
            it.parameterCount == 0 && Screen::class.java.isAssignableFrom(it.returnType)
        }
        if (method != null) {
            return method.invoke(minecraft) as? Screen
        }
        val field = Minecraft::class.java.declaredFields.firstOrNull {
            Screen::class.java.isAssignableFrom(it.type)
        } ?: return null
        field.isAccessible = true
        return field.get(minecraft) as? Screen
    }
}
