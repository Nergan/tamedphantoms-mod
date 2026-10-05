package com.tamedphantoms.mod.loader.fabric

import com.tamedphantoms.mod.TamedPhantomsMod
import com.tamedphantoms.mod.config.ClientConfig
import com.tamedphantoms.mod.config.ServerConfig
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/** Общие для обоих загрузчиков ключи. NeoForge пишет тот же toml своим экраном. */
object FabricSettings {

    val server = ServerValues()
    val client = ClientValues()
    var worldFile: Path? = null

    fun bind() {
        ServerConfig.settings = server
        ClientConfig.settings = client
        load(clientFile(), client::read, client::writeMissing)
        load(templateFile(), server::read, server::writeMissing)
    }

    fun loadWorld(serverGame: MinecraftServer) {
        val path = serverGame.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("tamedphantoms-server.toml")
        val parent = path.parent
        if (parent != null) Files.createDirectories(parent)
        if (!path.exists() && templateFile().exists()) {
            Files.copy(templateFile(), path)
        }
        worldFile = path
        load(path, server::read, server::writeMissing)
    }

    fun saveClient() {
        clientFile().writeText(client.write())
    }

    fun saveServer() {
        val world = worldFile
        if (world != null) {
            world.writeText(server.write())
            return
        }
        templateFile().writeText(server.write())
    }

    fun toPayload() = PhantomServerSettingsPayload(
        server.tameItem,
        server.releaseItem,
        server.tameChanceValue,
        server.repelRadiusValue,
        server.defendTicks,
        server.flightSpeedValue,
        server.acrobaticsStepValue,
        server.screamRadiusValue,
        server.screamCooldown,
        server.insomnia,
        server.groupMultiplier,
        server.size,
    )

    fun apply(payload: PhantomServerSettingsPayload) {
        server.tameItem = payload.tameItemId
        server.releaseItem = payload.releaseItemId
        server.tameChanceValue = payload.tameChance
        server.repelRadiusValue = payload.repelRadius
        server.defendTicks = payload.defendDurationTicks
        server.flightSpeedValue = payload.flightSpeed
        server.acrobaticsStepValue = payload.acrobaticsStep
        server.screamRadiusValue = payload.screamRadius
        server.screamCooldown = payload.screamCooldownSeconds
        server.insomnia = payload.insomniaDays
        server.groupMultiplier = payload.phantomGroupMultiplier
        server.size = payload.phantomSize
    }

    private fun clientFile(): Path = FabricLoader.getInstance().configDir.resolve("tamedphantoms-client.toml")

    private fun templateFile(): Path = FabricLoader.getInstance().configDir.resolve("tamedphantoms-server.toml")

    private fun load(path: Path, read: (Map<String, String>) -> Unit, writeMissing: () -> String) {
        if (!path.exists()) {
            path.parent?.let { Files.createDirectories(it) }
            path.writeText(writeMissing())
            return
        }
        read(parse(path.readText()))
    }

