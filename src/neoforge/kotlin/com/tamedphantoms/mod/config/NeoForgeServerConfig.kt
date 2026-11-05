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
                override fun screamRadius() = cfg.screamRadius.get()
                override fun screamCooldownSeconds() = cfg.screamCooldownSeconds.get()
                override fun stackedScreams() = if (SPEC.isLoaded) cfg.stackedScreams.get() else true
                override fun riderScream() = if (SPEC.isLoaded) cfg.riderScream.get() else false
                override fun insomniaDays() = cfg.insomniaDays.get()
                override fun phantomGroupMultiplier() = cfg.phantomGroupMultiplier.get()
                override fun phantomSize() = cfg.phantomSize.get()
                override fun immortalPhantoms() = if (SPEC.isLoaded) cfg.immortalPhantoms.get() else true
                override fun zoomBoost() = cfg.zoomBoost?.takeIf { SPEC.isLoaded }?.get() ?: ServerConfig.DEFAULT_ZOOM_BOOST
                override fun rollBoostLimit() = cfg.rollBoostLimit?.takeIf { SPEC.isLoaded }?.get() ?: ServerConfig.DEFAULT_ROLL_BOOST_LIMIT
                override fun rollBoostStep() = cfg.rollBoostStep?.takeIf { SPEC.isLoaded }?.get() ?: ServerConfig.DEFAULT_ROLL_BOOST_STEP
                override fun rollBoostClimbDrain() = cfg.rollClimbDrain?.takeIf { SPEC.isLoaded }?.get() ?: ServerConfig.DEFAULT_ROLL_CLIMB_DRAIN
                override fun rollBoostSpin() = cfg.rollSpin?.takeIf { SPEC.isLoaded }?.get() ?: ServerConfig.DEFAULT_ROLL_SPIN
                override fun rollBoostMinTicks() = cfg.rollMinTicks?.takeIf { SPEC.isLoaded }?.get() ?: ServerConfig.DEFAULT_ROLL_MIN_TICKS
            }
        }
    }

    val tameItemId: ModConfigSpec.ConfigValue<String>
    val releaseItemId: ModConfigSpec.ConfigValue<String>
    val tameChance: ModConfigSpec.DoubleValue
    val repelRadius: ModConfigSpec.DoubleValue
    val defendDurationTicks: ModConfigSpec.IntValue
    val flightSpeed: ModConfigSpec.DoubleValue
    val screamRadius: ModConfigSpec.DoubleValue
    val screamCooldownSeconds: ModConfigSpec.IntValue
    val stackedScreams: ModConfigSpec.BooleanValue
    val riderScream: ModConfigSpec.BooleanValue
    val insomniaDays: ModConfigSpec.IntValue
    val phantomGroupMultiplier: ModConfigSpec.DoubleValue
    val phantomSize: ModConfigSpec.IntValue
    val immortalPhantoms: ModConfigSpec.BooleanValue
    val zoomBoost: ModConfigSpec.DoubleValue?
    val rollBoostLimit: ModConfigSpec.DoubleValue?
    val rollBoostStep: ModConfigSpec.DoubleValue?
    val rollClimbDrain: ModConfigSpec.DoubleValue?
    val rollSpin: ModConfigSpec.DoubleValue?
    val rollMinTicks: ModConfigSpec.IntValue?

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
                "Модель и хитбокс считаются как 1.0 + 0.15 × размер. 4 — размер по умолчанию.",
                "0 — как дикий фантом.",
            )
            .translation("$KEY_PREFIX.taming.phantom_size")
            .defineInRange("phantom_size", 4, 0, 64)

        immortalPhantoms = builder
            .comment(
                "Существуют ли бессмертные фантомы. Включено — достижение «Властитель западных ветров»",
                "делает этого фантома бессмертным. Выключено — достижение бессмертия не даёт,",
                "а уже бессмертные фантомы в мире при следующем тике становятся обычными.",
                "По умолчанию включено. Применяется без перезапуска: сохраните файл или поменяйте значение в экране настроек.",
            )
            .translation("$KEY_PREFIX.taming.immortal_phantoms")
            .define("immortal_phantoms", true)

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
            .defineInRange("flight_speed", 1.25, 0.25, 16.0)

        if (RideExtras.enabled) {
            zoomBoost = builder
                .comment(
                    "Сила толчка вверх из мёртвой петли. Когда нос в петле смотрит вниз, седоку предлагают",
                    "нажать клавишу крика, и фантом получает один импульс вверх со сложенными крыльями.",
                    "8.0 — обычный толчок. Выше, вплоть до 64, скорость сразу после толчка больше,",
                    "а длится набор столько же. 0 — рывка нет и подсказки тоже.",
                )
                .translation("$KEY_PREFIX.flight.zoom_boost")
                .defineInRange("zoom_boost", ServerConfig.DEFAULT_ZOOM_BOOST, 0.0, ServerConfig.MAX_ZOOM_BOOST)
            rollBoostLimit = builder
                .comment(
                    "Потолок бонуса скорости за заряженную бочку, в блоках за тик.",
                    "Каждый полный оборот, пока седок держит приём, добавляет roll_boost_step, но не выше этого числа.",
                    "2.0 — обычный потолок. 0 — приёма на клавишу крика нет: бочку сделать можно, ускорения она не даёт.",
                )
                .translation("$KEY_PREFIX.flight.roll_boost_limit")
                .defineInRange("roll_boost_limit", ServerConfig.DEFAULT_ROLL_BOOST_LIMIT, 0.0, ServerConfig.MAX_ROLL_BOOST_LIMIT)
            rollBoostStep = builder
                .comment(
                    "Сколько блоков за тик добавляет один полный оборот заряженной бочки.",
                    "0.25 — обычная прибавка. 0 — обороты ничего не добавляют.",
                )
                .translation("$KEY_PREFIX.flight.roll_boost_step")
                .defineInRange("roll_boost_step", ServerConfig.DEFAULT_ROLL_BOOST_STEP, 0.0, ServerConfig.MAX_ROLL_BOOST_STEP)
            rollClimbDrain = builder
                .comment(
                    "Сколько бонуса скорости пропадает за две секунды, пока фантом летит вверх.",
                    "0.2 — за две секунды уходит одна прибавка оборота. 0 — набор высоты бонус не съедает.",
                    "Мёртвая петля и рывок из неё этот счётчик не трогают.",
                    "Под водой и в лаве бонус не тает сам, в том числе на всплытии.",
                    "Зависание, задний ход и полёт только вбок снимают бонус сразу.",
                )
                .translation("$KEY_PREFIX.flight.roll_boost_climb_drain")
                .defineInRange("roll_boost_climb_drain", ServerConfig.DEFAULT_ROLL_CLIMB_DRAIN, 0.0, ServerConfig.MAX_ROLL_CLIMB_DRAIN)
            rollSpin = builder
                .comment(
                    "Во сколько раз короче каждый следующий оборот заряженной бочки.",
                    "0.85 — каждый оборот примерно на 15% быстрее предыдущего. 1 — все обороты одной длины.",
                )
                .translation("$KEY_PREFIX.flight.roll_boost_spin")
                .defineInRange("roll_boost_spin", ServerConfig.DEFAULT_ROLL_SPIN, ServerConfig.MIN_ROLL_SPIN, 1.0)
            rollMinTicks = builder
                .comment(
                    "Самый короткий оборот заряженной бочки, в тиках. 20 тиков — секунда.",
                    "Дальше этого обороты не ускоряются. Обычная бочка без приёма всегда длится 16 тиков.",
                )
                .translation("$KEY_PREFIX.flight.roll_boost_min_ticks")
                .defineInRange("roll_boost_min_ticks", ServerConfig.DEFAULT_ROLL_MIN_TICKS, 2, ServerConfig.MAX_ROLL_MIN_TICKS)
        } else {
            zoomBoost = null
            rollBoostLimit = null
            rollBoostStep = null
            rollClimbDrain = null
            rollSpin = null
            rollMinTicks = null
        }

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

        stackedScreams = builder
            .comment(
                "Несколько криков сразу усиливают друг друга.",
                "Два крика — слепота вдвое дольше. С трёх добавляется урон, ближе к фантому — сильнее.",
                "Девять криков — потолок: 8 секунд слепоты и 10 урона. Страх мобов длится так же дольше.",
                "Выключено — каждый крик слепит на одну секунду, как раньше, и урона нет.",
            )
            .translation("$KEY_PREFIX.scream.stacked_screams")
            .define("stacked_screams", true)

        riderScream = builder
            .comment(
                "Крик хозяина с клавиши, пока он сидит на фантоме.",
                "Выключено — эта клавиша не кричит. Самозащита, игра и полнолуние кричат по-прежнему.",
                "Включено — после набора скорости бочкой та же клавиша молчит пять секунд.",
            )
            .translation("$KEY_PREFIX.scream.rider_scream")
            .define("rider_scream", false)

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
