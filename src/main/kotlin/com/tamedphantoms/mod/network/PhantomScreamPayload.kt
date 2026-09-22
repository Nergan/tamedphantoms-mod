package com.tamedphantoms.mod.network

import com.tamedphantoms.mod.TamedPhantomsMod
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/** Хозяин просит крик. Сервер сам проверяет место, владельца и перезарядку. */
object PhantomScreamPayload : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<PhantomScreamPayload> = TYPE

    val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "phantom_scream")
    val TYPE: CustomPacketPayload.Type<PhantomScreamPayload> = CustomPacketPayload.Type(ID)
    val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, PhantomScreamPayload> = StreamCodec.unit(PhantomScreamPayload)
}
