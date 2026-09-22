package com.tamedphantoms.mod.network

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.util.OwnerFlightSpeed
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
        val registrar = event.registrar("3")

        registrar.playToServer(
            PhantomInputPayload.TYPE,
            PhantomInputPayload.STREAM_CODEC,
        ) { payload, context ->
            context.player().let { player ->
                val vehicle = player.vehicle
                if (vehicle is TamedPhantomEntity && vehicle.isOwnedBy(player)) {
                    vehicle.setPilotInput(payload.ascending, payload.descending, payload.forward, payload.strafe)
                }
            }
        }

        registrar.playToServer(
            PhantomFlightSpeedPayload.TYPE,
            PhantomFlightSpeedPayload.STREAM_CODEC,
        ) { payload, context ->
            OwnerFlightSpeed.set(context.player().uuid, payload.speed.toDouble())
        }

        registrar.playToServer(
            PhantomScreamPayload.TYPE,
            PhantomScreamPayload.STREAM_CODEC,
        ) { _, context ->
            val player = context.player()
            val vehicle = player.vehicle
            if (vehicle is TamedPhantomEntity) {
                vehicle.tryOwnerScream(player)
            }
        }

        registrar.playToServer(
            PhantomLoopPayload.TYPE,
            PhantomLoopPayload.STREAM_CODEC,
        ) { _, context ->
            val player = context.player()
            val vehicle = player.vehicle
            if (vehicle is TamedPhantomEntity) {
                vehicle.tryOwnerLoop(player)
            }
        }
    }
}
