package com.tamedphantoms.mod.network

import com.tamedphantoms.mod.TamedPhantomsMod
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/** Пилот рванул вверх из петли. Сервер показывает порыв ветра всем вокруг, см. [com.tamedphantoms.mod.util.PhantomZoomBurst]. */
object PhantomZoomPayload : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<PhantomZoomPayload> = TYPE

    val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "phantom_zoom")
    val TYPE: CustomPacketPayload.Type<PhantomZoomPayload> = CustomPacketPayload.Type(ID)
    val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, PhantomZoomPayload> = StreamCodec.unit(PhantomZoomPayload)
}
