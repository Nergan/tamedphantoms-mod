package com.tamedphantoms.mod.loader.fabric

import com.tamedphantoms.mod.TamedPhantomsMod
import com.tamedphantoms.mod.config.ClientConfig
import com.tamedphantoms.mod.config.RideExtras
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
    private var worldStamp: Long = 0L

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
        worldStamp = stamp(path)
    }

    /**
     * Файл мира правят и на работающем сервере. NeoForge перечитывает свой toml сам,
     * здесь сервер раз в несколько секунд смотрит время изменения и шлёт правила игрокам.
     */
    fun reloadIfChanged(): Boolean {
        val path = worldFile ?: return false
        val now = stamp(path)
        if (now == 0L || now == worldStamp) return false
        worldStamp = now
        read(path, server::read)
        TamedPhantomsMod.LOGGER.info("Серверные правила Tamed Phantoms перечитаны из {}", path)
        return true
    }

    fun saveClient() {
        clientFile().writeText(client.write())
    }

    fun saveServer() {
        val world = worldFile
        if (world != null) {
            world.writeText(server.write())
            worldStamp = stamp(world)
            return
        }
        templateFile().writeText(server.write())
    }

    private fun stamp(path: Path): Long = try {
        if (path.exists()) Files.getLastModifiedTime(path).toMillis() else 0L
    } catch (_: Exception) {
        0L
    }

    private fun read(path: Path, read: (Map<String, String>) -> Unit) {
        try {
            read(parse(path.readText()))
        } catch (error: Exception) {
            TamedPhantomsMod.LOGGER.warn("Не удалось перечитать {}", path, error)
        }
    }

    fun toPayload() = PhantomServerSettingsPayload(
        server.tameItem,
        server.releaseItem,
        server.tameChanceValue,
        server.repelRadiusValue,
        server.defendTicks,
        server.flightSpeedValue,
        server.screamRadiusValue,
        server.screamCooldown,
        server.insomnia,
        server.groupMultiplier,
        server.size,
        server.immortal,
        server.stackedScreams,
        server.zoomBoostValue,
        server.rollBoostLimitValue,
        server.rollBoostStepValue,
        server.rollClimbDrainValue,
        server.rollSpinValue,
        server.rollMinTicksValue,
    )

    fun apply(payload: PhantomServerSettingsPayload) {
        server.tameItem = payload.tameItemId
        server.releaseItem = payload.releaseItemId
        server.tameChanceValue = payload.tameChance
        server.repelRadiusValue = payload.repelRadius
        server.defendTicks = payload.defendDurationTicks
        server.flightSpeedValue = payload.flightSpeed
        server.screamRadiusValue = payload.screamRadius
        server.screamCooldown = payload.screamCooldownSeconds
        server.insomnia = payload.insomniaDays
        server.groupMultiplier = payload.phantomGroupMultiplier
        server.size = payload.phantomSize
        server.immortal = payload.immortalPhantoms
        server.stackedScreams = payload.stackedScreams
        server.zoomBoostValue = payload.zoomBoost
        server.rollBoostLimitValue = payload.rollBoostLimit
        server.rollBoostStepValue = payload.rollBoostStep
        server.rollClimbDrainValue = payload.rollBoostClimbDrain
        server.rollSpinValue = payload.rollBoostSpin
        server.rollMinTicksValue = payload.rollBoostMinTicks
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
        var screamRadiusValue: Double = 32.0
        var screamCooldown: Int = 4
        var insomnia: Int = 1
        var groupMultiplier: Double = 2.0
        var size: Int = 4
        var immortal: Boolean = true
        var stackedScreams: Boolean = true
        var riderScream: Boolean = false
        var zoomBoostValue: Double = ServerConfig.DEFAULT_ZOOM_BOOST
        var rollBoostLimitValue: Double = ServerConfig.DEFAULT_ROLL_BOOST_LIMIT
        var rollBoostStepValue: Double = ServerConfig.DEFAULT_ROLL_BOOST_STEP
        var rollClimbDrainValue: Double = ServerConfig.DEFAULT_ROLL_CLIMB_DRAIN
        var rollSpinValue: Double = ServerConfig.DEFAULT_ROLL_SPIN
        var rollMinTicksValue: Int = ServerConfig.DEFAULT_ROLL_MIN_TICKS

        override fun tameItemId() = tameItem
        override fun releaseItemId() = releaseItem
        override fun tameChance() = tameChanceValue
        override fun repelRadius() = repelRadiusValue
        override fun defendDurationTicks() = defendTicks
        override fun flightSpeed() = flightSpeedValue
        override fun screamRadius() = screamRadiusValue
        override fun screamCooldownSeconds() = screamCooldown
        override fun insomniaDays() = insomnia
        override fun phantomGroupMultiplier() = groupMultiplier
        override fun phantomSize() = size
        override fun immortalPhantoms() = immortal
        override fun stackedScreams() = stackedScreams
        override fun riderScream() = riderScream
        override fun zoomBoost() = zoomBoostValue
        override fun rollBoostLimit() = rollBoostLimitValue
        override fun rollBoostStep() = rollBoostStepValue
        override fun rollBoostClimbDrain() = rollClimbDrainValue
        override fun rollBoostSpin() = rollSpinValue
        override fun rollBoostMinTicks() = rollMinTicksValue

        fun read(values: Map<String, String>) {
            tameItem = values["taming.tame_item"] ?: tameItem
            releaseItem = values["taming.release_item"] ?: releaseItem
            tameChanceValue = values.double("taming.tame_chance", tameChanceValue)
            repelRadiusValue = values.double("taming.repel_radius", repelRadiusValue)
            defendTicks = values.int("taming.defend_time_ticks", defendTicks)
            size = values.int("taming.phantom_size", size)
            immortal = values.bool("taming.immortal_phantoms", immortal)
            flightSpeedValue = values.double("flight.flight_speed", flightSpeedValue)
            if (RideExtras.enabled) {
                zoomBoostValue = values.double("flight.zoom_boost", zoomBoostValue)
                rollBoostLimitValue = values.double("flight.roll_boost_limit", rollBoostLimitValue)
                rollBoostStepValue = values.double("flight.roll_boost_step", rollBoostStepValue)
                rollClimbDrainValue = values.double("flight.roll_boost_climb_drain", rollClimbDrainValue)
                rollSpinValue = values.double("flight.roll_boost_spin", rollSpinValue)
                rollMinTicksValue = values.int("flight.roll_boost_min_ticks", rollMinTicksValue)
            }
            screamRadiusValue = values.double("scream.scream_radius", screamRadiusValue)
            screamCooldown = values.int("scream.scream_cooldown_seconds", screamCooldown)
            stackedScreams = values.bool("scream.stacked_screams", stackedScreams)
            riderScream = values.bool("scream.rider_scream", riderScream)
            insomnia = values.int("spawning.insomnia_days", insomnia)
            groupMultiplier = values.double("spawning.phantom_group_multiplier", groupMultiplier)
        }

        fun writeMissing(): String = write()

        fun write(): String {
            val flight = "flight_speed = $flightSpeedValue\n" +
                if (RideExtras.enabled) {
                    "zoom_boost = $zoomBoostValue\n" +
                        "roll_boost_limit = $rollBoostLimitValue\n" +
                        "roll_boost_step = $rollBoostStepValue\n" +
                        "roll_boost_climb_drain = $rollClimbDrainValue\n" +
                        "roll_boost_spin = $rollSpinValue\n" +
                        "roll_boost_min_ticks = $rollMinTicksValue\n"
                } else {
                    ""
                }
            return """
                [taming]
                tame_item = "$tameItem"
                release_item = "$releaseItem"
                tame_chance = $tameChanceValue
                repel_radius = $repelRadiusValue
                defend_time_ticks = $defendTicks
                phantom_size = $size
                immortal_phantoms = $immortal

                [flight]
            """.trimIndent() + "\n" + flight + "\n" + """
                [scream]
                scream_radius = $screamRadiusValue
                scream_cooldown_seconds = $screamCooldown
                stacked_screams = $stackedScreams
                rider_scream = $riderScream

                [spawning]
                insomnia_days = $insomnia
                phantom_group_multiplier = $groupMultiplier
            """.trimIndent() + "\n"
        }
    }

    class ClientValues : ClientConfig.Settings {
        var soundVolume: Double = 0.5
        var ownedSpeed: Double = 1.25
        var horizonHold: Int = ClientConfig.DEFAULT_HORIZON_HOLD
        var smoothing: Int = ClientConfig.DEFAULT_CAMERA_SMOOTHING

        override fun tamedSoundVolume() = soundVolume
        override fun ownedFlightSpeed() = ownedSpeed
        override fun cameraHorizonHold() = horizonHold
        override fun cameraSmoothing() = smoothing

        fun read(values: Map<String, String>) {
            soundVolume = values.double("sound.tamed_sound_volume", soundVolume)
            ownedSpeed = values.double("flight.owned_flight_speed", ownedSpeed)
            if (RideExtras.enabled) {
                horizonHold = values.int("flight.camera_horizon_hold", horizonHold)
                smoothing = values.int("flight.camera_smoothing", smoothing)
            }
        }

        fun writeMissing(): String = write()

        fun write(): String {
            val camera = if (RideExtras.enabled) {
                "camera_horizon_hold = $horizonHold\ncamera_smoothing = $smoothing\n"
            } else {
                ""
            }
            return """
                [sound]
                tamed_sound_volume = $soundVolume

                [flight]
                owned_flight_speed = $ownedSpeed
            """.trimIndent() + "\n" + camera
        }
    }
}

private fun Map<String, String>.double(key: String, fallback: Double): Double =
    this[key]?.toDoubleOrNull() ?: fallback.also {
        if (this[key] != null) {
            TamedPhantomsMod.LOGGER.warn("Некорректное число в настройке {}: '{}'", key, this[key])
        }
    }

private fun Map<String, String>.bool(key: String, fallback: Boolean): Boolean = when (this[key]?.trim()?.lowercase()) {
    "true" -> true
    "false" -> false
    null -> fallback
    else -> fallback.also { TamedPhantomsMod.LOGGER.warn("Некорректное значение в настройке {}: '{}'", key, this[key]) }
}

private fun Map<String, String>.int(key: String, fallback: Int): Int =
    this[key]?.toIntOrNull() ?: this[key]?.toDoubleOrNull()?.toInt() ?: fallback
