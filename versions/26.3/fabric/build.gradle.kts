import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties
import org.gradle.api.tasks.Sync

plugins {
    id("net.fabricmc.fabric-loom") version "1.18.4"
    kotlin("jvm")
}

fun prop(name: String): String = project.property(name) as String

fun parentProp(name: String): String {
    val props = Properties()
    rootProject.file("../../gradle.properties").reader(Charsets.UTF_8).use { props.load(it) }
    return props.getProperty(name) ?: error("Missing $name in the 1.21.1 gradle.properties")
}

version = parentProp("mod_version")
group = parentProp("mod_group_id")

base {
    archivesName.set("tamedphantoms-fabric-${prop("minecraft_version")}")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(25))

val rewriteFor26 = rootProject.extra["rewriteFor26"] as (String) -> String

val kotlinExcludes = listOf(
    "com/tamedphantoms/mod/entity/TamedPhantomEntity.kt",
    "com/tamedphantoms/mod/entity/ai/TamedPhantomMoveControl.kt",
    "com/tamedphantoms/mod/client/render/**",
    "com/tamedphantoms/mod/client/ClientPhantomInputSender.kt",
    "com/tamedphantoms/mod/client/ModKeyMappings.kt",
    "com/tamedphantoms/mod/client/ClientPhantomSoundHandler.kt",
    "com/tamedphantoms/mod/client/VanillaPhantomBuffers.kt",
    "com/tamedphantoms/mod/mixin/VanillaPhantomBufferMixin.kt",
    "com/tamedphantoms/mod/mixin/PhantomFlapMixin.kt",
    "com/tamedphantoms/mod/loader/fabric/TamedPhantomsFabric.kt",
    "com/tamedphantoms/mod/loader/fabric/TamedPhantomsFabricClient.kt",
    "com/tamedphantoms/mod/loader/fabric/FabricNetworking.kt",
)

val sharedKotlin by tasks.registering(Sync::class) {
    from(rootProject.file("../../src/main/kotlin"))
    from(rootProject.file("../../fabric/src/main/kotlin"))
    into(layout.buildDirectory.dir("shared-kotlin"))
    exclude(kotlinExcludes)
    inputs.property("rewriteStamp", "flight-model-1")
    filter { line -> rewriteFor26(line) }
}

val sharedJava by tasks.registering(Sync::class) {
    from(rootProject.file("../../src/main/java"))
    from(rootProject.file("../../fabric/src/main/java"))
    into(layout.buildDirectory.dir("shared-java"))
    inputs.property("sharedJavaStamp", "util-fog-1")
    exclude(
        "com/tamedphantoms/mod/mixin/PhantomModelLookMixin.java",
        "com/tamedphantoms/mod/mixin/PlayerRideBankMixin.java",
        "com/tamedphantoms/mod/mixin/DataFixerSchemaMixin.java",
        "com/tamedphantoms/mod/mixin/MoonSkyMixin.java",
        "com/tamedphantoms/mod/loader/fabric/mixin/PhantomGroupSpawnMixin.java",
        "com/tamedphantoms/mod/loader/fabric/mixin/EntityPersistentDataMixin.java",
        "com/tamedphantoms/mod/loader/fabric/mixin/PhantomFogMixin.java",
        "com/tamedphantoms/mod/loader/fabric/mixin/PhantomCameraMixin.java",
        "com/tamedphantoms/mod/loader/fabric/mixin/PhantomSoundMixin.java",
    )
}

kotlin {
    jvmToolchain(25)
    sourceSets.named("main") {
        kotlin.srcDir(sharedKotlin)
        kotlin.srcDir(rootProject.file("src/main/kotlin"))
        kotlin.srcDir(file("src/main/kotlin"))
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

sourceSets.named("main") {
    java.srcDir(sharedJava)
    java.srcDir(rootProject.file("src/main/java"))
    java.srcDir(file("src/main/java"))
    resources.srcDir("src/main/resources")
    resources.srcDir(rootProject.file("src/main/resources"))
    // fabric/ is a subproject of versions/26.3, so the mod root is two levels up.
    resources.srcDir(rootProject.file("../../src/main/resources"))
    resources.srcDir(rootProject.file("../../fabric/src/main/resources"))
    resources.exclude("**/*.bbmodel")
}

repositories {
    maven {
        name = "Shedaniel"
        url = uri("https://maven.shedaniel.me/")
    }
    maven {
        name = "Terraformers"
        url = uri("https://maven.terraformersmc.com/releases/")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${prop("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${prop("fabric_loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${prop("fabric_api_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${prop("fabric_kotlin_version")}")
    implementation("me.shedaniel.cloth:cloth-config-fabric:${prop("cloth_config_version")}")
    compileOnly("com.terraformersmc:modmenu:${prop("modmenu_version")}")
}

tasks.processResources {
    val version = project.version.toString()
    inputs.property("version", version)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("data/tamedphantoms/recipe/phantom_guide.json")
    filesMatching("fabric.mod.json") {
        filter { line -> line.replace("\${version}", version) }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

loom {
    runs {
        named("server") {
            programArg("--nogui")
            programArg("--port")
            programArg("25567")
            vmArg("-Xms128m")
            vmArg("-Xmx512m")
            vmArg("-XX:+UseSerialGC")
        }
        named("client") {
            vmArg("-Xms256m")
            vmArg("-Xmx1024m")
            vmArg("-XX:+UseSerialGC")
        }
    }
}
