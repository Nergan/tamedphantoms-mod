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
    /** Бонус скорости за бочку, блоки за тик. Сервер рисует по нему шлейф. */
    val boost: Float = 0f,
    /** Пилот врезался в поверхность на полном бонусе. Вектор — скорость удара. */
    val impactGust: Boolean = false,
    val impactX: Float = 0f,
    val impactY: Float = 0f,
    val impactZ: Float = 0f,
    /** Резкая остановка с бонусом. Число — скорость до остановки. */
    val sharpStop: Boolean = false,
    val sharpSpeed: Float = 0f,
    /** Заряженный оборот только что добавил скорость. */
    val rollBurst: Boolean = false,
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
                buf.writeFloat(payload.boost)
                buf.writeBoolean(payload.impactGust)
                buf.writeFloat(payload.impactX)
                buf.writeFloat(payload.impactY)
                buf.writeFloat(payload.impactZ)
                buf.writeBoolean(payload.sharpStop)
                buf.writeFloat(payload.sharpSpeed)
                buf.writeBoolean(payload.rollBurst)
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
                    buf.readFloat(),
                    buf.readBoolean(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readBoolean(),
                    buf.readFloat(),
                    buf.readBoolean(),
                )
            },
        )
    }
}
