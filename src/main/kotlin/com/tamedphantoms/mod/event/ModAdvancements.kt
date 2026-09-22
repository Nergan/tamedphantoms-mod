package com.tamedphantoms.mod.event

import com.tamedphantoms.mod.TamedPhantomsMod
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

/**
 * Выдаёт два кастомных достижения — "Крылатый питомец" (приручение) и
 * "Больше не в ответе" (освобождение).
 *
 * Намеренно НЕ реализован через кастомный `SimpleCriterionTrigger` (это
 * потребовало бы Codec-описания триггера — часть API, которую в этот раз
 * не удалось перепроверить поиском, см. README "История правок"). Вместо
 * этого достижения (см. JSON в data/tamedphantoms/advancement) используют
 * критерий со штатным ванильным триггером `minecraft:impossible` (специально
 * существующий триггер, который никогда не срабатывает "естественно" —
 * ровно для случаев вроде этого, когда достижение выдаётся модом/командой
 * напрямую) и выдаются напрямую через `PlayerAdvancements#award`, стабильный
 * и давно не менявшийся публичный API.
 */
object ModAdvancements {

    private val TAME_PHANTOM_ID = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "tame_phantom")
    private val RELEASE_PHANTOM_ID = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "release_phantom")
    private val WINGED_BEAST_ID = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "winged_beast")

    fun grantTamePhantom(player: ServerPlayer) = grant(player, TAME_PHANTOM_ID, "tamed_phantom")

    fun grantReleasePhantom(player: ServerPlayer) = grant(player, RELEASE_PHANTOM_ID, "released_phantom")

    fun grantWingedBeast(player: ServerPlayer) = grant(player, WINGED_BEAST_ID, "screamed")

    private fun grant(player: ServerPlayer, advancementId: ResourceLocation, criterion: String) {
        try {
            val holder = player.server.advancements.get(advancementId)
            if (holder == null) {
                TamedPhantomsMod.LOGGER.warn("Достижение {} не загружено — проверьте JSON в data/tamedphantoms/advancement", advancementId)
                return
            }
            player.advancements.award(holder, criterion)
        } catch (t: Throwable) {
            TamedPhantomsMod.LOGGER.warn("Не удалось выдать достижение {}", advancementId, t)
        }
    }
}
