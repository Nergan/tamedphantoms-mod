package com.tamedphantoms.mod.gametest

import com.tamedphantoms.mod.TamedPhantomsMod
import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.entity.ModEntities
import com.tamedphantoms.mod.entity.PhantomFear
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.event.PhantomGuideHandler
import com.tamedphantoms.mod.event.PhantomLeashTie
import com.tamedphantoms.mod.platform.storedData
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate

/** Только для `runGameTestServer`: в jar этот пакет не попадает. */
@GameTestHolder(TamedPhantomsMod.MOD_ID)
@PrefixGameTestTemplate(false)
object PhantomGameTests {

    private const val TEMPLATE = "empty"
    private const val GUIDE_TAG = "tamedphantoms.received_guide"

    private fun tamedBy(helper: GameTestHelper, owner: Player, at: BlockPos): TamedPhantomEntity {
        val pet = helper.spawn(ModEntities.TAMED_PHANTOM.get(), at)
        pet.tameTo(owner)
        pet.isNoAi = true
        return pet
    }

    @GameTest(template = TEMPLATE)
    @JvmStatic
    fun immortalityWearsOffWhileDisabled(helper: GameTestHelper) {
        helper.assertTrue(!ServerConfig.immortalPhantoms(), "immortal_phantoms should be off by default")
        val pet = tamedBy(helper, helper.makeMockPlayer(GameType.SURVIVAL), BlockPos(2, 2, 2))
        pet.isNoAi = false
        pet.immortal = true
        helper.succeedWhen {
            helper.assertTrue(!pet.immortal, "an immortal phantom should become ordinary while the option is off")
            helper.assertTrue(!pet.isCurrentlyGlowing, "the immortal glow should go away")
        }
    }

    @GameTest(template = TEMPLATE)
    @JvmStatic
    fun immortalPhantomTiesToFence(helper: GameTestHelper) {
        val owner = helper.makeMockPlayer(GameType.SURVIVAL)
        val fence = BlockPos(1, 1, 1)
        helper.setBlock(fence, Blocks.OAK_FENCE)
        val pet = tamedBy(helper, owner, BlockPos(3, 2, 3))
        pet.immortal = true
        pet.setLeashedTo(owner, true)
        helper.assertTrue(pet.leashHolder === owner, "setup: the lead should be in the player's hand")
        val result = PhantomLeashTie.onUseBlock(owner, helper.level, helper.absolutePos(fence))
        helper.assertTrue(result != null, "a click on the fence should be handled")
        helper.assertTrue(pet.leashHolder is LeashFenceKnotEntity, "the phantom should be tied to the fence, holder ${pet.leashHolder}")
        helper.succeed()
    }

    @GameTest(template = TEMPLATE)
    @JvmStatic
    fun sittingPhantomTiesToFence(helper: GameTestHelper) {
        val owner = helper.makeMockPlayer(GameType.SURVIVAL)
        val fence = BlockPos(1, 1, 1)
        helper.setBlock(fence, Blocks.OAK_FENCE)
        val pet = tamedBy(helper, owner, BlockPos(3, 1, 3))
        pet.setOrderedToSit(true, playEffects = false)
        pet.setLeashedTo(owner, true)
        PhantomLeashTie.onUseBlock(owner, helper.level, helper.absolutePos(fence))
        helper.assertTrue(pet.leashHolder is LeashFenceKnotEntity, "a sitting phantom should be tied too, holder ${pet.leashHolder}")
        helper.succeed()
    }

    @GameTest(template = TEMPLATE)
    @JvmStatic
    fun guideIsNotGivenAgainAfterRespawn(helper: GameTestHelper) {
        val before = helper.makeMockPlayer(GameType.SURVIVAL)
        val after = helper.makeMockPlayer(GameType.SURVIVAL)
        before.storedData.putBoolean(GUIDE_TAG, true)
        PhantomGuideHandler.copyTo(after, before)
        helper.assertTrue(after.storedData.getBoolean(GUIDE_TAG), "the respawned player should keep the received-guide mark")
        helper.succeed()
    }

    /**
     * Площадку теста обносят барьеры, улететь далеко дикий не может.
     * Проверяется то, что задаёт мод: точка полёта уходит в сторону и вверх от ручного.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    @JvmStatic
    fun wildPhantomHeadsAwayFromTamed(helper: GameTestHelper) {
        val pet = tamedBy(helper, helper.makeMockPlayer(GameType.SURVIVAL), BlockPos(2, 2, 2))
        val wild = helper.spawn(EntityType.PHANTOM, BlockPos(2, 4, 2))
        wild.isInvulnerable = true
        val field = Phantom::class.java.getDeclaredField("moveTargetPoint").apply { isAccessible = true }
        helper.succeedWhen {
            helper.assertTrue(PhantomFear.protector(wild) === pet, "the tamed phantom should be seen as a protector")
            val target = field.get(wild) as Vec3
            val away = Math.hypot(target.x - pet.x, target.z - pet.z)
            helper.assertTrue(away >= 20.0, "the wild phantom should head at least 20 blocks aside, got $away")
            helper.assertTrue(target.y >= pet.y + 8.0, "the wild phantom should head above the tamed one, got ${target.y - pet.y}")
        }
    }
}
