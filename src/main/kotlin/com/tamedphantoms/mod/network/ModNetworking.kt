package com.tamedphantoms.mod.network

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

/**
 * Регистрация пакета [PhantomInputPayload] и его обработчика на сервере.
 * Это ОБЩИЙ (common) код — вызывается для обеих сторон, но сам обработчик
 * реально что-то делает только когда пакет ПРИНИМАЕТ сервер.
 */
object ModNetworking {

    fun init(modBus: IEventBus) {
        modBus.addListener(::onRegisterPayloadHandlers)
    }

    private fun onRegisterPayloadHandlers(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar("1")

        registrar.playToServer(
            PhantomInputPayload.TYPE,
            PhantomInputPayload.STREAM_CODEC,
        ) { payload, context ->
            context.player().let { player ->
                val vehicle = player.vehicle
                if (vehicle is TamedPhantomEntity && vehicle.isOwnedBy(player)) {
                    vehicle.setPilotInput(payload.ascending, payload.descending)
                }
            }
        }
    }
}
