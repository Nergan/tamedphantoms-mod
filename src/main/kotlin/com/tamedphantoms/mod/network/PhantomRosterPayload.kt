package com.tamedphantoms.mod.network

import com.tamedphantoms.mod.TamedPhantomsMod
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/** Клиент просит список своих фантомов. Сервер отвечает сообщением в чат. */
object PhantomRosterPayload : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<PhantomRosterPayload> = TYPE

    val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "phantom_roster")
    val TYPE: CustomPacketPayload.Type<PhantomRosterPayload> = CustomPacketPayload.Type(ID)
    val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, PhantomRosterPayload> = StreamCodec.unit(PhantomRosterPayload)
}
