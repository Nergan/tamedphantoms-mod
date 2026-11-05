package com.tamedphantoms.mod.network

import com.tamedphantoms.mod.TamedPhantomsMod
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/** Кнопка «Призвать» в списке фантомов. */
data class PhantomRosterSummonPayload(val id: String) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<PhantomRosterSummonPayload> = TYPE

    companion object {
        val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "phantom_roster_summon")
        val TYPE: CustomPacketPayload.Type<PhantomRosterSummonPayload> = CustomPacketPayload.Type(ID)
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, PhantomRosterSummonPayload> = StreamCodec.of(
            { buf, payload -> buf.writeUtf(payload.id.take(64)) },
            { buf -> PhantomRosterSummonPayload(buf.readUtf(64)) },
        )
    }
}
