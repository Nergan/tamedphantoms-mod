package com.tamedphantoms.mod.entity

import com.tamedphantoms.mod.TamedPhantomsMod
import com.tamedphantoms.mod.config.ServerConfig
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.phys.Vec3
import java.lang.reflect.Field
import java.util.WeakHashMap
import kotlin.math.hypot

/**
 * Прирученные и освобождённые фантомы отпугивают диких. Дикий бросает цель,
 * уходит выше и в сторону, а якорь его кружения переезжает туда же.
 * Ваниль делает так же от кошек, только в 16 блоках.
 */
object PhantomFear {

    private const val SCAN_TICKS = 10
    private const val FLEE_DISTANCE = 28.0
    private const val FLEE_RISE = 14.0

    // Боевой Fabric 1.21.1 держит промежуточные имена, NeoForge и 26.3 — имена Mojang.
    private val anchorField: Field? = findField("anchorPoint", "field_7312")
    private val phaseField: Field? = findField("attackPhase", "field_7315")
    private var warned = false

    private class Seen(val tick: Int, val protector: TamedPhantomEntity?)

    private val cache = WeakHashMap<Phantom, Seen>()

    /** Ближайший прирученный или освобождённый фантом в радиусе отпугивания. */
    fun protector(phantom: Phantom): TamedPhantomEntity? {
        if (phantom is TamedPhantomEntity) return null
        val seen = cache[phantom]
        if (seen != null && phantom.tickCount - seen.tick in 0 until SCAN_TICKS) {
            return seen.protector?.takeIf { it.isAlive && it.level() === phantom.level() }
        }
        val radius = ServerConfig.repelRadius()
        val found = if (radius <= 0.0) {
            null
        } else {
            phantom.level().getEntitiesOfClass(TamedPhantomEntity::class.java, phantom.boundingBox.inflate(radius)) {
                it.isAlive && it.distanceToSqr(phantom) <= radius * radius
            }.minByOrNull { it.distanceToSqr(phantom) }
        }
        cache[phantom] = Seen(phantom.tickCount, found)
        return found
    }

    /** Точка, куда уходит дикий: от защитника по горизонтали и заметно выше него. */
    fun fleePoint(phantom: Phantom, protector: TamedPhantomEntity): Vec3 {
        var dx = phantom.x - protector.x
        var dz = phantom.z - protector.z
        val len = hypot(dx, dz)
        if (len < 0.5) {
            val angle = phantom.random.nextDouble() * Math.PI * 2.0
            dx = Math.cos(angle)
            dz = Math.sin(angle)
        } else {
            dx /= len
            dz /= len
        }
        val level = phantom.level()
        val top = (level.maxBuildHeight - 4).toDouble()
        val y = maxOf(protector.y + FLEE_RISE, phantom.y + 4.0).coerceAtMost(top)
        return Vec3(protector.x + dx * FLEE_DISTANCE, y, protector.z + dz * FLEE_DISTANCE)
    }

    /** Дикий кружит у новой точки, а не пикирует обратно к старой цели. */
    fun settle(phantom: Phantom, at: Vec3) {
        try {
            anchorField?.set(phantom, BlockPos.containing(at))
            val phase = phaseField ?: return
            val circle = phase.type.enumConstants?.firstOrNull() ?: return
            phase.set(phantom, circle)
        } catch (t: Throwable) {
            if (!warned) {
                warned = true
                TamedPhantomsMod.LOGGER.warn("Не удалось сдвинуть кружение дикого фантома.", t)
            }
        }
    }

    private fun findField(vararg names: String): Field? = names.firstNotNullOfOrNull { name ->
        runCatching { Phantom::class.java.getDeclaredField(name).apply { isAccessible = true } }.getOrNull()
    }
}
