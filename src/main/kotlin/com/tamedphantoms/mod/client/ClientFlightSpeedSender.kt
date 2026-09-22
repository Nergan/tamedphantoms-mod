package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.config.ClientConfig
import com.tamedphantoms.mod.network.PhantomFlightSpeedPayload
import com.tamedphantoms.mod.util.PhantomFlightPace
import net.minecraft.client.Minecraft
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.network.PacketDistributor
import kotlin.math.abs

/**
 * Шлёт серверу скорость из клиентского конфига, когда игрок заходит
 * и когда меняет настройку. Полёт верхом считается на клиенте наездника
 * и читает конфиг напрямую; пакет нужен для ИИ его фантомов на сервере.
 */
object ClientFlightSpeedSender {

    private var sent = Float.NaN

    fun init() {
        PhantomFlightPace.clientPreference = { ClientConfig.CONFIG.ownedFlightSpeed.get() }
        NeoForge.EVENT_BUS.addListener(::onClientTick)
    }

    private fun onClientTick(event: ClientTickEvent.Post) {
        val player = Minecraft.getInstance().player
        if (player == null || Minecraft.getInstance().connection == null) {
            sent = Float.NaN
            return
        }
        val value = ClientConfig.CONFIG.ownedFlightSpeed.get().toFloat()
        if (!sent.isNaN() && abs(sent - value) <= 0.001f) return
        PacketDistributor.sendToServer(PhantomFlightSpeedPayload(value))
        sent = value
    }
}
