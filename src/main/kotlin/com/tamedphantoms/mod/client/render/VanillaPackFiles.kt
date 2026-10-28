package com.tamedphantoms.mod.client.render

import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import java.io.InputStream

/**
 * Чтение файла из ванильного набора, а не из общего менеджера.
 * Сборка 26.3 этот класс не копирует: там свой файл с чтением через менеджер.
 */
object VanillaPackFiles {
    fun open(location: ResourceLocation): InputStream? {
        val supplier = Minecraft.getInstance().vanillaPackResources.getResource(PackType.CLIENT_RESOURCES, location)
            ?: return null
        return supplier.get()
    }
}
