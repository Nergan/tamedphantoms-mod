pluginManagement {
    repositories {
        // Репозиторий NeoForge (плагин net.neoforged.moddev и сам NeoForge).
        maven {
            name = "NeoForged"
            url = uri("https://maven.neoforged.net/releases")
        }
        // Репозиторий Kotlin for Forge.
        maven {
            name = "Kotlin for Forge"
            url = uri("https://thedarkcolour.github.io/KotlinForForge/")
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Автоматически находит/скачивает подходящий JDK для тулчейна (Java 21).
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
    repositories {
        maven {
            name = "NeoForged"
            url = uri("https://maven.neoforged.net/releases")
        }
        maven {
            name = "Kotlin for Forge"
            url = uri("https://thedarkcolour.github.io/KotlinForForge/")
        }
        mavenCentral()
    }
}

rootProject.name = "tamedphantoms"
