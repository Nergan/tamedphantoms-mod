package com.tamedphantoms.mod.network

import com.tamedphantoms.mod.TamedPhantomsMod
import com.tamedphantoms.mod.entity.PhantomRosterRow
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

/** Список фантомов хозяина для экрана координат. */
data class PhantomRosterListPayload(val rows: List<PhantomRosterRow>) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<PhantomRosterListPayload> = TYPE

    companion object {
        private const val MAX_ROWS = 128

        val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "phantom_roster_list")
        val TYPE: CustomPacketPayload.Type<PhantomRosterListPayload> = CustomPacketPayload.Type(ID)
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, PhantomRosterListPayload> = StreamCodec.of(
            { buf, payload ->
                val rows = payload.rows.take(MAX_ROWS)
                buf.writeVarInt(rows.size)
                for (row in rows) {
                    buf.writeUtf(row.name.take(64))
                    buf.writeBoolean(row.sitting)
                    buf.writeInt(row.x)
                    buf.writeInt(row.y)
                    buf.writeInt(row.z)
                    buf.writeUtf(row.dimension.take(128))
                    buf.writeInt(row.bodyDye)
                    buf.writeInt(row.saddleDye)
                    buf.writeBoolean(row.luminous)
                    buf.writeUtf(row.id.take(64))
                }
            },
            { buf ->
                val count = buf.readVarInt().coerceIn(0, MAX_ROWS)
                val rows = ArrayList<PhantomRosterRow>(count)
                repeat(count) {
                    rows.add(
                        PhantomRosterRow(
                            buf.readUtf(64),
                            buf.readBoolean(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readUtf(128),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readBoolean(),
                            buf.readUtf(64),
                        ),
                    )
                }
                PhantomRosterListPayload(rows)
            },
        )
    }
}
