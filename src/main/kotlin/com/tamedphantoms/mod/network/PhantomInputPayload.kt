package com.tamedphantoms.mod.network

import com.tamedphantoms.mod.TamedPhantomsMod
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/**
 * Клавиши пилота и поза, которую посчитала модель полёта у него на клиенте
 * (см. [com.tamedphantoms.mod.client.ClientPhantomInputSender]). Шлётся каждый тик верхом:
 * потеря одного пакета не оставляет клавишу «залипшей». Поза идёт остальным игрокам.
 */
data class PhantomInputPayload(
    val ascending: Boolean,
    val descending: Boolean,
    val forward: Float,
    val strafe: Float,
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val tuck: Float = 0f,
    val effort: Float = 0f,
    val acrobatic: Boolean = false,
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "phantom_input")
        val TYPE: CustomPacketPayload.Type<PhantomInputPayload> = CustomPacketPayload.Type(ID)

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, PhantomInputPayload> = StreamCodec.of(
            { buf, payload ->
                buf.writeBoolean(payload.ascending)
                buf.writeBoolean(payload.descending)
                buf.writeFloat(payload.forward)
                buf.writeFloat(payload.strafe)
                buf.writeFloat(payload.pitch)
                buf.writeFloat(payload.roll)
                buf.writeFloat(payload.tuck)
                buf.writeFloat(payload.effort)
                buf.writeBoolean(payload.acrobatic)
            },
            { buf ->
                PhantomInputPayload(
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readBoolean(),
                )
            },
        )
    }
}
