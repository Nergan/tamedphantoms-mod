package com.tamedphantoms.mod.config

import net.neoforged.neoforge.common.ModConfigSpec

/** Клиентский конфиг NeoForge. Файл: `config/tamedphantoms-client.toml`. */
class NeoForgeClientConfig(builder: ModConfigSpec.Builder) {

    companion object {
        private const val KEY_PREFIX = "tamedphantoms.configuration"

        val SPEC: ModConfigSpec
        val CONFIG: NeoForgeClientConfig

        init {
            val pair = ModConfigSpec.Builder().configure(::NeoForgeClientConfig)
            CONFIG = pair.getLeft()
            SPEC = pair.getRight()
        }

        fun bind() {
            val cfg = CONFIG
            ClientConfig.settings = object : ClientConfig.Settings {
                override fun tamedSoundVolume() = cfg.tamedSoundVolume.get()
                override fun ownedFlightSpeed() = cfg.ownedFlightSpeed.get()
                override fun cameraHorizonHold() = cfg.cameraHorizonHold?.get() ?: ClientConfig.DEFAULT_HORIZON_HOLD
                override fun cameraSmoothing() = cfg.cameraSmoothing?.get() ?: ClientConfig.DEFAULT_CAMERA_SMOOTHING
            }
        }
    }

    val tamedSoundVolume: ModConfigSpec.DoubleValue
    val ownedFlightSpeed: ModConfigSpec.DoubleValue
    val cameraHorizonHold: ModConfigSpec.IntValue?
    val cameraSmoothing: ModConfigSpec.IntValue?

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

        builder.push("flight")
        ownedFlightSpeed = builder
            .comment(
                "Скорость полёта СВОИХ прирученных фантомов относительно дикого.",
                "В игре это ползунок: правый край — максимум этого сервера, выше поставить нельзя.",
                "Если в файле записано больше, применится серверный максимум.",
                "Чужие и освобождённые фантомы этой настройкой не затрагиваются.",
                "1.0 — как дикий, 2.0 — прежняя скорость питомцев, 1.25 — значение по умолчанию.",
            )
            .translation("$KEY_PREFIX.flight.owned_flight_speed")
            .defineInRange("owned_flight_speed", 1.25, 0.25, 16.0)
        if (RideExtras.enabled) {
            cameraHorizonHold = builder
                .comment(
                    "Первое лицо верхом. Глаза всегда сидят на спине фантома.",
                    "Пока фантом наклонён меньше этого угла, взгляд держит горизонт.",
                    "Дальше фантом поворачивает взгляд за собой. 0 — взгляд всегда вместе с фантомом. Градусы.",
                )
                .translation("$KEY_PREFIX.flight.camera_horizon_hold")
                .defineInRange("camera_horizon_hold", ClientConfig.DEFAULT_HORIZON_HOLD, 0, ClientConfig.MAX_HORIZON_HOLD)
            cameraSmoothing = builder
                .comment(
                    "Первое лицо верхом. Насколько мягко взгляд идёт за фантомом и выравнивается обратно.",
                    "0 — сразу, 100 — медленнее всего. Проценты.",
                )
                .translation("$KEY_PREFIX.flight.camera_smoothing")
                .defineInRange("camera_smoothing", ClientConfig.DEFAULT_CAMERA_SMOOTHING, 0, ClientConfig.MAX_CAMERA_SMOOTHING)
        } else {
            cameraHorizonHold = null
            cameraSmoothing = null
        }
        builder.pop()
    }
}
