package com.tamedphantoms.mod.loader.fabric

import com.tamedphantoms.mod.TamedPhantomsMod
import com.tamedphantoms.mod.config.RideExtras
import com.tamedphantoms.mod.config.ServerConfig
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
    val insomniaDays: Int,
    val phantomGroupMultiplier: Double,
    val phantomSize: Int,
    val immortalPhantoms: Boolean,
    val zoomBoost: Double = ServerConfig.DEFAULT_ZOOM_BOOST,
    val rollBoostLimit: Double = ServerConfig.DEFAULT_ROLL_BOOST_LIMIT,
    val rollBoostStep: Double = ServerConfig.DEFAULT_ROLL_BOOST_STEP,
    val rollBoostClimbDrain: Double = ServerConfig.DEFAULT_ROLL_CLIMB_DRAIN,
    val rollBoostSpin: Double = ServerConfig.DEFAULT_ROLL_SPIN,
    val rollBoostMinTicks: Int = ServerConfig.DEFAULT_ROLL_MIN_TICKS,
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
                buf.writeVarInt(payload.insomniaDays)
                buf.writeDouble(payload.phantomGroupMultiplier)
                buf.writeVarInt(payload.phantomSize)
                buf.writeBoolean(payload.immortalPhantoms)
                if (RideExtras.enabled) {
                    buf.writeDouble(payload.zoomBoost)
                    buf.writeDouble(payload.rollBoostLimit)
                    buf.writeDouble(payload.rollBoostStep)
                    buf.writeDouble(payload.rollBoostClimbDrain)
                    buf.writeDouble(payload.rollBoostSpin)
                    buf.writeVarInt(payload.rollBoostMinTicks)
                }
            },
            { buf ->
                val extras = RideExtras.enabled
                PhantomServerSettingsPayload(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readVarInt(),
                    buf.readDouble(),
                    buf.readVarInt(),
                    buf.readDouble(),
                    buf.readVarInt(),
                    buf.readBoolean(),
                    if (extras) buf.readDouble() else ServerConfig.DEFAULT_ZOOM_BOOST,
                    if (extras) buf.readDouble() else ServerConfig.DEFAULT_ROLL_BOOST_LIMIT,
                    if (extras) buf.readDouble() else ServerConfig.DEFAULT_ROLL_BOOST_STEP,
                    if (extras) buf.readDouble() else ServerConfig.DEFAULT_ROLL_CLIMB_DRAIN,
                    if (extras) buf.readDouble() else ServerConfig.DEFAULT_ROLL_SPIN,
                    if (extras) buf.readVarInt() else ServerConfig.DEFAULT_ROLL_MIN_TICKS,
                )
            },
        )
    }
}
