package com.tamedphantoms.mod.config

import net.neoforged.neoforge.common.ModConfigSpec

/**
 * Конфиг типа SERVER (см. [net.neoforged.fml.config.ModConfig.Type.SERVER]).
 *
 * Единственный тип конфига NeoForge, который сервер рассылает клиентам,
 * поэтому правила приручения на одном сервере одинаковые у всех.
 * Экран: Mods → Tamed Phantoms → Config.
 *
 * Ключи локализации NeoForge ищет как `<modid>.configuration.<путь>`.
 * Для `tame_item` это `tamedphantoms.configuration.taming.tame_item`.
 */
class NeoForgeServerConfig(builder: ModConfigSpec.Builder) {

    companion object {
        private const val KEY_PREFIX = "tamedphantoms.configuration"

        val SPEC: ModConfigSpec
        val CONFIG: NeoForgeServerConfig

        init {
            val pair = ModConfigSpec.Builder().configure(::NeoForgeServerConfig)
            CONFIG = pair.getLeft()
            SPEC = pair.getRight()
        }

        fun bind() {
            val cfg = CONFIG
            ServerConfig.settings = object : ServerConfig.Settings {
                override fun tameItemId() = cfg.tameItemId.get()
                override fun releaseItemId() = cfg.releaseItemId.get()
                override fun tameChance() = cfg.tameChance.get()
                override fun repelRadius() = cfg.repelRadius.get()
                override fun defendDurationTicks() = cfg.defendDurationTicks.get()
                override fun flightSpeed() = cfg.flightSpeed.get()
                override fun acrobaticsStep() = cfg.acrobaticsStep.get()
                override fun screamRadius() = cfg.screamRadius.get()
                override fun screamCooldownSeconds() = cfg.screamCooldownSeconds.get()
                override fun insomniaDays() = cfg.insomniaDays.get()
                override fun phantomGroupMultiplier() = cfg.phantomGroupMultiplier.get()
                override fun phantomSize() = cfg.phantomSize.get()
            }
        }
    }

    val tameItemId: ModConfigSpec.ConfigValue<String>
    val releaseItemId: ModConfigSpec.ConfigValue<String>
    val tameChance: ModConfigSpec.DoubleValue
    val repelRadius: ModConfigSpec.DoubleValue
    val defendDurationTicks: ModConfigSpec.IntValue
    val flightSpeed: ModConfigSpec.DoubleValue
    val acrobaticsStep: ModConfigSpec.DoubleValue
    val screamRadius: ModConfigSpec.DoubleValue
    val screamCooldownSeconds: ModConfigSpec.IntValue
    val insomniaDays: ModConfigSpec.IntValue
    val phantomGroupMultiplier: ModConfigSpec.DoubleValue
    val phantomSize: ModConfigSpec.IntValue

