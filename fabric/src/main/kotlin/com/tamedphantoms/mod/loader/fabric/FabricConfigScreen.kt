package com.tamedphantoms.mod.loader.fabric

import com.tamedphantoms.mod.util.PhantomFlightPace
import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigCategory
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

object FabricConfigScreen {

    fun create(parent: Screen): Screen {
        val remote = remoteServer()
        val server = FabricSettings.server
        val client = FabricSettings.client
        val builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("itemGroup.tamedphantoms"))
            .setSavingRunnable {
                FabricSettings.saveClient()
                if (!remote) {
                    FabricSettings.saveServer()
                    syncIfHosting()
                }
            }
        val entries = builder.entryBuilder()
        addServer(builder.getOrCreateCategory(Component.translatable("tamedphantoms.configuration.taming")), entries, server, remote)
        addFlight(builder.getOrCreateCategory(Component.translatable("tamedphantoms.configuration.flight")), entries, server, client, remote)
        addScream(builder.getOrCreateCategory(Component.translatable("tamedphantoms.configuration.scream")), entries, server, remote)
        addSpawning(builder.getOrCreateCategory(Component.translatable("tamedphantoms.configuration.spawning")), entries, server, remote)
        addSound(builder.getOrCreateCategory(Component.translatable("tamedphantoms.configuration.sound")), entries, client)
        return builder.build()
    }

    private fun addServer(category: ConfigCategory, entries: ConfigEntryBuilder, server: FabricSettings.ServerValues, remote: Boolean) {
        category.addEntry(text(entries, "tamedphantoms.configuration.taming.tame_item", server.tameItem, "minecraft:cookie", remote) {
            server.tameItem = it
        })
        category.addEntry(text(entries, "tamedphantoms.configuration.taming.release_item", server.releaseItem, "minecraft:poisonous_potato", remote) {
            server.releaseItem = it
        })
        category.addEntry(decimal(entries, "tamedphantoms.configuration.taming.tame_chance", server.tameChanceValue, 1.0, 0.0, 1.0, remote) {
            server.tameChanceValue = it
        })
        category.addEntry(decimal(entries, "tamedphantoms.configuration.taming.repel_radius", server.repelRadiusValue, 64.0, 0.0, 256.0, remote) {
            server.repelRadiusValue = it
        })
        category.addEntry(whole(entries, "tamedphantoms.configuration.taming.defend_time_ticks", server.defendTicks, 200, 20, 20 * 60 * 30, remote) {
            server.defendTicks = it
        })
        category.addEntry(whole(entries, "tamedphantoms.configuration.taming.phantom_size", server.size, 3, 0, 64, remote) {
            server.size = it
        })
    }

    private fun addFlight(
        category: ConfigCategory,
        entries: ConfigEntryBuilder,
        server: FabricSettings.ServerValues,
        client: FabricSettings.ClientValues,
        remote: Boolean,
    ) {
        category.addEntry(decimal(entries, "tamedphantoms.configuration.flight.flight_speed", server.flightSpeedValue, 1.25, PhantomFlightPace.MIN, PhantomFlightPace.MAX, remote) {
            server.flightSpeedValue = it
        })
        category.addEntry(decimal(entries, "tamedphantoms.configuration.flight.acrobatics_step", server.acrobaticsStepValue, 0.3, 0.05, 12.0, remote) {
            server.acrobaticsStepValue = it
        })
        val cap = server.flightSpeedValue.coerceIn(PhantomFlightPace.MIN, PhantomFlightPace.MAX)
        category.addEntry(decimal(entries, "tamedphantoms.configuration.flight.owned_flight_speed", client.ownedSpeed.coerceIn(PhantomFlightPace.MIN, cap), 1.25, PhantomFlightPace.MIN, cap, false) {
            client.ownedSpeed = it.coerceIn(PhantomFlightPace.MIN, cap)
        })
    }

    private fun addScream(category: ConfigCategory, entries: ConfigEntryBuilder, server: FabricSettings.ServerValues, remote: Boolean) {
        category.addEntry(decimal(entries, "tamedphantoms.configuration.scream.scream_radius", server.screamRadiusValue, 32.0, 4.0, 128.0, remote) {
            server.screamRadiusValue = it
        })
        category.addEntry(whole(entries, "tamedphantoms.configuration.scream.scream_cooldown_seconds", server.screamCooldown, 4, 1, 600, remote) {
            server.screamCooldown = it
        })
    }

    private fun addSpawning(category: ConfigCategory, entries: ConfigEntryBuilder, server: FabricSettings.ServerValues, remote: Boolean) {
        category.addEntry(whole(entries, "tamedphantoms.configuration.spawning.insomnia_days", server.insomnia, 1, 0, 30, remote) {
            server.insomnia = it
        })
        category.addEntry(decimal(entries, "tamedphantoms.configuration.spawning.phantom_group_multiplier", server.groupMultiplier, 2.0, 0.0, 16.0, remote) {
            server.groupMultiplier = it
        })
    }

    private fun addSound(category: ConfigCategory, entries: ConfigEntryBuilder, client: FabricSettings.ClientValues) {
        category.addEntry(decimal(entries, "tamedphantoms.configuration.sound.tamed_sound_volume", client.soundVolume, 0.5, 0.0, 1.0, false) {
            client.soundVolume = it
        })
    }

    private fun text(
        entries: ConfigEntryBuilder,
        key: String,
        value: String,
        default: String,
        locked: Boolean,
        save: (String) -> Unit,
    ) = entries.startStrField(Component.translatable(key), value)
        .setDefaultValue(default)
        .setSaveConsumer { if (!locked) save(it) }
        .build()

    private fun decimal(
        entries: ConfigEntryBuilder,
        key: String,
        value: Double,
        default: Double,
        min: Double,
        max: Double,
        locked: Boolean,
        save: (Double) -> Unit,
    ) = entries.startDoubleField(Component.translatable(key), value)
        .setDefaultValue(default)
        .setMin(min)
        .setMax(max)
        .setSaveConsumer { if (!locked) save(it) }
        .build()

    private fun whole(
        entries: ConfigEntryBuilder,
        key: String,
        value: Int,
        default: Int,
        min: Int,
        max: Int,
        locked: Boolean,
        save: (Int) -> Unit,
    ) = entries.startIntField(Component.translatable(key), value)
        .setDefaultValue(default)
        .setMin(min)
        .setMax(max)
        .setSaveConsumer { if (!locked) save(it) }
        .build()

    private fun remoteServer(): Boolean {
        val minecraft = Minecraft.getInstance()
        return minecraft.level != null && minecraft.singleplayerServer == null
    }

    private fun syncIfHosting() {
        val server = Minecraft.getInstance().singleplayerServer ?: return
        val payload = FabricSettings.toPayload()
        for (player in server.playerList.players) {
            ServerPlayNetworking.send(player, payload)
        }
    }
}
