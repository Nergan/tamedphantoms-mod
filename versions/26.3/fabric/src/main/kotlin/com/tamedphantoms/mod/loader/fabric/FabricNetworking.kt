package com.tamedphantoms.mod.loader.fabric

import com.tamedphantoms.mod.api.TamedPhantomsApi
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.event.PhantomDismount
import com.tamedphantoms.mod.event.PhantomRoster
import com.tamedphantoms.mod.network.PhantomDismountAllowPayload
import com.tamedphantoms.mod.network.PhantomDismountTapPayload
import com.tamedphantoms.mod.network.PhantomFlightSpeedPayload
import com.tamedphantoms.mod.network.PhantomInputPayload
import com.tamedphantoms.mod.network.PhantomLoopPayload
import com.tamedphantoms.mod.network.PhantomRosterListPayload
import com.tamedphantoms.mod.network.PhantomRosterPayload
import com.tamedphantoms.mod.network.PhantomRosterSummonPayload
import com.tamedphantoms.mod.network.PhantomScreamPayload
import com.tamedphantoms.mod.network.PhantomZoomPayload
import com.tamedphantoms.mod.util.OwnerFlightSpeed
import com.tamedphantoms.mod.util.PhantomImpactGust
import com.tamedphantoms.mod.util.PhantomZoomBurst
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.server.level.ServerPlayer

object FabricNetworking {

    fun register() {
        PayloadTypeRegistry.serverboundPlay().register(PhantomInputPayload.TYPE, PhantomInputPayload.STREAM_CODEC)
        PayloadTypeRegistry.serverboundPlay().register(PhantomFlightSpeedPayload.TYPE, PhantomFlightSpeedPayload.STREAM_CODEC)
        PayloadTypeRegistry.serverboundPlay().register(PhantomScreamPayload.TYPE, PhantomScreamPayload.STREAM_CODEC)
        PayloadTypeRegistry.serverboundPlay().register(PhantomRosterPayload.TYPE, PhantomRosterPayload.STREAM_CODEC)
        PayloadTypeRegistry.serverboundPlay().register(PhantomRosterSummonPayload.TYPE, PhantomRosterSummonPayload.STREAM_CODEC)
        PayloadTypeRegistry.serverboundPlay().register(PhantomLoopPayload.TYPE, PhantomLoopPayload.STREAM_CODEC)
        PayloadTypeRegistry.serverboundPlay().register(PhantomZoomPayload.TYPE, PhantomZoomPayload.STREAM_CODEC)
        PayloadTypeRegistry.serverboundPlay().register(PhantomDismountTapPayload.TYPE, PhantomDismountTapPayload.STREAM_CODEC)
        PayloadTypeRegistry.clientboundPlay().register(PhantomDismountAllowPayload.TYPE, PhantomDismountAllowPayload.STREAM_CODEC)
        PayloadTypeRegistry.clientboundPlay().register(PhantomRosterListPayload.TYPE, PhantomRosterListPayload.STREAM_CODEC)
        PayloadTypeRegistry.clientboundPlay().register(PhantomServerSettingsPayload.TYPE, PhantomServerSettingsPayload.STREAM_CODEC)

        ServerPlayNetworking.registerGlobalReceiver(PhantomInputPayload.TYPE) { payload, context ->
            context.server().execute {
                val player = context.player()
                val vehicle = player.vehicle
                if (vehicle is TamedPhantomEntity && vehicle.isOwnedBy(player)) {
                    vehicle.setPilotInput(payload.ascending, payload.descending, payload.forward, payload.strafe)
                    vehicle.setPilotAttitude(payload.pitch, payload.roll, payload.tuck, payload.effort, payload.acrobatic)
                    vehicle.setRollBoost(payload.boost)
                    if (payload.impactGust) {
                        PhantomImpactGust.tryBlast(
                            vehicle,
                            net.minecraft.world.phys.Vec3(payload.impactX.toDouble(), payload.impactY.toDouble(), payload.impactZ.toDouble()),
                            payload.boost,
                        )
                    }
                    if (payload.sharpStop) {
                        PhantomImpactGust.trySharpStop(vehicle, payload.sharpSpeed, payload.boost)
                    }
                    if (payload.rollBurst) {
                        TamedPhantomsApi.rollBurst(vehicle)
                        PhantomZoomBurst.onRoll(vehicle)
                    }
                }
            }
        }
        ServerPlayNetworking.registerGlobalReceiver(PhantomFlightSpeedPayload.TYPE) { payload, context ->
            context.server().execute {
                OwnerFlightSpeed.set(context.player().uuid, payload.speed.toDouble())
            }
        }
        ServerPlayNetworking.registerGlobalReceiver(PhantomRosterPayload.TYPE) { _, context ->
            context.server().execute {
                PhantomRoster.report(context.player())
            }
        }
        ServerPlayNetworking.registerGlobalReceiver(PhantomRosterSummonPayload.TYPE) { payload, context ->
            context.server().execute {
                com.tamedphantoms.mod.entity.PhantomOwnerRecall.requestSummon(context.player(), payload.id)
            }
        }
        ServerPlayNetworking.registerGlobalReceiver(PhantomScreamPayload.TYPE) { _, context ->
            context.server().execute {
                val player = context.player()
                val vehicle = player.vehicle
                if (vehicle is TamedPhantomEntity) TamedPhantomsApi.riderCry(vehicle, player)
            }
        }
        ServerPlayNetworking.registerGlobalReceiver(PhantomLoopPayload.TYPE) { _, context ->
            context.server().execute {
                val player = context.player()
                val vehicle = player.vehicle
                if (vehicle is TamedPhantomEntity) vehicle.tryOwnerLoop(player)
            }
        }
        ServerPlayNetworking.registerGlobalReceiver(PhantomZoomPayload.TYPE) { _, context ->
            context.server().execute {
                PhantomZoomBurst.onRequest(context.player())
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
