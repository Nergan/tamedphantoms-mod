package com.tamedphantoms.mod.loader.fabric

import com.tamedphantoms.mod.TamedPhantomsMod
import com.tamedphantoms.mod.client.ClientFlightSpeedSender
import com.tamedphantoms.mod.client.ClientPhantomInputSender
import com.tamedphantoms.mod.client.ModKeyMappings
import com.tamedphantoms.mod.client.render.TamedPhantomRenderer
import com.tamedphantoms.mod.client.render.VanillaPhantomRedEyesLayer
import com.tamedphantoms.mod.client.texture.PhantomTextureProcessor
import com.tamedphantoms.mod.entity.ModEntities
import com.tamedphantoms.mod.event.PhantomDismount
import com.tamedphantoms.mod.network.PhantomDismountAllowPayload
import com.tamedphantoms.mod.client.PhantomRosterClient
import com.tamedphantoms.mod.client.PhantomRosterScreen
import com.tamedphantoms.mod.network.PhantomRosterListPayload
import com.tamedphantoms.mod.platform.ModNetwork
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import me.shedaniel.clothconfig2.gui.ClothConfigScreen
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.PhantomRenderer
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.world.entity.EntityType

class TamedPhantomsFabricClient : ClientModInitializer {

    override fun onInitializeClient() {
        ClientPhantomInputSender.init()
        ClientFlightSpeedSender.init()
        ModNetwork.sendToServer = { ClientPlayNetworking.send(it) }

        KeyBindingHelper.registerKeyBinding(ModKeyMappings.FLY_UP)
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.FLY_DOWN)
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.SCREAM)
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.LOOP)
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.ROLL_LEFT)
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.ROLL_RIGHT)
        ModKeyMappings.init()

        EntityRendererRegistry.register(ModEntities.TAMED_PHANTOM.get(), ::TamedPhantomRenderer)
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register { entityType, renderer, helper, _ ->
            if (entityType == EntityType.PHANTOM && renderer is PhantomRenderer) {
                helper.register(VanillaPhantomRedEyesLayer(renderer))
            }
        }
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(object : SimpleSynchronousResourceReloadListener {
            override fun getFabricId(): ResourceLocation =
                ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "phantom_textures")

            override fun onResourceManagerReload(resourceManager: ResourceManager) {
                PhantomTextureProcessor.resetGeneratedTextures()
                PhantomTextureProcessor.prepareEyeTextures()
                com.tamedphantoms.mod.client.PhantomCloudWisp.forget()
            }
        })

        ClientTickEvents.END_CLIENT_TICK.register {
            ClientPhantomInputSender.onClientTick()
            ClientFlightSpeedSender.onClientTick()
            PhantomRosterOpener.onTick()
        }
        com.tamedphantoms.mod.entity.TamedPhantomEntity.clientAfterTick = { phantom ->
            com.tamedphantoms.mod.client.PhantomDiveWind.update(phantom)
            com.tamedphantoms.mod.client.PhantomCloudWisp.afterClientTick(phantom)
        }

        ClientPlayNetworking.registerGlobalReceiver(PhantomDismountAllowPayload.TYPE) { _, context ->
            context.client().execute { PhantomDismount.grantClientDismount() }
        }
        ClientPlayNetworking.registerGlobalReceiver(PhantomServerSettingsPayload.TYPE) { payload, context ->
            context.client().execute { FabricSettings.apply(payload) }
        }
        ClientPlayNetworking.registerGlobalReceiver(PhantomRosterListPayload.TYPE) { payload, context ->
            context.client().execute { PhantomRosterClient.accept(payload.rows) }
        }
    }
}

/** Вкладка «Где мои фантомы» сразу открывает список, а не кнопку в строке настроек. */
private object PhantomRosterOpener {
    private var armed = true

    fun onTick() {
        val minecraft = Minecraft.getInstance()
        val screen = minecraft.screen
        if (screen is PhantomRosterScreen) return
        if (System.nanoTime() < PhantomRosterClient.quietUntil) return
        if (screen !is ClothConfigScreen) {
            armed = true
            return
        }
        val want = Component.translatable("tamedphantoms.configuration.roster").string
        if (screen.selectedCategory.string != want) {
            armed = true
            return
        }
        if (!armed || minecraft.player == null) return
        armed = false
        minecraft.setScreen(PhantomRosterScreen(screen))
    }
}
