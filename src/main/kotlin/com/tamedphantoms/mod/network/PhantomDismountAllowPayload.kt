package com.tamedphantoms.mod.network

import com.tamedphantoms.mod.TamedPhantomsMod
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/** Сервер уже снял игрока: клиент не должен сажать его обратно. */
object PhantomDismountAllowPayload : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<PhantomDismountAllowPayload> = TYPE

    val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "phantom_dismount_allow")
    val TYPE: CustomPacketPayload.Type<PhantomDismountAllowPayload> = CustomPacketPayload.Type(ID)
    val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, PhantomDismountAllowPayload> =
        StreamCodec.unit(PhantomDismountAllowPayload)
}
