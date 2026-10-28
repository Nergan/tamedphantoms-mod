package com.tamedphantoms.mod.client.render

import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import java.io.InputStream

/** 26.3 читает текстуру через общий менеджер: отдельного вызова ванильного набора здесь нет. */
object VanillaPackFiles {
    fun open(location: Identifier): InputStream? {
        val found = Minecraft.getInstance().resourceManager.getResource(location)
        return if (found.isPresent) found.get().open() else null
    }
}
