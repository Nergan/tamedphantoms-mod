package com.tamedphantoms.mod

import com.tamedphantoms.mod.config.NeoForgeClientConfig
import com.tamedphantoms.mod.config.NeoForgeServerConfig
import com.tamedphantoms.mod.event.ModSetup
import com.tamedphantoms.mod.loader.NeoForgeGameEvents
import com.tamedphantoms.mod.loader.NeoForgeRegistries
import com.tamedphantoms.mod.network.ModNetworking
import com.tamedphantoms.mod.platform.ModAccess
import com.tamedphantoms.mod.platform.ModNetwork
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModList
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.neoforge.network.PacketDistributor

/**
 * Точка входа NeoForge.
 *
 * Обычный класс, а не Kotlin `object`: NeoForge отдаёт [ModContainer]
 * только параметром конструктора, и без него не зарегистрировать конфиг.
 */
@Mod(TamedPhantomsMod.MOD_ID)
class TamedPhantomsNeoForge(modEventBus: IEventBus, modContainer: ModContainer) {

    init {
        TamedPhantomsMod.LOGGER.info("Инициализация мода Tamed Phantoms ({})", TamedPhantomsMod.MOD_ID)

        NeoForgeRegistries.register(modEventBus)
        modContainer.registerConfig(ModConfig.Type.SERVER, NeoForgeServerConfig.SPEC)
        modContainer.registerConfig(ModConfig.Type.CLIENT, NeoForgeClientConfig.SPEC)
        NeoForgeServerConfig.bind()
        NeoForgeClientConfig.bind()

        ModAccess.persistentData = { it.persistentData }
        ModAccess.addGoal = { mob, priority, goal -> mob.goalSelector.addGoal(priority, goal) }
        ModNetwork.isModLoaded = { ModList.get().isLoaded(it) }
        ModNetwork.sendToServer = { PacketDistributor.sendToServer(it) }
        ModNetwork.sendToPlayer = { player, payload -> PacketDistributor.sendToPlayer(player, payload) }
        ModNetwork.sendToDimension = { level, payload ->
            PacketDistributor.sendToPlayersInDimension(level, payload)
        }

        ModSetup.init(modEventBus, modContainer)
        ModNetworking.init(modEventBus)
        NeoForgeGameEvents.register()
    }
}
