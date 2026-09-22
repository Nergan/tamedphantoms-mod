package com.tamedphantoms.mod.config

import com.tamedphantoms.mod.TamedPhantomsMod
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.neoforged.neoforge.common.ModConfigSpec

/**
 * Конфиг типа SERVER (см. [net.neoforged.fml.config.ModConfig.Type.SERVER]).
 *
 * ## Как это работает в мультиплеере
 * Конфиг типа SERVER — единственный тип конфига NeoForge, который
 * автоматически рассылается сервером всем подключающимся клиентам, поэтому
 * все игроки на одном сервере всегда видят и используют одни и те же
 * значения (`tame_item`, `release_item` и т.д.), независимо от своих
 * локальных файлов. Весь код, читающий эти значения, выполняется только на
 * сервере (см. `PhantomInteractionHandler`, `PhantomTemptHandler`), поэтому
 * рассинхрона между игроками быть не должно. Подробнее — в README, раздел
 * "Конфиг и мультиплеер".
 *
 * Экран конфига в игре: "Mods" -> выбрать Tamed Phantoms -> кнопка "Config"
 * (см. [com.tamedphantoms.mod.client.ClientModEvents]).
 *
 * ВАЖНО про ключи локализации: NeoForge-экран конфига ищет их по шаблону
 * `<modid>.configuration.<путь>`, А НЕ `<modid>.config.<путь>` — это
 * подтверждено реальным примером стороннего мода (в его changelog отдельно
 * упомянуто, что raw-ключи вида `<modid>.configuration.<key>` заменялись на
 * человекочитаемые после добавления переводов). Ключи ниже используют
 * именно "configuration", и в паре с секцией `taming` (через `push`)
 * итоговый путь для, например, `tame_item` — `tamedphantoms.configuration.taming.tame_item`
 * (см. lang-файлы).
 */
class ServerConfig(builder: ModConfigSpec.Builder) {

    companion object {
        private const val KEY_PREFIX = "tamedphantoms.configuration"

        val SPEC: ModConfigSpec
        val CONFIG: ServerConfig

        init {
            val pair = ModConfigSpec.Builder().configure(::ServerConfig)
            CONFIG = pair.getLeft()
            SPEC = pair.getRight()
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

    private var cachedTameItemId: String? = null
    private var cachedTameItem: Item? = null
    private var cachedReleaseItemId: String? = null
    private var cachedReleaseItem: Item? = null

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

    /** Разрешает настроенный предмет приручения. Кэшируется, пересчитывается только если строка в конфиге изменилась. */
    fun resolveTameItem(): Item = resolveCached(
        tameItemId.get(),
        cachedTameItemId,
        cachedTameItem,
        Items.COOKIE,
        "tame_item",
    ) { id, item -> cachedTameItemId = id; cachedTameItem = item }

    /** Разрешает настроенный предмет освобождения. Кэшируется аналогично [resolveTameItem]. */
    fun resolveReleaseItem(): Item = resolveCached(
        releaseItemId.get(),
        cachedReleaseItemId,
        cachedReleaseItem,
        Items.POISONOUS_POTATO,
        "release_item",
    ) { id, item -> cachedReleaseItemId = id; cachedReleaseItem = item }

    private inline fun resolveCached(
        configured: String,
        cachedId: String?,
        cachedItem: Item?,
        fallback: Item,
        settingName: String,
        store: (String, Item) -> Unit,
    ): Item {
        if (cachedItem != null && cachedId == configured) return cachedItem
        val resolved = tryResolveItem(configured, fallback, settingName)
        store(configured, resolved)
        return resolved
    }

    private fun tryResolveItem(id: String, fallback: Item, settingName: String): Item {
        val location = ResourceLocation.tryParse(id)
        if (location == null) {
            TamedPhantomsMod.LOGGER.warn(
                "Некорректный ID предмета в настройке {}: '{}' — использую {} вместо него.",
                settingName, id, BuiltInRegistries.ITEM.getKey(fallback),
            )
            return fallback
        }
        val item = BuiltInRegistries.ITEM.getOptional(location).orElse(null)
        if (item == null || item === Items.AIR) {
            TamedPhantomsMod.LOGGER.warn(
                "Предмет '{}' из настройки {} не найден в реестре — использую {} вместо него.",
                id, settingName, BuiltInRegistries.ITEM.getKey(fallback),
            )
            return fallback
        }
        return item
    }
}
