package com.tamedphantoms.mod.config

/** То, что слышит и выбирает один игрок. Файл: `config/tamedphantoms-client.toml`. */
object ClientConfig {

    interface Settings {
        fun tamedSoundVolume(): Double
        fun ownedFlightSpeed(): Double
    }

    var settings: Settings = Defaults

    fun tamedSoundVolume(): Double = settings.tamedSoundVolume().coerceIn(0.0, 1.0)
    fun ownedFlightSpeed(): Double = settings.ownedFlightSpeed()

    private object Defaults : Settings {
        override fun tamedSoundVolume() = 0.5
        override fun ownedFlightSpeed() = 1.25
    }
}
