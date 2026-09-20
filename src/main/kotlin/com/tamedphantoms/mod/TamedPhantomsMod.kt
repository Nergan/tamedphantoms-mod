package com.tamedphantoms.mod

import com.tamedphantoms.mod.config.ClientConfig
import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.entity.ModEntities
import com.tamedphantoms.mod.item.ModItems
import com.tamedphantoms.mod.event.ModSetup
import com.tamedphantoms.mod.event.PhantomDamageHandler
import com.tamedphantoms.mod.event.PhantomGuideHandler
import com.tamedphantoms.mod.event.PhantomInteractionHandler
import com.tamedphantoms.mod.event.PhantomOwnerRecallHandler
import com.tamedphantoms.mod.event.PhantomTemptHandler
import com.tamedphantoms.mod.network.ModNetworking
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.neoforge.common.NeoForge
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * Точка входа мода.
 *
 * ВАЖНО про архитектурное решение "обычный класс, а не Kotlin `object`":
 * изначально по духу Kotlin for Forge (KFF) главный класс мода делают
 * Kotlin `object`-синглтоном. Но чтобы предмет приручения (и другие
 * параметры) можно было менять через полноценный конфиг NeoForge
 * с сохранением в конфиг-файл, вебом сервера и in-game экраном настроек,
 * NeoForge требует получить [ModContainer] — а официальный способ его
 * получить — это параметр КОНСТРУКТОРА мод-класса (см.
 * https://docs.neoforged.net/docs/1.21.1/misc/config/#registering-a-configuration).
 * У Kotlin `object` параметризованного конструктора быть не может, поэтому
 * здесь используется обычный `class` — как это принято для "чистого"
 * NeoForge/Java мода.
 *
 * Kotlin for Forge при этом всё равно используется и остаётся обязательной
 * зависимостью мода: она даёт удобный синтаксис делегата `by ...register(...)`
 * для `DeferredRegister` (см. `entity/ModEntities.kt`), а он никак не
 * привязан к тому, `object` у нас точка входа или `class`. Поэтому
 * modLoader в neoforge.mods.toml — стандартный `"javafml"` (обычный класс
 * с таким конструктором прекрасно загружается им "из коробки", без
 * специального загрузчика KFF), а зависимость на мод `kotlinforforge`
 * прописана отдельным блоком `[[dependencies]]`.
 */
@Mod(TamedPhantomsMod.MOD_ID)
class TamedPhantomsMod(modEventBus: IEventBus, modContainer: ModContainer) {

    companion object {
        const val MOD_ID = "tamedphantoms"

        @JvmField
        val LOGGER: Logger = LogManager.getLogger(MOD_ID)
    }

    init {
        LOGGER.info("Инициализация мода Tamed Phantoms ({})", MOD_ID)

        // Регистрируем DeferredRegister с типами сущностей на шине мода.
        ModEntities.ENTITY_TYPES.register(modEventBus)
        ModItems.register(modEventBus)

        // Серверный конфиг (предмет приручения, шанс приручения и т.д.).
        // Тип SERVER выбран осознанно: значения синхронизируются с клиентами
        // при подключении к серверу (см. README, раздел "Конфиг и мультиплеер"),
        // так что все игроки на одном сервере всегда видят одинаковые правила.
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC)
        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC)

        // Остальная инициализация (клиентская настройка — рендереры, экран
        // конфига и т.п.) — в отдельном объекте, вызывается условно только
        // на клиенте, чтобы не трогать клиентские классы на выделенном сервере.
        ModSetup.init(modEventBus, modContainer)

        // Сеть (состояние клавиш взлёта/снижения клиент -> сервер).
        ModNetworking.init(modEventBus)

        // Обработчики игровых событий (взаимодействия, урон, "приманивание"
        // диких фантомов едой) — на общую игровую шину NeoForge (а не шину
        // мода, которая только для загрузки).
        NeoForge.EVENT_BUS.register(PhantomInteractionHandler)
        NeoForge.EVENT_BUS.register(PhantomDamageHandler)
        NeoForge.EVENT_BUS.register(PhantomTemptHandler)
        NeoForge.EVENT_BUS.register(PhantomGuideHandler)
        NeoForge.EVENT_BUS.register(PhantomOwnerRecallHandler)
    }
}
