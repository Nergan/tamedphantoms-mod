import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties
import org.gradle.api.tasks.Sync

plugins {
    kotlin("jvm") version "2.4.0"
    id("net.neoforged.moddev") version "2.0.148"
    `maven-publish`
    idea
}

fun prop(name: String): String = project.property(name) as String

fun parentProp(name: String): String {
    val props = Properties()
    file("../../gradle.properties").reader(Charsets.UTF_8).use { props.load(it) }
    return props.getProperty(name) ?: error("Missing $name in the 1.21.1 gradle.properties")
}

fun rewriteIdentifiers(line: String): String = rewriteFor26(line)

fun rewriteFor26(line: String): String {
    var s = line
    val pairs = listOf(
        // 26.3 getPixel/setPixel already speak ARGB. The 1.21.1 helpers swap ABGR and would tint twice.
        "val native = image.getPixelRGBA(x, y)" to "val argb = image.getPixel(x, y)",
        "val argb = nativeToArgb(native)" to "",
        "image.setPixelRGBA(x, y, argbToNative(newArgb))" to "image.setPixel(x, y, newArgb)",
        "image.setPixelRGBA(x, y, argbToNative(color))" to "image.setPixel(x, y, color)",
        "textures/entity/phantom_eyes.png" to "textures/entity/phantom/phantom_eyes.png",
        "textures/entity/phantom.png" to "textures/entity/phantom/phantom.png",
        "net.minecraft.resources.ResourceLocation" to "net.minecraft.resources.Identifier",
        "ResourceLocation" to "Identifier",
        "net.minecraft.client.model.PhantomModel" to "net.minecraft.client.model.monster.phantom.PhantomModel",
        "net.minecraft.client.renderer.RenderType" to "net.minecraft.client.renderer.rendertype.RenderType",
        "net.minecraft.world.entity.MobSpawnType" to "net.minecraft.world.entity.EntitySpawnReason",
        "MobSpawnType" to "EntitySpawnReason",
        "isControlledByLocalInstance" to "isLocalInstanceAuthoritative",
        ".displayClientMessage(" to ".sendSystemMessage(",
        ".minBuildHeight" to ".dimensionType().minY()",
        ".maxBuildHeight" to ".dimensionType().let { it.minY() + it.height() }",
        ".serverLevel()" to ".level()",
        "net.minecraft.world.entity.RelativeMovement" to "net.minecraft.world.entity.Relative",
        "RelativeMovement" to "Relative",
        "dropLeash(true, true)" to "dropLeash()",
        "dropLeash(true, dropLead)" to "dropLeash()",
        "dropLeash(true, !player.abilities.instabuild)" to "dropLeash()",
        "canHaveALeashAttachedToIt()" to "canHaveALeashAttachedTo(player)",
        "setPixelRGBA" to "setPixel",
        "getPixelRGBA" to "getPixel",
        "FMLEnvironment.dist" to "FMLEnvironment.getDist()",
        "ModConfig.Type.SERVER" to "ModConfig.Type.SYNCED",
        "player.server.advancements.get(advancementId)" to "player.level().getServer().getAdvancements().get(advancementId)",
        "player.advancements.award" to "player.getAdvancements().award",
        "event.newDamage" to "event.healthDamage",
        "startRiding(phantom, true)" to "startRiding(phantom)",
        "Mth.sin(" to "com.tamedphantoms.mod.compat.FloatMath.sin(",
        "Mth.cos(" to "com.tamedphantoms.mod.compat.FloatMath.cos(",
        "target.hurt(phantom.damageSources().mobAttack(phantom), damage)" to
            "target.hurtServer(level, phantom.damageSources().mobAttack(phantom), damage)",
        "phantom.swing(net.minecraft.world.InteractionHand.MAIN_HAND)" to
            "phantom.swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true)",
        "player.drop(book, false)" to "player.drop(book, false, net.minecraft.util.Prediction.SERVER_ONLY)",
        "BuiltInRegistries.ITEM.get(Identifier.parse(\"patchouli:guide_book\"))" to
            "BuiltInRegistries.ITEM.get(Identifier.parse(\"patchouli:guide_book\")).orElse(null)",
        "if (item === Items.AIR) return null" to "if (item == null || item.value() === Items.AIR) return null",
        "BuiltInRegistries.DATA_COMPONENT_TYPE.get(Identifier.parse(\"patchouli:book\"))" to
            "BuiltInRegistries.DATA_COMPONENT_TYPE.get(Identifier.parse(\"patchouli:book\")).orElse(null)",
        ".getList(TAG_OWNED, Tag.TAG_COMPOUND.toInt())" to ".getListOrEmpty(TAG_OWNED)",
        "list.getCompound(i)" to "list.getCompoundOrEmpty(i)",
        "stored.getCompound(i)" to "stored.getCompoundOrEmpty(i)",
        "tag.getString(\"D\")" to "tag.getStringOr(\"D\", \"\")",
        "tag.getDouble(\"X\")" to "tag.getDoubleOr(\"X\", 0.0)",
        "tag.getDouble(\"Y\")" to "tag.getDoubleOr(\"Y\", 0.0)",
        "tag.getDouble(\"Z\")" to "tag.getDoubleOr(\"Z\", 0.0)",
        "tag.hasUUID(\"U\")" to "tag.getString(\"U\").isPresent",
        "tag.getUUID(\"U\")" to "java.util.UUID.fromString(tag.getString(\"U\").orElse(\"\"))",
        "entry.putUUID(\"U\", phantom.uuid)" to "entry.putString(\"U\", phantom.uuid.toString())",
        "entry.putUUID(\"U\", " to "entry.putString(\"U\", ",
        ".dimension().location()" to ".dimension().identifier()",
        "import net.minecraft.world.entity.FlyingMob" to "",
        "mob is FlyingMob || " to "",
        "if (blindness && mob !is Player)" to "if (blindness)",
        "DynamicTexture(" to "DynamicTexture({ \"tamedphantoms\" }, ",
        "EntityType.PHANTOM" to "com.tamedphantoms.mod.compat.VanillaTypes.phantom()",
        ".create(server)" to ".create(server, net.minecraft.world.entity.EntitySpawnReason.NATURAL)",
        "copy.moveTo(" to "copy.snapTo(",
        "tamed.moveTo(" to "tamed.snapTo(",
        "player.storedData.getBoolean(TAG_RECEIVED)" to "player.storedData.getBooleanOr(TAG_RECEIVED, false)",
        "oldPlayer.storedData.getBoolean(TAG_RECEIVED)" to "oldPlayer.storedData.getBooleanOr(TAG_RECEIVED, false)",
        "emptySet<Relative>(), phantom.yRot, phantom.xRot)" to
            "emptySet<Relative>(), phantom.yRot, phantom.xRot, true)",
        "fallDistance = 0.0f" to "fallDistance = 0.0",
        "val server = owner.server ?: return" to
            "val server = (owner as? net.minecraft.server.level.ServerPlayer)?.level()?.getServer() ?: return",
        "fun isFullMoonNight(level: ServerLevel): Boolean = level.moonPhase == 0 && level.isNight" to
            "fun isFullMoonNight(level: ServerLevel): Boolean = ((level.getOverworldClockTime() / 24000L) % 8L).toInt() == 0 && level.isDarkOutside",
        // 26.3 has no interactAt: the click point arrives in the event, relative to the target.
        "PhantomInteractionHandler.onEntityInteract(event.entity, event.target, event.hand)" to
            "PhantomInteractionHandler.onEntityInteract(event.entity, event.target, event.hand, event.target.position().add(event.location))",
        "night = !level.isDay," to "night = level.isDarkOutside,",
        "moonPhase = level.moonPhase," to "moonPhase = ((level.getOverworldClockTime() / 24000L) % 8L).toInt(),",
    )
    for ((from, to) in pairs) {
        s = s.replace(from, to)
    }
    return s
}

