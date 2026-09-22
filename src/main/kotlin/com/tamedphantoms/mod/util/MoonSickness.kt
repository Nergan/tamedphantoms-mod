package com.tamedphantoms.mod.util

/** Секунда болезненно-жёлтой луны после полнолунного крика. */
object MoonSickness {

    @Volatile
    var ticks: Int = 0

    fun trigger() {
        ticks = 20
    }

    fun tick() {
        if (ticks > 0) ticks--
    }

    fun strength(): Float = (ticks / 20f).coerceIn(0f, 1f)
}
