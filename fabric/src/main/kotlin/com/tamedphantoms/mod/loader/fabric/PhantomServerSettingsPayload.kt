package com.tamedphantoms.mod.loader.fabric

import com.tamedphantoms.mod.TamedPhantomsMod
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/** Серверные правила для клиента: потолок скорости, ползунок и книга на клиенте их не читают, но полёт верхом — да. */
data class PhantomServerSettingsPayload(
    val tameItemId: String,
    val releaseItemId: String,
    val tameChance: Double,
    val repelRadius: Double,
    val defendDurationTicks: Int,
    val flightSpeed: Double,
    val acrobaticsStep: Double,
    val screamRadius: Double,
    val screamCooldownSeconds: Int,
    val insomniaDays: Int,
    val phantomGroupMultiplier: Double,
    val phantomSize: Int,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE: CustomPacketPayload.Type<PhantomServerSettingsPayload> =
            CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "server_settings"))

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, PhantomServerSettingsPayload> = StreamCodec.of(
            { buf, payload ->
                buf.writeUtf(payload.tameItemId)
                buf.writeUtf(payload.releaseItemId)
                buf.writeDouble(payload.tameChance)
                buf.writeDouble(payload.repelRadius)
                buf.writeVarInt(payload.defendDurationTicks)
                buf.writeDouble(payload.flightSpeed)
                buf.writeDouble(payload.acrobaticsStep)
                buf.writeDouble(payload.screamRadius)
                buf.writeVarInt(payload.screamCooldownSeconds)
                buf.writeVarInt(payload.insomniaDays)
                buf.writeDouble(payload.phantomGroupMultiplier)
                buf.writeVarInt(payload.phantomSize)
            },
            { buf ->
                PhantomServerSettingsPayload(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readVarInt(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readDouble(),
                    buf.readVarInt(),
                )
            },
        )
    }
}
