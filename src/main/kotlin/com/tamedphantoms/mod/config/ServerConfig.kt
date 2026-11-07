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
        fun insomniaDays(): Int
        fun phantomGroupMultiplier(): Double
        fun phantomSize(): Int
        fun immortalPhantoms(): Boolean
        fun zoomBoost(): Double = DEFAULT_ZOOM_BOOST
        fun rollBoostLimit(): Double = DEFAULT_ROLL_BOOST_LIMIT
        fun rollBoostStep(): Double = DEFAULT_ROLL_BOOST_STEP
        fun rollBoostClimbDrain(): Double = DEFAULT_ROLL_CLIMB_DRAIN
        fun rollBoostSpin(): Double = DEFAULT_ROLL_SPIN
        fun rollBoostMinTicks(): Int = DEFAULT_ROLL_MIN_TICKS
    }

    const val DEFAULT_ZOOM_BOOST = 8.0
    const val MAX_ZOOM_BOOST = 64.0
    const val DEFAULT_ROLL_BOOST_LIMIT = 2.0
    const val MAX_ROLL_BOOST_LIMIT = 8.0
    const val DEFAULT_ROLL_BOOST_STEP = 0.25
    const val MAX_ROLL_BOOST_STEP = 2.0
    /** Сколько бонуса скорости пропадает за две секунды набора высоты. */
    const val DEFAULT_ROLL_CLIMB_DRAIN = 0.2
    const val MAX_ROLL_CLIMB_DRAIN = 4.0
    /** Доля длительности следующего оборота от предыдущего. */
    const val DEFAULT_ROLL_SPIN = 0.85
    const val MIN_ROLL_SPIN = 0.5
    const val DEFAULT_ROLL_MIN_TICKS = 6
    const val MAX_ROLL_MIN_TICKS = 16

    var settings: Settings = Defaults

    fun tameItemId(): String = settings.tameItemId()
    fun releaseItemId(): String = settings.releaseItemId()
    fun tameChance(): Double = settings.tameChance()
    fun repelRadius(): Double = settings.repelRadius()
    fun defendDurationTicks(): Int = settings.defendDurationTicks()
    fun flightSpeed(): Double = settings.flightSpeed()

    @JvmStatic
    fun insomniaDays(): Int = settings.insomniaDays()

    fun phantomGroupMultiplier(): Double = settings.phantomGroupMultiplier()
    fun phantomSize(): Int = settings.phantomSize()

    /** Выключено — «Властитель западных ветров» бессмертия не даёт, а уже бессмертные становятся обычными. */
    fun immortalPhantoms(): Boolean = settings.immortalPhantoms()

    /** Сила рывка вверх из мёртвой петли. 0 — рывка нет. Клиент пилота берёт её у сервера. */
    fun zoomBoost(): Double = settings.zoomBoost().coerceIn(0.0, MAX_ZOOM_BOOST)

    /** Потолок бонуса скорости за бочку. 0 — приёма на R нет, обычная бочка остаётся. */
    fun rollBoostLimit(): Double = settings.rollBoostLimit().coerceIn(0.0, MAX_ROLL_BOOST_LIMIT)

    /** Прибавка за один полный оборот заряженной бочки. */
    fun rollBoostStep(): Double = settings.rollBoostStep().coerceIn(0.0, MAX_ROLL_BOOST_STEP)

    /** Потеря бонуса за две секунды набора высоты. Петля и рывок из неё этот счётчик не трогают. */
    fun rollBoostClimbDrain(): Double = settings.rollBoostClimbDrain().coerceIn(0.0, MAX_ROLL_CLIMB_DRAIN)

    /** Насколько короче каждый следующий оборот набора. 1 — обороты одинаковые. */
    fun rollBoostSpin(): Double = settings.rollBoostSpin().coerceIn(MIN_ROLL_SPIN, 1.0)

    /** Самый короткий оборот набора, в тиках. */
    fun rollBoostMinTicks(): Int = settings.rollBoostMinTicks().coerceIn(2, MAX_ROLL_MIN_TICKS)

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
        override fun insomniaDays() = 1
        override fun phantomGroupMultiplier() = 2.0
        override fun phantomSize() = 4
        override fun immortalPhantoms() = true
    }
}
