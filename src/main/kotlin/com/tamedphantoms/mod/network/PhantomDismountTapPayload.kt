package com.tamedphantoms.mod.network

import com.tamedphantoms.mod.TamedPhantomsMod
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/** Клиент сообщает о новом нажатии Shift верхом. Сервер сам решает, предупреждать или слезать. */
object PhantomDismountTapPayload : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<PhantomDismountTapPayload> = TYPE

    val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "phantom_dismount_tap")
    val TYPE: CustomPacketPayload.Type<PhantomDismountTapPayload> = CustomPacketPayload.Type(ID)
    val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, PhantomDismountTapPayload> =
        StreamCodec.unit(PhantomDismountTapPayload)
}
