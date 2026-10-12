pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "NeoForged"
            url = uri("https://maven.neoforged.net/releases")
        }
        maven {
            name = "Kotlin for Forge"
            url = uri("https://thedarkcolour.github.io/KotlinForForge/")
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "Shedaniel"
            url = uri("https://maven.shedaniel.me/")
        }
        maven {
            name = "Terraformers"
            url = uri("https://maven.terraformersmc.com/releases/")
        }
        maven {
            name = "Minecraft libraries"
            url = uri("https://libraries.minecraft.net/")
        }
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

rootProject.name = "tamedphantoms-26.3"
include("fabric")
