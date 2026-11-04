package com.tamedphantoms.mod.loader.fabric

import com.tamedphantoms.mod.TamedPhantomsMod
import com.tamedphantoms.mod.client.ClientFlightSpeedSender
import com.tamedphantoms.mod.client.PhantomCloudWisp
import com.tamedphantoms.mod.client.ClientPhantomInputSender
import com.tamedphantoms.mod.client.ModKeyMappings
import com.tamedphantoms.mod.client.PhantomMoonLight
import com.tamedphantoms.mod.client.render.TamedPhantomRenderer
import com.tamedphantoms.mod.client.render.VanillaPhantomRedEyesLayer
import com.tamedphantoms.mod.client.texture.PhantomTextureProcessor
import com.tamedphantoms.mod.compat.VanillaTypes
import com.tamedphantoms.mod.entity.ModEntities
import com.tamedphantoms.mod.event.PhantomDismount
import com.tamedphantoms.mod.network.PhantomDismountAllowPayload
import com.tamedphantoms.mod.client.PhantomRosterClient
import com.tamedphantoms.mod.client.PhantomRosterScreen
import com.tamedphantoms.mod.network.PhantomMoonPulsePayload
import com.tamedphantoms.mod.network.PhantomRosterListPayload
import com.tamedphantoms.mod.platform.ModNetwork
import com.tamedphantoms.mod.util.MoonSickness
import me.shedaniel.clothconfig2.gui.ClothConfigScreen
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.client.renderer.entity.PhantomRenderer
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager

class TamedPhantomsFabricClient : ClientModInitializer {

    override fun onInitializeClient() {
        ClientPhantomInputSender.init()
        ClientFlightSpeedSender.init()
        ModNetwork.sendToServer = { ClientPlayNetworking.send(it) }

        KeyMappingHelper.registerKeyMapping(ModKeyMappings.FLY_UP)
        KeyMappingHelper.registerKeyMapping(ModKeyMappings.FLY_DOWN)
        KeyMappingHelper.registerKeyMapping(ModKeyMappings.SCREAM)
        KeyMappingHelper.registerKeyMapping(ModKeyMappings.LOOP)
        KeyMappingHelper.registerKeyMapping(ModKeyMappings.ROLL_LEFT)
        KeyMappingHelper.registerKeyMapping(ModKeyMappings.ROLL_RIGHT)
        ModKeyMappings.init()

        EntityRendererRegistry.register(ModEntities.TAMED_PHANTOM.get(), ::TamedPhantomRenderer)
        LivingEntityRenderLayerRegistrationCallback.EVENT.register { entityType, renderer, helper, _ ->
            if (entityType == VanillaTypes.phantom() && renderer is PhantomRenderer) {
                helper.register(VanillaPhantomRedEyesLayer(renderer))
            }
        }
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(object : SimpleSynchronousResourceReloadListener {
            override fun getFabricId(): Identifier =
                Identifier.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "phantom_textures")

            override fun onResourceManagerReload(resourceManager: ResourceManager) {
                val minecraft = Minecraft.getInstance()
                val work = {
                    PhantomTextureProcessor.resetGeneratedTextures()
                    PhantomTextureProcessor.prepareEyeTextures()
                    PhantomCloudWisp.forget()
                }
                if (minecraft.isSameThread) work() else minecraft.execute { work() }
            }
        })

        ClientTickEvents.END_CLIENT_TICK.register {
            ClientPhantomInputSender.onClientTick()
            ClientFlightSpeedSender.onClientTick()
            PhantomMoonLight.onTick()
            PhantomRosterOpener.onTick()
        }
        com.tamedphantoms.mod.entity.TamedPhantomEntity.clientAfterTick = { phantom ->
            com.tamedphantoms.mod.client.PhantomDiveWind.update(phantom)
            PhantomCloudWisp.afterClientTick(phantom)
        }

        ClientPlayNetworking.registerGlobalReceiver(PhantomMoonPulsePayload.TYPE) { _, context ->
            context.client().execute { MoonSickness.trigger() }
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
        val screen = minecraft.gui.screen()
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
        minecraft.gui.setScreen(PhantomRosterScreen(screen))
    }
}
