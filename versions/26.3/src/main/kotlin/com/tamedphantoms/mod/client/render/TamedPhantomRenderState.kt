package com.tamedphantoms.mod.client.render

import net.minecraft.client.renderer.entity.state.PhantomRenderState
import net.minecraft.world.entity.monster.Phantom

/** Состояние кадра: ванильные поля плюс сущность, чтобы поза читала ручного фантома. */
class TamedPhantomRenderState : PhantomRenderState() {
    var phantom: Phantom? = null
    var partialTick: Float = 0f
}
