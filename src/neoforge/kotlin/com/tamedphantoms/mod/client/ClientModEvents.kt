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
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.client.event.ViewportEvent
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent
import net.neoforged.neoforge.client.gui.IConfigScreenFactory
import net.neoforged.neoforge.common.NeoForge

object ClientModEvents {

    fun init(modBus: IEventBus, modContainer: ModContainer) {
        modBus.addListener(::onRegisterRenderers)
        modBus.addListener(::onAddLayers)
        modBus.addListener(::onRegisterKeyMappings)
        modBus.addListener(::onClientSetup)
        modBus.addListener(::onRegisterReloadListeners)
        ClientPhantomInputSender.init()
        ClientFlightSpeedSender.init()

        val bus = NeoForge.EVENT_BUS
        bus.addListener { _: ClientTickEvent.Post ->
            ClientPhantomInputSender.onClientTick()
            ClientFlightSpeedSender.onClientTick()
            PhantomMoonLight.onTick()
        }
        bus.addListener { event: PlaySoundEvent ->
            val next = ClientPhantomSoundHandler.adjust(event.sound)
            if (next !== event.sound) {
                event.setSound(next)
            }
        }
        bus.addListener { event: ViewportEvent.ComputeCameraAngles ->
            val angles = PhantomRideBank.adjust(event.camera.entity, event.partialTick.toFloat(), event.pitch, event.roll)
                ?: return@addListener
            event.pitch = angles.pitch
            event.roll = angles.roll
        }
        bus.addListener { event: ViewportEvent.ComputeFogColor ->
            val tint = PhantomMoonLight.tint(event.red, event.green, event.blue) ?: return@addListener
            event.red = tint.red
            event.green = tint.green
            event.blue = tint.blue
        }
        com.tamedphantoms.mod.entity.TamedPhantomEntity.clientAfterTick = { phantom ->
            PhantomDiveWind.update(phantom)
        }

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
