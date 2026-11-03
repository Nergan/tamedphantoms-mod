package com.tamedphantoms.mod.util

/**
 * Взгляд седока с наклонённого корпуса. Седок сидит на спине фантома, а не висит над ним:
 * глаза и взгляд поворачиваются вместе с корпусом вокруг его центра, как модель седока
 * в третьем лице ([com.tamedphantoms.mod.client.PhantomRiderPose]). Знаки как у `Camera`.
 */
object PhantomRiderSight {

    /** Наклон корпуса в осях его курса: тангаж, затем крен. */
    fun bodyTilt(pitch: Float, bank: Float): PhantomQuat =
        PhantomQuat.x(-pitch * RAD) * PhantomQuat.z(bank * RAD)

    /** Тот же наклон в мировых осях при курсе [yaw]: так рендерер крутит модель седока. */
    fun worldTilt(yaw: Float, tilt: PhantomQuat): PhantomQuat =
        PhantomQuat.y(-yaw * RAD) * tilt * PhantomQuat.y(yaw * RAD)

    /** Поворот ванильной камеры для курса и тангажа взгляда. */
    fun look(yaw: Float, pitch: Float): PhantomQuat =
        PhantomQuat.y(Math.PI - yaw * RAD) * PhantomQuat.x(-pitch * RAD)

    /**
     * Курс, тангаж и крен для `Camera.setRotation`, градусы.
     * Курс выбирается ближайший к [nearYaw]: угол камеры не прыгает на полный оборот.
     */
    fun angles(camera: PhantomQuat, nearYaw: Float): FloatArray {
        val euler = (PhantomQuat.y(-Math.PI) * camera).eulerYXZ()
        val yaw = (-euler[1] / RAD).toFloat()
        return floatArrayOf(nearYaw + wrapDegrees(yaw - nearYaw), (-euler[0] / RAD).toFloat(), (-euler[2] / RAD).toFloat())
    }

    /** Направление взгляда камеры в мире. */
    fun forward(camera: PhantomQuat): DoubleArray = camera.transform(0.0, 0.0, -1.0)

    private fun wrapDegrees(degrees: Float): Float {
        var d = degrees % 360f
        if (d >= 180f) d -= 360f
        if (d < -180f) d += 360f
        return d
    }

    private const val RAD = Math.PI / 180.0
}
