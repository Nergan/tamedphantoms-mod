package com.tamedphantoms.mod.compat

import net.minecraft.util.Mth

/** Kotlin не расширяет Float до Double в вызовах Java, а sin/cos в 26.3 принимают только double. */
object FloatMath {
    fun sin(value: Float): Float = Mth.sin(value.toDouble())

    fun sin(value: Double): Float = Mth.sin(value)

    fun cos(value: Float): Float = Mth.cos(value.toDouble())

    fun cos(value: Double): Float = Mth.cos(value)
}
