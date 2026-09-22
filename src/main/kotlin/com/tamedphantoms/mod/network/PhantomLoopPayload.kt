package com.tamedphantoms.mod.network

import com.tamedphantoms.mod.TamedPhantomsMod
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/** Клиент увидел полный оборот. Сервер проверяет, что набор высоты уже почти замкнулся. */
object PhantomLoopPayload : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<PhantomLoopPayload> = TYPE

    val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "phantom_loop")
    val TYPE: CustomPacketPayload.Type<PhantomLoopPayload> = CustomPacketPayload.Type(ID)
    val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, PhantomLoopPayload> = StreamCodec.unit(PhantomLoopPayload)
}