extra["rewriteFor26"] = { line: String -> rewriteFor26(line) }

val modId = parentProp("mod_id")
val modVersion = parentProp("mod_version")
val modGroupId = parentProp("mod_group_id")
val modName = parentProp("mod_name")
val modLicense = parentProp("mod_license")
val modAuthors = parentProp("mod_authors")
val modDescription = parentProp("mod_description")
val minecraftVersion = prop("minecraft_version")
val minecraftVersionRange = prop("minecraft_version_range")
val neoVersion = prop("neo_version")
val kffVersion = prop("kff_version")
val kffVersionRange = prop("kff_version_range")

version = modVersion
group = modGroupId

base {
    archivesName.set("${modId}-neoforge-${minecraftVersion}")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(25))

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
    "com/tamedphantoms/mod/client/FlightAwareSectionScreen.kt",
    "com/tamedphantoms/mod/client/ClientModEvents.kt",
    "com/tamedphantoms/mod/TamedPhantomsNeoForge.kt",
    "com/tamedphantoms/mod/loader/NeoForgeRegistries.kt",
)

val sharedKotlin by tasks.registering(Sync::class) {
    from(file("../../src/main/kotlin"))
    from(file("../../src/neoforge/kotlin"))
    into(layout.buildDirectory.dir("shared-kotlin"))
    exclude(kotlinExcludes)
    inputs.property("rewriteStamp", "interact-location-1")
    filter { line -> rewriteIdentifiers(line) }
}

