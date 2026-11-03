package com.tamedphantoms.mod.config

/**
 * Камера из седла и рывок из мёртвой петли есть в каждой выпущенной сборке.
 * Точка входа ставит флаг до сборки конфигов и сети: иначе нет настроек камеры,
 * `zoom_boost`, поля в пакете настроек и канала рывка.
 */
object RideExtras {
    var enabled: Boolean = false
}
