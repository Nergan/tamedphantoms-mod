import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.20"
    id("net.neoforged.moddev") version "2.0.146"
    `maven-publish`
    idea
}

// ---------------------------------------------------------------------
// Свойства из gradle.properties.
//
// ВАЖНО: имена свойств в gradle.properties — snake_case (mod_id,
// minecraft_version и т.д.), как принято в официальных шаблонах NeoForge.
// Делегат Kotlin `by project` ищет свойство С ТЕМ ЖЕ ИМЕНЕМ, что и сама
// Kotlin-переменная (т.е. "modId", а не "mod_id") — поэтому здесь читаем
// их явно через project.property(...), а не через `by project`.
// ---------------------------------------------------------------------
fun prop(name: String): String = project.property(name) as String

val modId = prop("mod_id")
val modVersion = prop("mod_version")
val modGroupId = prop("mod_group_id")
val modName = prop("mod_name")
val modLicense = prop("mod_license")
val modAuthors = prop("mod_authors")
val modDescription = prop("mod_description")
val minecraftVersion = prop("minecraft_version")
val minecraftVersionRange = prop("minecraft_version_range")
val neoVersion = prop("neo_version")
val loaderVersionRange = prop("loader_version_range")
val parchmentMinecraftVersion = prop("parchment_minecraft_version")
val parchmentMappingsVersion = prop("parchment_mappings_version")
val kffVersion = prop("kff_version")
val kffVersionRange = prop("kff_version_range")

version = modVersion
group = modGroupId

base {
    archivesName.set(modId)
}

// Mojang поставляет Java 21 в комплекте с 1.21.x — компилируем под неё.
java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

kotlin {
    jvmToolchain(21)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

sourceSets.main {
    resources {
        // Ресурсы, сгенерированные датагеном / шаблонизацией mods.toml.
        srcDir("src/generated/resources")
        exclude("**/*.bbmodel")
        exclude("src/generated/**/.cache")
    }
}

repositories {
    maven {
        name = "Kotlin for Forge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
    }
    mavenCentral()
}

neoForge {
    version = neoVersion

    parchment {
        mappingsVersion = parchmentMappingsVersion
        minecraftVersion = parchmentMinecraftVersion
    }

    // Access Transformer'ы этому моду не нужны: вся кастомная логика
    // живёт в наших собственных подклассах (protected-поля Mob доступны
    // по обычному Java/Kotlin-наследованию), поэтому файл не подключаем.

    runs {
        create("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        create("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        create("gameTestServer") {
            type = "gameTestServer"
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        create("data") {
            data()
            programArguments.addAll(
                listOf(
                    "--mod", modId,
                    "--all",
                    "--output", file("src/generated/resources/").absolutePath,
                    "--existing", file("src/main/resources/").absolutePath,
                )
            )
        }
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel.set(org.slf4j.event.Level.DEBUG)
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

// 'localRuntime' — как runtimeOnly, но не публикуется как транзитивная
// зависимость. ВАЖНО: саму конфигурацию нужно создавать на верхнем уровне
// блока configurations{}, а не внутри лямбды named("runtimeClasspath"){...}
// — вызов configurations.create(...) внутри чужой лямбды выполняется в
// контексте, где ContainerScope не позволяет создавать новые элементы
// контейнера (ошибка "cannot be executed in the current context").
val localRuntime by configurations.creating
configurations.named("runtimeClasspath") {
    extendsFrom(localRuntime)
}

dependencies {
    // --- Kotlin for Forge (обязательно вариант "-neoforge") ---
    implementation("thedarkcolour:kotlinforforge-neoforge:$kffVersion")

    // --- Тесты (чистая JVM, без игрового рантайма — см. README) ---
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// ---------------------------------------------------------------------
// Подстановка ${...} в src/main/templates/META-INF/neoforge.mods.toml.
//
// ВАЖНО: обычный Gradle-метод `expand(map)` прогоняет файл через движок
// Groovy-шаблонов (SimpleTemplateEngine), который пытается ПОЛНОЦЕННО
// распарсить содержимое как Groovy-код — и спотыкается о самые обычные
// символы в TOML-комментариях/строках (круглые скобки, доллары не в составе
// ${...} и т.п.), падая с "Unexpected input: '(' @ line 1". Поэтому вместо
// expand() используем простую, "тупую" построчную замену ТОЛЬКО подстрок
// вида ${key} на значения из карты — без интерпретации файла как кода.
// ---------------------------------------------------------------------
val generateModMetadata by tasks.registering(ProcessResources::class) {
    val replaceProperties = mapOf(
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "neo_version" to neoVersion,
        "loader_version_range" to loaderVersionRange,
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_license" to modLicense,
        "mod_version" to modVersion,
        "mod_authors" to modAuthors,
        "mod_description" to modDescription,
        "kff_version_range" to kffVersionRange,
    )
    inputs.properties(replaceProperties)
    from("src/main/templates")
    into("build/generated/sources/modMetadata")
    filteringCharset = "UTF-8"
    filter { line ->
        var result = line
        for ((key, value) in replaceProperties) {
            result = result.replace("\${$key}", value)
        }
        result
    }
}

sourceSets.main.get().resources.srcDir(generateModMetadata)
neoForge.ideSyncTask(generateModMetadata)

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.jar {
    manifest {
        attributes(
            "Specification-Title" to modId,
            "Specification-Vendor" to modAuthors,
            "Specification-Version" to "1",
            "Implementation-Title" to modName,
            "Implementation-Version" to modVersion,
            "Implementation-Vendor" to modAuthors,
        )
    }
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri("file://${project.projectDir}/repo")
        }
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
