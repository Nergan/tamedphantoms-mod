# Tamed Phantoms — инструкции для AI-агентов

Мод Minecraft 1.21.1 для NeoForge и Fabric: приручить фантома, оседлать, посадить, вести и отпустить. Раскладка образца: общий код в `src/main`, NeoForge в `src/neoforge`, Fabric в проекте `fabric/`. Сборка 26.3 — отдельный проект `versions/26.3`.

## Опыт

Если по ходу задачи стало ясно то, что в следующий раз сэкономит неверный ход, допиши это в этот `AGENTS.md`. Ход работы и разовую ошибку не копируй. Неверную старую строку поправь, а не оставляй вторую рядом.

Взрывное зелье не проходит через `PhantomTreats.tryPotion`: эффект приходит в `LivingEntity.addEffect`. Подмена огнестойкости на сопротивление стоит там. `TamedPhantomEntity.kt` в `kotlinExcludes` сборки 26.3, поэтому ту же подмену нужно перенести и в копию `versions/26.3`, иначе выпущенный jar 26.3 останется со старым поведением. На 26.3 поле эффекта называется `MobEffects.RESISTANCE`, не `DAMAGE_RESISTANCE`: имя подменяет `rewriteFor26`.


## Скиллы

Перед правкой загрузчика прочитай подходящий скилл из https://github.com/SirEdvin/minecraft-modding-skills (`fabric-modding`, `neoforge-modding`, `neoforge-legacy-modding`, `minecraft-26-1-migration`, `modrinth-api`). Версии бери из `gradle.properties` этого репозитория: если скилл с ними разошёлся, верь файлу.

Перед багфиксом открой https://github.com/DietrichGebert/ponytail/blob/main/skills/ponytail/SKILL.md. Раздел «Готово» этот скилл не отменяет.Для этого мода читай `neoforge-modding` и `fabric-modding`. Вторую сборку не добавляй и не снимай, пока задача этого не просит. Ставится один набор, не оба сразу. Версию 26.3 не трогай, пока задача её прямо не назовёт.

Для правок `versions/26.3` дополнительно читай `minecraft-26-1-migration`.

## Правила

- Прежде чем писать своё или декомпилировать, поищи готовое решение, в первую очередь на GitHub.
- Код — Kotlin. Новый Java-файл не добавляй. Уже лежащий Java переписывай на Kotlin, когда трогаешь этот мод, не меняя поведение. Kotlin-миксин на Fabric 1.21.1 требует своих строк в `patchKotlinRefmap` (`fabric/build.gradle.kts`): Java-процессор refmap не видит Kotlin, и без них сервер ищет имя Mojmap.
- Имя jar: `<mod_id>-<loader>-<minecraft_version>-<mod_version>.jar`. В Prism и в чужую папку модов его не клади. Игрок качает GitHub Release, ту же сборку.
- Issues: чинить только дефект. На каждый issue ответить и закрыть, включая те, где правки нет.
- Коммит и пуш — только когда владелец прямо просит. Для пуша человек запускает `add-commit-push.cmd`. Теги `minecraft-*` двигает workflow релиза.
- Версии в `gradle.properties` не поднимай, пока задача этого не требует. Локальный путь к JDK туда не записывай.

Перед записью на CDN прочитай https://cdn.mikchan.net/ai/SKILL.md. Сначала `GET https://cdn.mikchan.net/version`. Токен не печатать и не выдумывать. Уже опубликованный номер версии не перезаписывается.

## Готово

Из каталога мода, по порядку. Сеть нужна. Собранный jar человеку для игры не отдавай.

1. Собери проект (`gradlew build --stacktrace`, либо `gradle build --stacktrace`, если wrapper нет).
2. Запусти клиент задачей Gradle и дождись главного меню, затем закрой окно.
3. Запусти локальный сервер и дождись готовности, затем останови его. Клиентский мод тоже не должен ронять сервер.
4. Если клиент или сервер упали, так и напиши. Сборка jar без запуска игры — не конец работы.


## Сборка 26.3

`versions/26.3` — отдельный Gradle-проект, не подпроект сборки 1.21.1. Её этими версиями не поднимай. Fabric внутри 26.3 — подпроект, корень мода для него `../../`, не `../`: иначе в jar не попадают `assets` и `data`. Каталог ресурсов 26.3 ставь в `srcDir` раньше родительского.

Иконка в `neoforge.mods.toml` 26.3 — `iconFile`: устаревший `logoFile` даёт при каждом запуске экран предупреждений, который ждёт клика. На 26.3 у предмета и блока есть `assets/<mod_id>/items/<id>.json`. Одного `models/item` мало. Сигнатуры бери `javap` из клиентского jar этой версии. `NativeImage.getPixel` и `setPixel` уже ARGB: вторую перестановку от старых RGBA не делай. Таблица добычи — одно поле `condition` с `type`, не массив `conditions`. Рецепт пишет ингредиент строкой. В workflow Gradle ровно `9.7.0`, JDK 25. Локальный сервер слушает `25567`. `@Shadow` на 26.3 видит поле только самого целевого класса; поле родителя читай `@Accessor` на классе, где оно объявлено. Parchment нет. Foojay resolver не подключай.

