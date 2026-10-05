package com.tamedphantoms.mod.loader.fabric

import com.tamedphantoms.mod.TamedPhantomsMod
import com.tamedphantoms.mod.entity.ModEntities
import com.tamedphantoms.mod.entity.TamedPhantomEntity
import com.tamedphantoms.mod.event.PhantomDamageHandler
import com.tamedphantoms.mod.event.PhantomDismount
import com.tamedphantoms.mod.event.PhantomGuideHandler
import com.tamedphantoms.mod.event.PhantomInteractionHandler
import com.tamedphantoms.mod.event.PhantomOwnerRecallHandler
import com.tamedphantoms.mod.event.PhantomTemptHandler
import com.tamedphantoms.mod.item.ModItems
import com.tamedphantoms.mod.loader.fabric.mixin.MobGoalAccessor
import com.tamedphantoms.mod.loader.fabric.mixin.PersistentDataCarrier
import com.tamedphantoms.mod.platform.ModAccess
import com.tamedphantoms.mod.platform.ModNetwork
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.event.player.UseItemCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.SpawnEggItem
import java.util.function.Supplier

class TamedPhantomsFabric : ModInitializer {

    override fun onInitialize() {
        TamedPhantomsMod.LOGGER.info("Инициализация Tamed Phantoms (Fabric)")
        FabricSettings.bind()
        ModAccess.persistentData = { (it as PersistentDataCarrier).tamedphantomsPersistentData() }
        ModAccess.addGoal = { mob, priority, goal ->
            (mob as MobGoalAccessor).tamedphantomsGoals().addGoal(priority, goal)
        }
        registerContent()
        FabricNetworking.register()
        ModNetwork.isModLoaded = { FabricLoader.getInstance().isModLoaded(it) }
        ModNetwork.sendToPlayer = { player, payload -> ServerPlayNetworking.send(player, payload) }
        ModNetwork.sendToDimension = { level, payload ->
            for (player in level.players()) {
                ServerPlayNetworking.send(player, payload)
            }
        }
        registerEvents()
    }

    private fun registerContent() {
        val id = ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "tamed_phantom")
        val type = ModEntities.builder().build("tamed_phantom")
        Registry.register(BuiltInRegistries.ENTITY_TYPE, id, type)
        ModEntities.TAMED_PHANTOM = Supplier { type }
        FabricDefaultAttributeRegistry.register(type, TamedPhantomEntity.createAttributes())

        val icon = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "tab_icon"),
            Item(Item.Properties()),
        )
        val egg = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "released_phantom_spawn_egg"),
            SpawnEggItem(type, 0x252D4C, 0x4B8C00, Item.Properties()),
        )
        ModItems.TAB_ICON = Supplier { icon }
        ModItems.RELEASED_PHANTOM_SPAWN_EGG = Supplier { egg }
        val tab = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath(TamedPhantomsMod.MOD_ID, "tamedphantoms"),
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup.tamedphantoms"))
                .icon { ItemStack(icon) }
                .displayItems { _, output -> ModItems.fillTab(output) }
                .build(),
        )
        ModItems.CREATIVE_TAB = Supplier { tab }
    }

    private fun registerEvents() {
        UseEntityCallback.EVENT.register { player, _, hand, entity, _ ->
            PhantomInteractionHandler.onEntityInteract(player, entity, hand) ?: InteractionResult.PASS
        }
        UseItemCallback.EVENT.register { player, _, hand ->
            val stack = player.getItemInHand(hand)
            val result = PhantomInteractionHandler.onUseItem(player, hand)
            if (result == null) InteractionResultHolder.pass(stack) else InteractionResultHolder.consume(stack)
        }
        ServerLivingEntityEvents.AFTER_DAMAGE.register { entity, source, _, taken, _ ->
            PhantomDamageHandler.onLivingDamage(entity, source, taken)
        }
        ServerLivingEntityEvents.ALLOW_DEATH.register { entity, source, amount ->
            PhantomDamageHandler.onLivingDamage(entity, source, amount)
            true
        }
        ServerEntityEvents.ENTITY_LOAD.register { entity, _ ->
            PhantomTemptHandler.onJoin(entity)
        }
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            PhantomGuideHandler.onPlayerLogin(handler.player)
            PhantomOwnerRecallHandler.onLogin(handler.player)
            FabricNetworking.sendSettings(handler.player)
        }
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            PhantomDismount.onLogout(handler.player)
            com.tamedphantoms.mod.util.OwnerFlightSpeed.clear(handler.player.uuid)
        }
        ServerPlayerEvents.AFTER_RESPAWN.register { _, newPlayer, _ ->
            PhantomOwnerRecallHandler.onRespawn(newPlayer)
        }
        ServerPlayerEvents.COPY_FROM.register { oldPlayer, newPlayer, _ ->
            PhantomOwnerRecallHandler.onClone(newPlayer, oldPlayer)
        }
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register { player, _, _ ->
            PhantomOwnerRecallHandler.onChangedDimension(player)
        }
        ServerTickEvents.END_SERVER_TICK.register { server ->
            for (player in server.playerList.players) {
                PhantomDismount.onPlayerTick(player)
            }
        }
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            FabricSettings.loadWorld(server)
        }
    }
}
