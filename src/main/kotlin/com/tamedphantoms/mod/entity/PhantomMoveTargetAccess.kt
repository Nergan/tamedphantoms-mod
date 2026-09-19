package com.tamedphantoms.mod.entity

import com.tamedphantoms.mod.TamedPhantomsMod
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.phys.Vec3
import java.lang.reflect.Field

/**
 * Ванильный [Phantom.PhantomMoveControl] двигает фантома только к полю
 * `moveTargetPoint` и полностью игнорирует [net.minecraft.world.entity.ai.control.MoveControl.setWantedPosition].
 * Поле пакетное, поэтому для диких фантомов выставляем его рефлексией.
 */
object PhantomMoveTargetAccess {

    private val moveTargetField: Field? = findField("moveTargetPoint")
    private var warned = false

    fun set(phantom: Phantom, target: Vec3): Boolean {
        val field = moveTargetField
        if (field == null) {
            if (!warned) {
                warned = true
                TamedPhantomsMod.LOGGER.warn("Не удалось получить Phantom.moveTargetPoint — приманивание диких фантомов может не сработать.")
            }
            return false
        }
        return try {
            field.set(phantom, target)
            true
        } catch (t: Throwable) {
            if (!warned) {
                warned = true
                TamedPhantomsMod.LOGGER.warn("Не удалось записать Phantom.moveTargetPoint", t)
            }
            false
        }
    }

    private fun findField(name: String): Field? {
        return try {
            Phantom::class.java.getDeclaredField(name).apply { isAccessible = true }
        } catch (_: Throwable) {
            null
        }
    }
}
