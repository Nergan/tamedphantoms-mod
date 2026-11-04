package com.tamedphantoms.mod.entity

import com.tamedphantoms.mod.platform.ModAccess
import com.tamedphantoms.mod.platform.storedData
import com.tamedphantoms.mod.util.PhantomOwnedList
import com.tamedphantoms.mod.util.PhantomOwnerTeleport
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.RelativeMovement
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CampfireBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.UUID

/**
 * Телепорт ручного фантома к хозяину: только в свободный воздух,
 * иначе ждём следующий тик. Помнит последнее место, чтобы после
 * возрождения хозяина подгрузить чанк, где фантом остался.
 */
object PhantomOwnerRecall {
    private const val TAG_OWNED = "tamedphantoms.owned_phantoms"
    private const val TICKET_REFRESH_TICKS = 200L

    /** Фаза поиска пропавшего фантома: 0 рядом, 1 шире. */
    private val loadPhase = HashMap<UUID, Int>()
    private val ticketedAt = HashMap<UUID, Long>()

    fun hasPendingLoads(): Boolean = loadPhase.isNotEmpty() || ticketedAt.isNotEmpty()

    fun remember(player: Player, phantom: TamedPhantomEntity) {
        val list = player.storedData.getList(TAG_OWNED, Tag.TAG_COMPOUND.toInt())
        val ids = ArrayList<UUID?>(list.size)
        for (i in 0 until list.size) {
            ids.add(PhantomIdTag.read(list.getCompound(i)))
        }
        val keep = PhantomOwnedList.keptIds(ids, phantom.uuid)
        val next = ListTag()
        val written = HashSet<UUID>()
        for (i in 0 until list.size) {
            val id = ids[i]
            if (id == null || id !in keep || !written.add(id)) continue
            next.add(list.getCompound(i))
        }
        val entry = CompoundTag()
        PhantomIdTag.write(entry, phantom.uuid)
        entry.putString("D", phantom.level().dimension().location().toString())
        entry.putDouble("X", phantom.x)
        entry.putDouble("Y", phantom.y)
        entry.putDouble("Z", phantom.z)
        entry.putBoolean("S", phantom.isOrderedToSit())
        next.add(entry)
        player.storedData.put(TAG_OWNED, next)
    }

    fun copyTo(newPlayer: Player, oldPlayer: Player) {
        val tag = oldPlayer.storedData.get(TAG_OWNED) ?: return
        newPlayer.storedData.put(TAG_OWNED, tag.copy())
    }

    fun forget(player: Player, phantomId: UUID) {
        loadPhase.remove(phantomId)
        ticketedAt.remove(phantomId)
        val list = player.storedData.getList(TAG_OWNED, Tag.TAG_COMPOUND.toInt())
        val next = ListTag()
        for (i in 0 until list.size) {
            val tag = list.getCompound(i)
            val id = PhantomIdTag.read(tag)
            if (id != null && id != phantomId) {
                next.add(tag)
            }
        }
        player.storedData.put(TAG_OWNED, next)
    }

    fun tryTeleportToOwner(phantom: TamedPhantomEntity, owner: Player): Boolean {
        val sameLevel = phantom.level() === owner.level()
        val distanceSq = if (sameLevel) phantom.distanceToSqr(owner) else Double.MAX_VALUE
        if (!PhantomOwnerTeleport.shouldTryTeleport(
                distanceSq,
                owner.isAlive,
                phantom.isOrderedToSit(),
                phantom.isLeashed,
                phantom.isVehicle,
            )
        ) {
            return false
        }
        val dest = owner.level() as? ServerLevel ?: return false
        val pos = findSafePosition(dest, phantom, owner) ?: return false
        val moved = if (phantom.level() !== dest) {
            phantom.teleportTo(dest, pos.x, pos.y, pos.z, emptySet<RelativeMovement>(), phantom.yRot, phantom.xRot)
        } else {
            phantom.teleportTo(pos.x, pos.y, pos.z)
            true
        }
        if (moved) {
            phantom.deltaMovement = Vec3.ZERO
            phantom.fallDistance = 0.0f
            remember(owner, phantom)
        }
        return moved
    }

