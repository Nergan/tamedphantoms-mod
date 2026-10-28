import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("fabric-loom") version "1.9.2"
    kotlin("jvm") version "2.1.20"
}

fun prop(name: String): String = project.property(name) as String

version = prop("mod_version")
group = prop("mod_group_id")

base {
    archivesName.set("tamedphantoms-fabric-${prop("minecraft_version")}")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

kotlin {
    jvmToolchain(21)
    sourceSets.named("main") {
        kotlin.srcDir(rootProject.file("src/main/kotlin"))
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

sourceSets.named("main") {
    java.srcDir(rootProject.file("src/main/java"))
    resources.srcDir(rootProject.file("src/main/resources"))
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
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${prop("fabric_loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("fabric_api_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${prop("fabric_kotlin_version")}")
    modImplementation("me.shedaniel.cloth:cloth-config-fabric:${prop("cloth_config_version")}")
    modCompileOnly("com.terraformersmc:modmenu:${prop("modmenu_version")}")
}

tasks.processResources {
    val version = project.version.toString()
    inputs.property("version", version)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    // Рецепт NeoForge лежит по тому же пути. В jar Fabric остаётся файл из fabric/src.
    exclude { details ->
        val path = details.file.invariantSeparatorsPath
        path.endsWith("data/tamedphantoms/recipe/phantom_guide.json") && "/fabric/" !in path
    }
    filesMatching("fabric.mod.json") {
        filter { line -> line.replace("\${version}", version) }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

// Java annotation processor writes the refmap and does not see Kotlin mixins.
// Fabric production needs those names, or the server looks for the Mojmap method.
val patchKotlinRefmap by tasks.registering {
    dependsOn(tasks.named("compileJava"))
    doLast {
        val refmap = layout.buildDirectory.file("classes/java/main/${base.archivesName.get()}-refmap.json").get().asFile
        val slurper = groovy.json.JsonSlurper()
        @Suppress("UNCHECKED_CAST")
        val root = slurper.parse(refmap) as MutableMap<String, Any?>
        val extra = mapOf(
            "com/tamedphantoms/mod/mixin/PhantomInteractMixin" to mapOf(
                "interactOn" to "Lnet/minecraft/class_1657;method_7287(Lnet/minecraft/class_1297;Lnet/minecraft/class_1268;)Lnet/minecraft/class_1269;",
            ),
            "com/tamedphantoms/mod/mixin/VanillaPhantomBufferMixin" to mapOf(
                "getCompleteBuffer" to "Lnet/minecraft/class_4237;method_19743(Lnet/minecraft/class_2960;)Ljava/util/concurrent/CompletableFuture;",
                "getStream" to "Lnet/minecraft/class_4237;method_19744(Lnet/minecraft/class_2960;Z)Ljava/util/concurrent/CompletableFuture;",
            ),
        )
        fun putAll(target: MutableMap<String, Any?>) {
            for ((mixin, methods) in extra) {
                @Suppress("UNCHECKED_CAST")
                val current = target[mixin] as? MutableMap<String, Any?> ?: mutableMapOf()
                current.putAll(methods)
                target[mixin] = current
            }
        }
        @Suppress("UNCHECKED_CAST")
        val mappings = root["mappings"] as MutableMap<String, Any?>
        putAll(mappings)
        @Suppress("UNCHECKED_CAST")
        val data = root["data"] as MutableMap<String, Any?>
        @Suppress("UNCHECKED_CAST")
        val named = data["named:intermediary"] as MutableMap<String, Any?>
        putAll(named)
        refmap.writeText(groovy.json.JsonOutput.prettyPrint(groovy.json.JsonOutput.toJson(root)), Charsets.UTF_8)
    }
}

tasks.named("classes") {
    dependsOn(patchKotlinRefmap)
}
