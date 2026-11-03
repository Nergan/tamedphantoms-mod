package com.tamedphantoms.mod.util

/**
 * Корпус, на спине которого сидит седок. Сборка с камерой из седла отдаёт наклон корпуса;
 * без него глаза и взгляд седока остаются ванильными.
 */
interface PhantomCockpit {

    /** Наклон корпуса для седока. null — седок сидит ровно. */
    fun cockpitAttitude(partial: Float): Attitude?

    /** Курс, тангаж и крен корпуса в градусах, со знаками рендерера фантома. */
    class Attitude(val yaw: Float, val pitch: Float, val bank: Float)
}
