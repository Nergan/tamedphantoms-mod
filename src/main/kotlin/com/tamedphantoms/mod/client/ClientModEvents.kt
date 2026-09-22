package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.client.render.TamedPhantomRenderer
import com.tamedphantoms.mod.client.render.VanillaPhantomRedEyesLayer
import com.tamedphantoms.mod.client.texture.PhantomTextureProcessor
import com.tamedphantoms.mod.entity.ModEntities
import net.minecraft.client.renderer.entity.PhantomRenderer
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraft.world.entity.EntityType
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.NeoForge

/**
 * Клиентская настройка. Вызывается условно из [com.tamedphantoms.mod.event.ModSetup]
 * — только на клиенте, никогда на выделенном сервере (где классов вроде
 * [EntityRenderersEvent] попросту не существует в classpath).
 */
object ClientModEvents {

    fun init(modBus: IEventBus, modContainer: ModContainer) {
        modBus.addListener(::onRegisterRenderers)
        modBus.addListener(::onAddLayers)
        modBus.addListener(::onRegisterKeyMappings)
        modBus.addListener(::onClientSetup)
        modBus.addListener(::onRegisterReloadListeners)
        ClientPhantomInputSender.init()
        ClientFlightSpeedSender.init()
        NeoForge.EVENT_BUS.register(ClientPhantomSoundHandler)
        NeoForge.EVENT_BUS.register(PhantomRideBank)
        NeoForge.EVENT_BUS.register(PhantomMoonLight)
        com.tamedphantoms.mod.entity.TamedPhantomEntity.clientAfterTick = { phantom ->
            PhantomDiveWind.update(phantom)
        }

        // Подключаем встроенный экран настроек NeoForge: Mods -> Tamed Phantoms
        // -> кнопка "Config". Он сам строит интерфейс по зарегистрированному
        // ServerConfig.SPEC, ClientConfig.SPEC и переводам ключей вида
        // "tamedphantoms.configuration.*" (см. ServerConfig.kt / ClientConfig.kt).
        // Для конфигов типа SERVER,
        // если игрок подключён к чужому серверу (не хостит локально сам),
        // экран показывает значения как read-only — это поведение самого
        // NeoForge, не мода.
        modContainer.registerExtensionPoint(
            IConfigScreenFactory::class.java,
            IConfigScreenFactory { container, currentScreen -> modConfigurationScreen(container, currentScreen) },
        )
    }

    private fun onRegisterRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerEntityRenderer(ModEntities.TAMED_PHANTOM.get(), ::TamedPhantomRenderer)
    }

    private fun onAddLayers(event: EntityRenderersEvent.AddLayers) {
        val renderer = event.getRenderer(EntityType.PHANTOM) as? PhantomRenderer ?: return
        renderer.addLayer(VanillaPhantomRedEyesLayer(renderer))
    }

    private fun onRegisterKeyMappings(event: RegisterKeyMappingsEvent) {
        event.register(ModKeyMappings.FLY_UP)
        event.register(ModKeyMappings.FLY_DOWN)
        event.register(ModKeyMappings.SCREAM)
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        event.enqueueWork { PhantomTextureProcessor.prepareEyeTextures() }
    }

    private fun onRegisterReloadListeners(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(ResourceManagerReloadListener { _: ResourceManager ->
            PhantomTextureProcessor.resetGeneratedTextures()
            PhantomTextureProcessor.prepareEyeTextures()
        })
    }
}
