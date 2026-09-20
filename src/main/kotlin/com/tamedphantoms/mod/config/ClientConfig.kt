package com.tamedphantoms.mod.config

import net.neoforged.neoforge.common.ModConfigSpec

/**
 * Клиентский конфиг: только то, что слышит/видит этот игрок.
 * Файл: `config/tamedphantoms-client.toml`.
 */
class ClientConfig(builder: ModConfigSpec.Builder) {

    companion object {
        private const val KEY_PREFIX = "tamedphantoms.configuration"

        val SPEC: ModConfigSpec
        val CONFIG: ClientConfig

        init {
            val pair = ModConfigSpec.Builder().configure(::ClientConfig)
            CONFIG = pair.getLeft()
            SPEC = pair.getRight()
        }
    }

    val tamedSoundVolume: ModConfigSpec.DoubleValue

    init {
        builder.push("sound")
        tamedSoundVolume = builder
            .comment(
                "Громкость звуков ПРИРУЧЕННОГО фантома на этом клиенте.",
                "0.5 = в два раза тише. Дикие и освобождённые не затрагиваются.",
            )
            .translation("$KEY_PREFIX.sound.tamed_sound_volume")
            .defineInRange("tamed_sound_volume", 0.5, 0.0, 1.0)
        builder.pop()
    }
}