    init {
        builder.push("taming")

        tameItemId = builder
            .comment(
                "Каким предметом можно приручить дикого фантома (правый клик по фантому с этим предметом в руке).",
                "Указывается ID предмета в формате 'namespace:path', например 'minecraft:cookie'.",
                "Подходит ЛЮБОЙ предмет из игры или другого установленного мода — необязательно еда.",
                "Тот же самый предмет, поднесённый УЖЕ приручённому фантому, будет использован как",
                "обычная еда для лечения, если у него есть пищевая ценность (FoodProperties).",
                "Некорректный/несуществующий ID -> мод тихо откатится на minecraft:cookie и запишет",
                "предупреждение в лог.",
            )
            .translation("$KEY_PREFIX.taming.tame_item")
            .define("tame_item", "minecraft:cookie")

        releaseItemId = builder
            .comment(
                "Каким предметом можно ОСВОБОДИТЬ уже прирученного фантома (снять с него владельца).",
                "Формат такой же, как у tame_item. Некорректный/несуществующий ID -> откат на",
                "minecraft:poisonous_potato.",
            )
            .translation("$KEY_PREFIX.taming.release_item")
            .define("release_item", "minecraft:poisonous_potato")

        tameChance = builder
            .comment("Шанс успешного приручения за одно скармливание предмета. 1.0 = приручается всегда.")
            .translation("$KEY_PREFIX.taming.tame_chance")
            .defineInRange("tame_chance", 1.0, 0.0, 1.0)

        repelRadius = builder
            .comment("Радиус в блоках, на котором прирученный фантом отпугивает диких фантомов и мешает им заспавниться рядом.")
            .translation("$KEY_PREFIX.taming.repel_radius")
            .defineInRange("repel_radius", 64.0, 0.0, 256.0)

        defendDurationTicks = builder
            .comment("Сколько тиков длится режим самозащиты после того, как фантома ударили (20 тиков = 1 секунда).")
            .translation("$KEY_PREFIX.taming.defend_time_ticks")
            .defineInRange("defend_time_ticks", 200, 20, 20 * 60 * 30)

        phantomSize = builder
            .comment(
                "Ванильный размер прирученных и освобождённых фантомов.",
                "Модель и хитбокс считаются как 1.0 + 0.15 × размер. 3 — текущий вид, примерно в 1.45 раза крупнее дикого.",
                "0 — как дикий фантом.",
            )
            .translation("$KEY_PREFIX.taming.phantom_size")
            .defineInRange("phantom_size", 3, 0, 64)

        builder.pop()

        builder.push("flight")

        flightSpeed = builder
            .comment(
                "Скорость полёта прирученных и освобождённых фантомов относительно дикого.",
                "1.0 — как дикий, 2.0 — прежняя скорость питомцев в этом моде, 1.25 — значение по умолчанию.",
                "Под водой фантом сам замедляется на 50%, эта настройка на то не влияет.",
                "Действует и на полёт верхом, и на самостоятельный полёт.",
                "Хозяин может убавить скорость только своих прирученных фантомов в клиентском конфиге;",
                "выше этого значения поднять её нельзя. Освобождённые всегда летают на серверной скорости.",
            )
            .translation("$KEY_PREFIX.flight.flight_speed")
            .defineInRange("flight_speed", 1.25, 0.25, 4.0)

        acrobaticsStep = builder
            .comment(
                "Насколько быстро в прямом полёте набираются бочка и мёртвая петля, в градусах за тик.",
                "Меньше — фигуры медленнее. 0.3 — около минуты на полный оборот при зажатой клавише.",
            )
            .translation("$KEY_PREFIX.flight.acrobatics_step")
            .defineInRange("acrobatics_step", 0.3, 0.05, 12.0)

        builder.pop()

        builder.push("scream")

        screamRadius = builder
            .comment("Радиус крика в блоках: мобы в этом радиусе разбегаются.")
            .translation("$KEY_PREFIX.scream.scream_radius")
            .defineInRange("scream_radius", 32.0, 4.0, 128.0)

        screamCooldownSeconds = builder
            .comment("Перезарядка крика в секундах. Общая для хозяина, самозащиты и полнолуния.")
            .translation("$KEY_PREFIX.scream.scream_cooldown_seconds")
            .defineInRange("scream_cooldown_seconds", 4, 1, 600)

        builder.pop()

        builder.push("spawning")

        insomniaDays = builder
            .comment(
                "Сколько суток игроку нужно не спать, чтобы ванильные фантомы могли появиться.",
                "В ванили это 3. Проверка та же: чем дольше не спать после порога, тем выше шанс.",
                "0 — порога нет, фантомы могут появляться в первую же ночь.",
            )
            .translation("$KEY_PREFIX.spawning.insomnia_days")
            .defineInRange("insomnia_days", 1, 0, 30)

        phantomGroupMultiplier = builder
            .comment(
                "Во сколько раз больше ванильных фантомов появляется в одной группе.",
                "2.0 — вдвое больше, чем решила бы ваниль. 1.0 — как в ванили.",
            )
            .translation("$KEY_PREFIX.spawning.phantom_group_multiplier")
            .defineInRange("phantom_group_multiplier", 2.0, 0.0, 16.0)

        builder.pop()
    }
}
