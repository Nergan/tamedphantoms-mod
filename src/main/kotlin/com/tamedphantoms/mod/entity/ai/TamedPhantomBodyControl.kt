package com.tamedphantoms.mod.entity.ai

import com.tamedphantoms.mod.entity.TamedPhantomEntity
import net.minecraft.world.entity.ai.control.BodyRotationControl

/**
 * Тело смотрит туда, куда летит. Голову не приклеивает к телу:
 * ванильный контроль фантома каждый тик затирал yHeadRot.
 */
class TamedPhantomBodyControl(private val phantom: TamedPhantomEntity) : BodyRotationControl(phantom) {

    override fun clientTick() {
        phantom.yBodyRot = phantom.yRot
    }
}
