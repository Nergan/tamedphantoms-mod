package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.core.BlockPos
import net.minecraft.tags.BlockTags
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.LeadItem
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult

/**
 * Привязка прирученного фантома к столбу.
 *
 * Ванильный поиск у забора смотрит только 7 блоков вокруг: сидящий фантом
 * на поводке туда часто не попадает. А летающий кружит у лица игрока,
 * и клик по забору приходится в фантома, который тогда снимает поводок.
 * Здесь столб берёт всех фантомов игрока на поводке до [REACH] блоков,
 * а клик в фантома с забором позади вяжет к этому забору.
 */
object PhantomLeashTie {

    private const val REACH = 12.0

    /** Клик по блоку. null — не наш случай, ваниль и другие моды делают своё. */
    fun onUseBlock(player: Player, level: Level, pos: BlockPos): InteractionResult? {
        if (level.isClientSide) return null
        if (!level.getBlockState(pos).`is`(BlockTags.FENCES)) return null
        val phantoms = leashedTo(player, level, pos)
        if (phantoms.isEmpty()) return null
        val knot = LeashFenceKnotEntity.getOrCreateKnot(level, pos)
        knot.playPlacementSound()
        for (phantom in phantoms) {
            phantom.setLeashedTo(knot, true)
        }
        LeadItem.bindPlayerMobs(player, level, pos)
        return InteractionResult.SUCCESS
    }

    /** Столб, на который игрок смотрит сквозь своего фантома, в пределах досягаемости. */
    fun postBehind(player: Player): BlockPos? {
        val level = player.level()
        val eye = player.eyePosition
        val end = eye.add(player.getViewVector(1f).scale(player.blockInteractionRange()))
        val hit = level.clip(ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player))
        if (hit.type != HitResult.Type.BLOCK) return null
        return hit.blockPos.takeIf { level.getBlockState(it).`is`(BlockTags.FENCES) }
    }

    private fun leashedTo(player: Player, level: Level, pos: BlockPos): List<TamedPhantomEntity> =
        level.getEntitiesOfClass(TamedPhantomEntity::class.java, AABB(pos).inflate(REACH)) {
            it.isAlive && it.leashHolder === player
        }
}
