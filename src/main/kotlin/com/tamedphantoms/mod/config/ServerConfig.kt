package com.tamedphantoms.mod.config

import com.tamedphantoms.mod.TamedPhantomsMod
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items

/**
 * Значения серверных правил. NeoForge и Fabric подставляют свой источник:
 * файл мира у NeoForge синхронизируется самим загрузчиком, Fabric шлёт
 * пакет при входе. Пока источник не подключён, читаются значения по умолчанию.
 */
object ServerConfig {

    interface Settings {
        fun tameItemId(): String
        fun releaseItemId(): String
        fun tameChance(): Double
        fun repelRadius(): Double
        fun defendDurationTicks(): Int
        fun flightSpeed(): Double
        fun acrobaticsStep(): Double
        fun screamRadius(): Double
        fun screamCooldownSeconds(): Int
        fun insomniaDays(): Int
        fun phantomGroupMultiplier(): Double
        fun phantomSize(): Int
    }

    var settings: Settings = Defaults

    fun tameItemId(): String = settings.tameItemId()
    fun releaseItemId(): String = settings.releaseItemId()
    fun tameChance(): Double = settings.tameChance()
    fun repelRadius(): Double = settings.repelRadius()
    fun defendDurationTicks(): Int = settings.defendDurationTicks()
    fun flightSpeed(): Double = settings.flightSpeed()
    fun acrobaticsStep(): Double = settings.acrobaticsStep()
    fun screamRadius(): Double = settings.screamRadius()
    fun screamCooldownSeconds(): Int = settings.screamCooldownSeconds()

    @JvmStatic
    fun insomniaDays(): Int = settings.insomniaDays()

    fun phantomGroupMultiplier(): Double = settings.phantomGroupMultiplier()
    fun phantomSize(): Int = settings.phantomSize()

    private var cachedTameItemId: String? = null
    private var cachedTameItem: Item? = null
    private var cachedReleaseItemId: String? = null
    private var cachedReleaseItem: Item? = null

    fun resolveTameItem(): Item = resolveCached(
        tameItemId(),
        cachedTameItemId,
        cachedTameItem,
        Items.COOKIE,
        "tame_item",
    ) { id, item -> cachedTameItemId = id; cachedTameItem = item }

    fun resolveReleaseItem(): Item = resolveCached(
        releaseItemId(),
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

    private object Defaults : Settings {
        override fun tameItemId() = "minecraft:cookie"
        override fun releaseItemId() = "minecraft:poisonous_potato"
        override fun tameChance() = 1.0
        override fun repelRadius() = 64.0
        override fun defendDurationTicks() = 200
        override fun flightSpeed() = 1.25
        override fun acrobaticsStep() = 0.3
        override fun screamRadius() = 32.0
        override fun screamCooldownSeconds() = 4
        override fun insomniaDays() = 1
        override fun phantomGroupMultiplier() = 2.0
        override fun phantomSize() = 3
    }
}
