package com.tamedphantoms.mod.util

/**
 * Путь звука прирученного и освобождённого фантома.
 * Чужой ресурс может подменить `minecraft:sounds/mob/phantom/`.
 * Эти двое читают копию из ванильного набора, дикий фантом — нет.
 */
object VanillaPhantomSoundPaths {
    fun redirectedLocation(namespace: String, path: String): String? {
        if (namespace == "tamedphantoms" && path.startsWith("vanilla/")) return null
        if (namespace != "minecraft" || !path.startsWith("mob/phantom/")) return null
        return "tamedphantoms:vanilla/$path"
    }

    fun bufferToVanillaFile(namespace: String, path: String): String? {
        val prefix = "sounds/vanilla/mob/phantom/"
        if (namespace != "tamedphantoms" || !path.startsWith(prefix) || !path.endsWith(".ogg")) return null
        return "minecraft:sounds/" + path.removePrefix("sounds/vanilla/")
    }
}