    fun recallOwned(owner: Player) {
        if (owner.level().isClientSide || !owner.isAlive) return
        val server = owner.server ?: return
        val seen = HashSet<UUID>()
        for (level in server.allLevels) {
            for (phantom in loadedOwned(level, owner)) {
                seen += phantom.uuid
                tryTeleportToOwner(phantom, owner)
            }
        }
        val stored = owner.storedData.getList(TAG_OWNED, Tag.TAG_COMPOUND.toInt())
        for (i in 0 until stored.size) {
            val tag = stored.getCompound(i)
            if (tag.getBoolean("S")) continue
            val id = PhantomIdTag.read(tag) ?: continue
            if (id in seen) continue
            val dim = ResourceLocation.tryParse(tag.getString("D")) ?: continue
            val level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dim)) ?: continue
            val pos = BlockPos.containing(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"))
            val phantom = level.getEntity(id) as? TamedPhantomEntity
            if (phantom == null) {
                if (miss(level, id, pos)) forget(owner, id)
            } else {
                bring(phantom, owner)
            }
        }
    }

    /**
     * Хозяин тикает даже в другом чанке. Сидящий фантом остаётся на месте
     * и его чанк ради этого не грузится.
     */
    fun recallUnloaded(owner: Player) {
        if (owner.level().isClientSide || !owner.isAlive) return
        val server = owner.server ?: return
        val stored = owner.storedData.getList(TAG_OWNED, Tag.TAG_COMPOUND.toInt())
        val pending = ArrayList<CompoundTag>(stored.size)
        for (i in 0 until stored.size) {
            pending.add(stored.getCompound(i).copy())
        }
        for (tag in pending) {
            if (tag.getBoolean("S")) continue
            val id = PhantomIdTag.read(tag) ?: continue
            val dim = ResourceLocation.tryParse(tag.getString("D")) ?: continue
            val level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dim)) ?: continue
            val pos = BlockPos.containing(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"))
            val phantom = level.getEntity(id) as? TamedPhantomEntity
            if (phantom == null) {
                if (miss(level, id, pos)) forget(owner, id)
                continue
            }
            bring(phantom, owner)
        }
    }

    fun roster(owner: ServerPlayer): List<PhantomRosterRow> {
        val server = owner.server ?: return emptyList()
        val stored = owner.storedData.getList(TAG_OWNED, Tag.TAG_COMPOUND.toInt())
        val rows = ArrayList<PhantomRosterRow>()
        val seen = HashSet<UUID>()
        for (i in 0 until stored.size) {
            val tag = stored.getCompound(i)
            val id = PhantomIdTag.read(tag) ?: continue
            if (!seen.add(id)) continue
            val dimName = tag.getString("D")
            val dim = ResourceLocation.tryParse(dimName) ?: continue
            val level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dim))
            val pos = BlockPos.containing(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"))
            val live = if (level != null && level.hasChunk(pos.x shr 4, pos.z shr 4)) {
                level.getEntity(id) as? TamedPhantomEntity
            } else {
                null
            }
            if (live != null) {
                if (!live.isAlive || !live.tamed || !live.isOwnedBy(owner)) continue
                rows.add(
                    PhantomRosterRow(
                        live.isOrderedToSit(),
                        live.blockPosition().x,
                        live.blockPosition().y,
                        live.blockPosition().z,
                        dimName,
                    ),
                )
            } else {
                rows.add(PhantomRosterRow(tag.getBoolean("S"), pos.x, pos.y, pos.z, dimName))
            }
        }
        return rows
    }

    /**
     * Фантом далеко от хозяина. Поводок снимаем: иначе он останется в выгруженном чанке.
     * Сидящий уже отсеян. Рядом с живым хозяином не трогаем.
     */
    private fun bring(phantom: TamedPhantomEntity, owner: Player) {
        loadPhase.remove(phantom.uuid)
        ticketedAt.remove(phantom.uuid)
        if (!phantom.isAlive || !phantom.tamed || !phantom.isOwnedBy(owner)) {
            forget(owner, phantom.uuid)
            return
        }
        if (phantom.isOrderedToSit()) {
            remember(owner, phantom)
            return
        }
        val same = phantom.level() === owner.level()
        val near = same && phantom.distanceToSqr(owner) <= PhantomOwnerTeleport.DISTANCE * PhantomOwnerTeleport.DISTANCE
        if (near) return
        if (phantom.isLeashed) phantom.dropLeash(true, true)
        tryTeleportToOwner(phantom, owner)
    }

    /**
     * Сущности выгруженного чанка нет в поиске по id, пока чанк не начнёт тикать.
     * Сначала держим ближайшие чанки, и только если фантома там нет — широкое кольцо.
     * @return true, когда широкое кольцо уже тикает, а фантома нет: его больше не существует.
     */
    private fun miss(level: ServerLevel, id: UUID, pos: BlockPos): Boolean {
        val phase = loadPhase[id] ?: 0
        val radius = PhantomOwnedList.radiusFor(phase)
        val (next, drop) = PhantomOwnedList.afterMiss(phase, areaEntityTicking(level, pos, radius))
        if (drop) {
            loadPhase.remove(id)
            ticketedAt.remove(id)
            return true
        }
        if (next != phase) loadPhase[id] = next
        val useRadius = PhantomOwnedList.radiusFor(loadPhase[id] ?: 0)
        val now = level.gameTime
        val last = ticketedAt[id]
        if (last == null || now - last >= TICKET_REFRESH_TICKS || next != phase) {
            ticketedAt[id] = now
            ModAccess.keepChunkLoaded(level, pos, useRadius)
        }
        return false
    }

    private fun areaEntityTicking(level: ServerLevel, pos: BlockPos, radius: Int): Boolean {
        val cx = pos.x shr 4
        val cz = pos.z shr 4
        for (dx in -radius..radius) {
            for (dz in -radius..radius) {
                val check = BlockPos((cx + dx) shl 4, pos.y, (cz + dz) shl 4)
                if (!level.isPositionEntityTicking(check)) return false
            }
        }
        return true
    }

    private fun loadedOwned(level: ServerLevel, owner: Player): List<TamedPhantomEntity> {
        val border = level.worldBorder
        val box = AABB(
            border.minX,
            level.minBuildHeight.toDouble(),
            border.minZ,
            border.maxX,
            level.maxBuildHeight.toDouble(),
            border.maxZ,
        )
        return level.getEntities(ModEntities.TAMED_PHANTOM.get(), box) { phantom ->
            phantom.isAlive && phantom.tamed && phantom.isOwnedBy(owner)
        }
    }

    private fun findSafePosition(level: Level, phantom: TamedPhantomEntity, owner: Player): Vec3? {
        for ((dx, dy, dz) in PhantomOwnerTeleport.candidateOffsets()) {
            val x = owner.x + dx
            val y = owner.y + dy
            val z = owner.z + dz
            if (isSafe(level, phantom, x, y, z)) {
                return Vec3(x, y, z)
            }
        }
        return null
    }

    private fun isSafe(level: Level, phantom: TamedPhantomEntity, x: Double, y: Double, z: Double): Boolean {
        val box = phantom.getDimensions(phantom.pose).makeBoundingBox(x, y, z)
        val inBorder = level.worldBorder.isWithinBounds(x, z)
        val noCollision = level.noCollision(box)
        val inLava = level.getFluidState(BlockPos.containing(x, y, z)).`is`(FluidTags.LAVA) ||
            level.getFluidState(BlockPos.containing(x, box.minY, z)).`is`(FluidTags.LAVA)
        val harmful = harms(level, box)
        return PhantomOwnerTeleport.isSafeSpace(inBorder, noCollision, inLava, harmful)
    }

    private fun harms(level: Level, box: AABB): Boolean {
        val min = BlockPos.containing(box.minX, box.minY, box.minZ)
        val max = BlockPos.containing(box.maxX, box.maxY, box.maxZ)
        for (pos in BlockPos.betweenClosed(min, max)) {
            if (hurts(level.getBlockState(pos))) return true
        }
        return false
    }

    private fun hurts(state: BlockState): Boolean {
        val block = state.block
        if (block == Blocks.FIRE || block == Blocks.SOUL_FIRE || block == Blocks.CACTUS ||
            block == Blocks.MAGMA_BLOCK || block == Blocks.SWEET_BERRY_BUSH || block == Blocks.WITHER_ROSE ||
            block == Blocks.POWDER_SNOW || block == Blocks.POINTED_DRIPSTONE || block == Blocks.LAVA
        ) {
            return true
        }
        if (state.hasProperty(CampfireBlock.LIT)) {
            return state.getValue(CampfireBlock.LIT)
        }
        return state.fluidState.`is`(FluidTags.LAVA)
    }
}

data class PhantomRosterRow(
    val sitting: Boolean,
    val x: Int,
    val y: Int,
    val z: Int,
    val dimension: String,
)
