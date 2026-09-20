package com.tamedphantoms.mod.entity

import com.tamedphantoms.mod.util.PhantomOwnerTeleport
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.FluidTags
import net.minecraft.world.entity.RelativeMovement
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
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

    fun remember(player: Player, phantom: TamedPhantomEntity) {
        val list = player.persistentData.getList(TAG_OWNED, Tag.TAG_COMPOUND.toInt())
        val next = ListTag()
        for (i in 0 until list.size) {
            val tag = list.getCompound(i)
            if (!tag.hasUUID("U") || tag.getUUID("U") != phantom.uuid) {
                next.add(tag)
            }
        }
        val entry = CompoundTag()
        entry.putUUID("U", phantom.uuid)
        entry.putString("D", phantom.level().dimension().location().toString())
        entry.putDouble("X", phantom.x)
        entry.putDouble("Y", phantom.y)
        entry.putDouble("Z", phantom.z)
        next.add(entry)
        player.persistentData.put(TAG_OWNED, next)
    }

    fun copyTo(newPlayer: Player, oldPlayer: Player) {
        val tag = oldPlayer.persistentData.get(TAG_OWNED) ?: return
        newPlayer.persistentData.put(TAG_OWNED, tag.copy())
    }

    fun forget(player: Player, phantomId: UUID) {
        val list = player.persistentData.getList(TAG_OWNED, Tag.TAG_COMPOUND.toInt())
        val next = ListTag()
        for (i in 0 until list.size) {
            val tag = list.getCompound(i)
            if (!tag.hasUUID("U") || tag.getUUID("U") != phantomId) {
                next.add(tag)
            }
        }
        player.persistentData.put(TAG_OWNED, next)
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
        val stored = owner.persistentData.getList(TAG_OWNED, Tag.TAG_COMPOUND.toInt())
        for (i in 0 until stored.size) {
            val tag = stored.getCompound(i)
            if (!tag.hasUUID("U")) continue
            val id = tag.getUUID("U")
            if (id in seen) continue
            val dim = ResourceLocation.tryParse(tag.getString("D")) ?: continue
            val level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dim)) ?: continue
            val pos = BlockPos.containing(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"))
            level.getChunk(pos)
            val phantom = level.getEntity(id) as? TamedPhantomEntity ?: continue
            tryTeleportToOwner(phantom, owner)
        }
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
        return PhantomOwnerTeleport.isSafeSpace(inBorder, noCollision, inLava)
    }
}
