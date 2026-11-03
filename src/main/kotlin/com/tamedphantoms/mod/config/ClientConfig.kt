package com.tamedphantoms.mod.config

/** То, что слышит и выбирает один игрок. Файл: `config/tamedphantoms-client.toml`. */
object ClientConfig {

    interface Settings {
        fun tamedSoundVolume(): Double
        fun ownedFlightSpeed(): Double
        fun cameraHorizonHold(): Int = DEFAULT_HORIZON_HOLD
        fun cameraSmoothing(): Int = DEFAULT_CAMERA_SMOOTHING
    }

    const val DEFAULT_HORIZON_HOLD = 30
    const val MAX_HORIZON_HOLD = 90
    const val DEFAULT_CAMERA_SMOOTHING = 25
    const val MAX_CAMERA_SMOOTHING = 100

    /** Самое сильное сглаживание камеры, секунды. */
    private const val SMOOTHING_SECONDS = 0.4

    var settings: Settings = Defaults

    fun tamedSoundVolume(): Double = settings.tamedSoundVolume().coerceIn(0.0, 1.0)
    fun ownedFlightSpeed(): Double = settings.ownedFlightSpeed()

    /** До какого наклона фантома взгляд в первом лице держит горизонт, градусы. */
    fun cameraHorizonHold(): Int = settings.cameraHorizonHold().coerceIn(0, MAX_HORIZON_HOLD)

    fun cameraSmoothing(): Int = settings.cameraSmoothing().coerceIn(0, MAX_CAMERA_SMOOTHING)

    fun cameraSmoothingSeconds(): Double = cameraSmoothing() / MAX_CAMERA_SMOOTHING.toDouble() * SMOOTHING_SECONDS

    private object Defaults : Settings {
        override fun tamedSoundVolume() = 0.5
        override fun ownedFlightSpeed() = 1.25
    }
}