`checkEntityPostEffect` зовётся только при смене камеры и при F5, не каждый кадр. Шейдер всадника включай в `render` на 1.21.1 и в `update` на 26.3. `ConfigurationScreen` в 26.3 финальный: кнопку вешай из `ScreenEvent.Init.Post`. Мгновенное лечение и урон — `MobEffects.INSTANT_HEALTH` и `INSTANT_DAMAGE`. `SoundEvents.GENERIC_DRINK` — `Holder`, у звука бери `value()`. `Player` не наследник `Mob`, поэтому `mob !is Player` не компилируется.

`RenderPipelines.EYES` на 26.3 смешивает как обычную прозрачность. В текстуре должен лежать уже тот цвет, который нужно увидеть. Родителя `minecraft:item/template_spawn_egg` в 26.3 нет. Наклон свободного полёта не считай углом до близкой точки маршрута. Файлы из `kotlinExcludes` на 26.3 — отдельные копии: правка только родительского файла в сборку 26.3 не входит.

Правку 1.21.1 в копию 26.3 переноси `git merge-file <копия 26.3> <файл HEAD> <новый файл 1.21.1>`. Базу бери из `git show HEAD:<путь>` и переводи в CRLF, как рабочие файлы: с базой в LF конфликтует каждая строка. После слияния ищи в новых строках API 1.21.1. На 26.3 нет `Entity.interactAt`: точка клика приходит в `interact(player, hand, Vec3)`, на Fabric — в `UseEntityCallback`, на NeoForge — в `PlayerInteractEvent.EntityInteract.getLocation()`. Эта точка относительно сущности: мировая — `target.position().add(location)`. Без неё клик по седлу на NeoForge 26.3 считается кликом по телу. Надпись над хотбаром — `player.sendOverlayMessage(component)`, не `displayClientMessage(..., true)`. `DustParticleOptions` принимает `(int rgb, float)`. Вместо `isDay` и `moonPhase` — `isDarkOutside` и `getOverworldClockTime() / 24000 % 8`. В Kotlin-миксине 26.3 поле `at` у `@Redirect` — массив: `at = [At(...)]`, на 1.21.1 одиночный `At`.

На 26.3 все состояния рендера кадра собираются раньше, чем рисуются. Сущность, пойманная в `extractRenderState` в статическое поле, к `submit` и `setupAnim` уже другая. Данные для отрисовки храни по самому состоянию: `client/render/PhantomRenderCapture`. Регистр на 26.3: `Registry.get(id)` отдаёт `Optional<Holder>`, сам объект — `getOptional(id)`, он есть и на 1.21.1. Гайдбук 26.3 — Modonomicon, Patchouli для 26.3 нет. Книга лежит в `versions/26.3/src/main/resources/data/tamedphantoms/modonomicon`, рецепт — `phantom_guide_book.json`. Тексты страниц с разметкой Patchouli идут в Modonomicon через ключи `tamedphantoms.guide.md.*` в markdown. После правки текста книги обнови и их.

## API для аддонов

Облик фантома для аддонов (`bodyDye`, `saddleDye`, `luminous`, `startShake`) хранит, синхронизирует и рисует этот мод. Аддон только регистрирует обработчики в `TamedPhantomsApi`: клик с частью тела (`BODY` или `SADDLE`), освобождение и предметы седока (`registerRiderTool`: своего маунта прицел не выбирает, клик в воздух идёт лучом взгляда). Fabric отправляет `UseItemCallback` на сервер, только если клиент ответил `SUCCESS`: `CONSUME` на клиенте до сервера не доходит. Новое поле облика добавляй сюда, в обе копии сущности, а не в аддон. Изменение API — новая `mod_version`: аддон `tamedphantoms-colors-mod` указывает нижнюю границу версии. Аддон и сборка качают jar этого мода со страниц релиза, а страница держит только текущую версию. После нового `mod_version` подними `tamedphantoms_version` в аддоне и ссылку в `kidou-mods.yml`.

## Локальные инструменты

Kidou и Mikchan CDN — локальные инструменты этой мастерской. Искать сведения о них в интернете бесполезно: публичной документации нет. Для Kidou источник — спецификация в `D:\DATA\soft\code\projects\minecraft\modpacks\Kidou-1.1.4-release.jar` и `D:\DATA\soft\code\projects\minecraft\modpacks\AGENTS.md`. Для Mikchan CDN — https://cdn.mikchan.net/ai/SKILL.md и `GET https://cdn.mikchan.net/version`.

## Проверка мода и сборки

Работа над модом или сборкой заканчивается только после тестов, проверки запуском локального сервера и локального клиента. Сборка jar без этого не считается готовой.

