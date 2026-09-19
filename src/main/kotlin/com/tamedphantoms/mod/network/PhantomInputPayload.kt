package com.tamedphantoms.mod.network

import com.tamedphantoms.mod.TamedPhantomsMod
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/**
 * Состояние клавиш "Взлёт"/"Снижение" для полёта на фантоме, отправляется
 * с клиента на сервер (см. [com.tamedphantoms.mod.client.ClientPhantomInputSender]).
 *
 * ЭТО САМАЯ РИСКОВАННАЯ НОВАЯ ЧАСТЬ В ЭТОМ РАУНДЕ ПРАВОК: сетевой API
 * NeoForge (`CustomPacketPayload`/`RegisterPayloadHandlersEvent`) в
 * прошлом несколько раз менялся между версиями, а полноценный поиск для
 * подтверждения точной сигнатуры под 1.21.1 сейчас недоступен (см. README,
 * раздел "История правок" — временный сбой инструмента поиска). Код ниже
 * написан по общей, устоявшейся форме этого API, но если сборка упадёт
 * именно на файлах в пакете `network/` — это самое вероятное место,
 * пришлите лог так же, как раньше, и это быстро поправится.
 */
data class PhantomInputPayload(val ascending: Boolean, val descending: Boolean) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "phantom_input")
        val TYPE: CustomPacketPayload.Type<PhantomInputPayload> = CustomPacketPayload.Type(ID)

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, PhantomInputPayload> = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            PhantomInputPayload::ascending,
            ByteBufCodecs.BOOL,
            PhantomInputPayload::descending,
            ::PhantomInputPayload,
        )
    }
}
