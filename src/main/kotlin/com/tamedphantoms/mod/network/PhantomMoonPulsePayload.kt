package com.tamedphantoms.mod.network

import com.tamedphantoms.mod.TamedPhantomsMod
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/** Полнолунный крик: на секунду луна и туман становятся болезненно жёлтыми. */
object PhantomMoonPulsePayload : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<PhantomMoonPulsePayload> = TYPE

    val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "phantom_moon_pulse")
    val TYPE: CustomPacketPayload.Type<PhantomMoonPulsePayload> = CustomPacketPayload.Type(ID)
    val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, PhantomMoonPulsePayload> =
        StreamCodec.unit(PhantomMoonPulsePayload)
}
