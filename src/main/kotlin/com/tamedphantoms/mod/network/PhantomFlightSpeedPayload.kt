package com.tamedphantoms.mod.network

import com.tamedphantoms.mod.TamedPhantomsMod
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/**
 * Клиент сообщает серверу, какую скорость он хочет для своих прирученных
 * фантомов. Сервер всё равно обрежет её своим потолком.
 */
data class PhantomFlightSpeedPayload(val speed: Float) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "flight_speed")
        val TYPE: CustomPacketPayload.Type<PhantomFlightSpeedPayload> = CustomPacketPayload.Type(ID)

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, PhantomFlightSpeedPayload> = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            PhantomFlightSpeedPayload::speed,
            ::PhantomFlightSpeedPayload,
        )
    }
}