    private fun parse(text: String): Map<String, String> {
        val values = HashMap<String, String>()
        var section = ""
        for (raw in text.lineSequence()) {
            val line = stripComment(raw).trim()
            if (line.isEmpty()) continue
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length - 1).trim()
                continue
            }
            val eq = line.indexOf('=')
            if (eq <= 0) continue
            val key = line.substring(0, eq).trim()
            val value = unquote(line.substring(eq + 1).trim())
            val full = if (section.isEmpty()) key else "$section.$key"
            values[full] = value
        }
        return values
    }

    private fun stripComment(line: String): String {
        var quote = false
        val out = StringBuilder()
        for (ch in line) {
            if (ch == '"') quote = !quote
            if (ch == '#' && !quote) break
            out.append(ch)
        }
        return out.toString()
    }

    private fun unquote(value: String): String {
        if (value.length >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length - 1)
        }
        return value
    }

    class ServerValues : ServerConfig.Settings {
        var tameItem: String = "minecraft:cookie"
        var releaseItem: String = "minecraft:poisonous_potato"
        var tameChanceValue: Double = 1.0
        var repelRadiusValue: Double = 64.0
        var defendTicks: Int = 200
        var flightSpeedValue: Double = 1.25
        var acrobaticsStepValue: Double = 0.3
        var screamRadiusValue: Double = 32.0
        var screamCooldown: Int = 4
        var insomnia: Int = 1
        var groupMultiplier: Double = 2.0
        var size: Int = 3

        override fun tameItemId() = tameItem
        override fun releaseItemId() = releaseItem
        override fun tameChance() = tameChanceValue
        override fun repelRadius() = repelRadiusValue
        override fun defendDurationTicks() = defendTicks
        override fun flightSpeed() = flightSpeedValue
        override fun acrobaticsStep() = acrobaticsStepValue
        override fun screamRadius() = screamRadiusValue
        override fun screamCooldownSeconds() = screamCooldown
        override fun insomniaDays() = insomnia
        override fun phantomGroupMultiplier() = groupMultiplier
        override fun phantomSize() = size

        fun read(values: Map<String, String>) {
            tameItem = values["taming.tame_item"] ?: tameItem
            releaseItem = values["taming.release_item"] ?: releaseItem
            tameChanceValue = values.double("taming.tame_chance", tameChanceValue)
            repelRadiusValue = values.double("taming.repel_radius", repelRadiusValue)
            defendTicks = values.int("taming.defend_time_ticks", defendTicks)
            size = values.int("taming.phantom_size", size)
            flightSpeedValue = values.double("flight.flight_speed", flightSpeedValue)
            acrobaticsStepValue = values.double("flight.acrobatics_step", acrobaticsStepValue)
            screamRadiusValue = values.double("scream.scream_radius", screamRadiusValue)
            screamCooldown = values.int("scream.scream_cooldown_seconds", screamCooldown)
            insomnia = values.int("spawning.insomnia_days", insomnia)
            groupMultiplier = values.double("spawning.phantom_group_multiplier", groupMultiplier)
        }

        fun writeMissing(): String = write()

        fun write(): String = """
            [taming]
            tame_item = "$tameItem"
            release_item = "$releaseItem"
            tame_chance = $tameChanceValue
            repel_radius = $repelRadiusValue
            defend_time_ticks = $defendTicks
            phantom_size = $size

            [flight]
            flight_speed = $flightSpeedValue
            acrobatics_step = $acrobaticsStepValue

            [scream]
            scream_radius = $screamRadiusValue
            scream_cooldown_seconds = $screamCooldown

            [spawning]
            insomnia_days = $insomnia
            phantom_group_multiplier = $groupMultiplier
        """.trimIndent() + "\n"
    }

    class ClientValues : ClientConfig.Settings {
        var soundVolume: Double = 0.5
        var ownedSpeed: Double = 1.25

        override fun tamedSoundVolume() = soundVolume
        override fun ownedFlightSpeed() = ownedSpeed

        fun read(values: Map<String, String>) {
            soundVolume = values.double("sound.tamed_sound_volume", soundVolume)
            ownedSpeed = values.double("flight.owned_flight_speed", ownedSpeed)
        }

        fun writeMissing(): String = write()

        fun write(): String = """
            [sound]
            tamed_sound_volume = $soundVolume

            [flight]
            owned_flight_speed = $ownedSpeed
        """.trimIndent() + "\n"
    }
}

private fun Map<String, String>.double(key: String, fallback: Double): Double =
    this[key]?.toDoubleOrNull() ?: fallback.also {
        if (this[key] != null) {
            TamedPhantomsMod.LOGGER.warn("Некорректное число в настройке {}: '{}'", key, this[key])
        }
    }

private fun Map<String, String>.int(key: String, fallback: Int): Int =
    this[key]?.toIntOrNull() ?: this[key]?.toDoubleOrNull()?.toInt() ?: fallback
