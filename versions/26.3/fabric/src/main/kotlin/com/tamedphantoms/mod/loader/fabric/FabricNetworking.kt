package com.tamedphantoms.mod.loader.fabric

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.event.PhantomDismount
import com.tamedphantoms.mod.network.PhantomDismountAllowPayload
import com.tamedphantoms.mod.network.PhantomDismountTapPayload
import com.tamedphantoms.mod.network.PhantomFlightSpeedPayload
import com.tamedphantoms.mod.network.PhantomInputPayload
import com.tamedphantoms.mod.network.PhantomLoopPayload
import com.tamedphantoms.mod.network.PhantomMoonPulsePayload
import com.tamedphantoms.mod.network.PhantomScreamPayload
import com.tamedphantoms.mod.util.OwnerFlightSpeed
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.level.ServerPlayer

object FabricNetworking {

    fun register() {
        PayloadTypeRegistry.serverboundPlay().register(PhantomInputPayload.TYPE, PhantomInputPayload.STREAM_CODEC)
        PayloadTypeRegistry.serverboundPlay().register(PhantomFlightSpeedPayload.TYPE, PhantomFlightSpeedPayload.STREAM_CODEC)
        PayloadTypeRegistry.serverboundPlay().register(PhantomScreamPayload.TYPE, PhantomScreamPayload.STREAM_CODEC)
        PayloadTypeRegistry.serverboundPlay().register(PhantomLoopPayload.TYPE, PhantomLoopPayload.STREAM_CODEC)
        PayloadTypeRegistry.serverboundPlay().register(PhantomDismountTapPayload.TYPE, PhantomDismountTapPayload.STREAM_CODEC)
        PayloadTypeRegistry.clientboundPlay().register(PhantomMoonPulsePayload.TYPE, PhantomMoonPulsePayload.STREAM_CODEC)
        PayloadTypeRegistry.clientboundPlay().register(PhantomDismountAllowPayload.TYPE, PhantomDismountAllowPayload.STREAM_CODEC)
        PayloadTypeRegistry.clientboundPlay().register(PhantomServerSettingsPayload.TYPE, PhantomServerSettingsPayload.STREAM_CODEC)

        ServerPlayNetworking.registerGlobalReceiver(PhantomInputPayload.TYPE) { payload, context ->
            context.server().execute {
                val player = context.player()
                val vehicle = player.vehicle
                if (vehicle is TamedPhantomEntity && vehicle.isOwnedBy(player)) {
                    vehicle.setPilotInput(payload.ascending, payload.descending, payload.forward, payload.strafe)
                }
            }
        }
        ServerPlayNetworking.registerGlobalReceiver(PhantomFlightSpeedPayload.TYPE) { payload, context ->
            context.server().execute {
                OwnerFlightSpeed.set(context.player().uuid, payload.speed.toDouble())
            }
        }
        ServerPlayNetworking.registerGlobalReceiver(PhantomScreamPayload.TYPE) { _, context ->
            context.server().execute {
                val player = context.player()
                val vehicle = player.vehicle
                if (vehicle is TamedPhantomEntity) vehicle.tryOwnerScream(player)
            }
        }
        ServerPlayNetworking.registerGlobalReceiver(PhantomLoopPayload.TYPE) { _, context ->
            context.server().execute {
                val player = context.player()
                val vehicle = player.vehicle
                if (vehicle is TamedPhantomEntity) vehicle.tryOwnerLoop(player)
            }
        }
        ServerPlayNetworking.registerGlobalReceiver(PhantomDismountTapPayload.TYPE) { _, context ->
            context.server().execute {
                PhantomDismount.onTap(context.player())
            }
        }
    }

    fun sendSettings(player: ServerPlayer) {
        ServerPlayNetworking.send(player, FabricSettings.toPayload())
    }
}