val sharedJava by tasks.registering(Sync::class) {
    from(file("../../src/main/java"))
    from(file("../../src/neoforge/java"))
    into(layout.buildDirectory.dir("shared-java"))
    inputs.property("sharedJavaStamp", "util-fog-1")
    exclude(
        "com/tamedphantoms/mod/mixin/PhantomModelLookMixin.java",
        "com/tamedphantoms/mod/mixin/PlayerRideBankMixin.java",
        "com/tamedphantoms/mod/mixin/DataFixerSchemaMixin.java",
        "com/tamedphantoms/mod/mixin/MoonSkyMixin.java",
    )
}

kotlin {
    jvmToolchain(25)
    sourceSets.named("main") {
        kotlin.srcDir(sharedKotlin)
        kotlin.srcDir(file("src/main/kotlin"))
        kotlin.srcDir(file("src/neoforge/kotlin"))
    }
    sourceSets.named("test") {
        kotlin.srcDir(file("../../src/test/kotlin"))
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

sourceSets.main {
    java.srcDir(sharedJava)
    java.srcDir(file("src/main/java"))
    resources.srcDir("src/main/resources")
    resources.srcDir(file("../../src/main/resources"))
    resources.srcDir(file("../../src/neoforge/resources"))
    resources {
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
    enable {
        version = neoVersion
        // CI sets this automatically. Locally it skips Vineflower, which needs more RAM than this machine has.
        setDisableRecompilation(true)
    }

    runs {
        create("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
            jvmArguments.addAll(listOf("-Xms256m", "-Xmx1024m", "-XX:+UseSerialGC"))
        }
        create("server") {
            server()
            programArguments.addAll(listOf("--nogui", "--port", "25567"))
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
            jvmArguments.addAll(listOf("-Xms256m", "-Xmx768m", "-XX:+UseSerialGC"))
        }
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel.set(org.slf4j.event.Level.INFO)
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

val localRuntime by configurations.creating
configurations.named("runtimeClasspath") {
    extendsFrom(localRuntime)
}

dependencies {
    implementation("thedarkcolour:kotlinforforge-neoforge:$kffVersion")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

val generateModMetadata by tasks.registering(ProcessResources::class) {
    val replaceProperties = mapOf(
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "neo_version" to neoVersion,
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

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("data/tamedphantoms/recipe/phantom_guide.json")
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
