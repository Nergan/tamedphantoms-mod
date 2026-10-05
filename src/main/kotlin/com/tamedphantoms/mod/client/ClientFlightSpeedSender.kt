package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.config.ClientConfig
import com.tamedphantoms.mod.network.PhantomFlightSpeedPayload
import com.tamedphantoms.mod.platform.ModNetwork
import com.tamedphantoms.mod.util.PhantomFlightPace
import net.minecraft.client.Minecraft
import kotlin.math.abs

/**
 * Шлёт серверу скорость из клиентского конфига, когда игрок заходит
 * и когда меняет настройку. Полёт верхом считается на клиенте наездника
 * и читает конфиг напрямую; пакет нужен для ИИ его фантомов на сервере.
 */
object ClientFlightSpeedSender {

    private var sent = Float.NaN

    fun init() {
        PhantomFlightPace.clientPreference = { ClientConfig.ownedFlightSpeed() }
    }

    fun onClientTick() {
        val player = Minecraft.getInstance().player
        if (player == null || Minecraft.getInstance().connection == null) {
            sent = Float.NaN
            return
        }
        val value = ClientConfig.ownedFlightSpeed().toFloat()
        if (!sent.isNaN() && abs(sent - value) <= 0.001f) return
        ModNetwork.sendToServer(PhantomFlightSpeedPayload(value))
        sent = value
    }
}
