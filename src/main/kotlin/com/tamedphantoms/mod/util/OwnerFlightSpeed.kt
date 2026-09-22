package com.tamedphantoms.mod.util

import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Пожелание хозяина по скорости его прирученных фантомов.
 * Приходит пакетом с клиента и хранится только в памяти сервера.
 * Потолок всё равно серверный — он применяется в момент чтения.
 */
object OwnerFlightSpeed {

    private val byOwner = ConcurrentHashMap<UUID, Double>()

    fun init() {
        NeoForge.EVENT_BUS.addListener(::onLogout)
    }

    fun set(owner: UUID, speed: Double) {
        byOwner[owner] = speed.coerceIn(PhantomFlightPace.MIN, PhantomFlightPace.MAX)
    }

    fun get(owner: UUID?): Double? = owner?.let { byOwner[it] }

    fun clear(owner: UUID) {
        byOwner.remove(owner)
    }

    private fun onLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        clear(event.entity.uuid)
    }
}
