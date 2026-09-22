package com.tamedphantoms.mod.client

import com.electronwill.nightconfig.core.UnmodifiableConfig
import com.tamedphantoms.mod.config.ServerConfig
import com.tamedphantoms.mod.util.PhantomFlightPace
import net.minecraft.client.gui.components.AbstractSliderButton
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.data.models.blockstates.PropertyDispatch.QuadFunction
import net.minecraft.network.chat.Component
import net.neoforged.fml.ModContainer
import net.neoforged.fml.config.ModConfig
import net.neoforged.neoforge.client.gui.ConfigurationScreen
import net.neoforged.neoforge.common.ModConfigSpec
import java.util.Locale
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.math.abs
import kotlin.math.roundToInt

/** Экран настроек: разделы NeoForge, но скорость своих фантомов — ползунок с серверным потолком. */
fun modConfigurationScreen(mod: ModContainer, parent: Screen): Screen = ConfigurationScreen(
    mod,
    parent,
    QuadFunction { parentScreen, type, modConfig, title ->
        FlightAwareSectionScreen(parentScreen, type, modConfig, title)
    },
)

/**
 * Раздел конфига. Вложенные разделы открывает тоже собой, чтобы ползунок
 * скорости доехал до страницы «Полёт».
 */
class FlightAwareSectionScreen : ConfigurationScreen.ConfigurationSectionScreen {

    constructor(parent: Screen, type: ModConfig.Type, modConfig: ModConfig, title: Component) :
        super(parent, type, modConfig, title)

    constructor(
        parentContext: Context,
        parent: Screen,
        valueSpecs: Map<String, Any>,
        key: String,
        entrySet: Set<UnmodifiableConfig.Entry>,
        title: Component,
    ) : super(parentContext, parent, valueSpecs, key, entrySet, title)

    @Suppress("DEPRECATION")
    override fun createSection(
        key: String,
        subconfig: UnmodifiableConfig,
        subsection: UnmodifiableConfig,
    ): Element? {
        if (subconfig.isEmpty) return null
        val label = getTranslationComponent(key).append("...")
        val tooltip = getTooltipComponent(key, null)
        val button = Button.builder(label) {
            minecraft?.setScreen(
                sectionCache.computeIfAbsent(key) {
                    FlightAwareSectionScreen(
                        context,
                        this,
                        subconfig.valueMap(),
                        key,
                        subsection.entrySet(),
                        getTranslationComponent(key),
                    ).rebuild()
                },
            )
        }.tooltip(Tooltip.create(tooltip)).width(Button.DEFAULT_WIDTH).build()
        return Element(label, tooltip, button, false)
    }

    override fun createDoubleValue(
        key: String,
        spec: ModConfigSpec.ValueSpec,
        source: Supplier<Double>,
        target: Consumer<Double>,
    ): Element? {
        if (key != OWNED_FLIGHT_SPEED) {
            return super.createDoubleValue(key, spec, source, target)
        }
        val cap = serverFlightCap()
        val min = PhantomFlightPace.MIN
        val stored = source.get()
        val shown = stored.coerceIn(min, cap)
        if (abs(stored - shown) > 0.0001) {
            target.accept(shown)
            onChanged(key)
        }
        val tooltip = Component.translatable(TOOLTIP, formatSpeed(cap))
        val slider = FlightSpeedSlider(min, cap, shown) { newValue ->
            val oldValue = source.get()
            if (abs(newValue - oldValue) <= 0.0001) return@FlightSpeedSlider
            undoManager.add(
                { value: Double ->
                    target.accept(value)
                    onChanged(key)
                },
                newValue,
                { value: Double ->
                    target.accept(value)
                    onChanged(key)
                },
                oldValue,
            )
        }
        slider.tooltip = Tooltip.create(tooltip)
        return Element(getTranslationComponent(key), tooltip, slider)
    }

    private class FlightSpeedSlider(
        private val min: Double,
        private val max: Double,
        initial: Double,
        private val onCommit: (Double) -> Unit,
    ) : AbstractSliderButton(
        0,
        0,
        Button.DEFAULT_WIDTH,
        Button.DEFAULT_HEIGHT,
        Component.empty(),
        fraction(initial, min, max),
    ) {
        private var dragging = false

        init {
            updateMessage()
        }

        override fun updateMessage() {
            message = Component.translatable(VALUE, formatSpeed(speed()), formatSpeed(max))
        }

        override fun applyValue() {
            if (!dragging) onCommit(speed())
        }

        override fun onClick(mouseX: Double, mouseY: Double) {
            dragging = true
            super.onClick(mouseX, mouseY)
        }

        override fun onRelease(mouseX: Double, mouseY: Double) {
            super.onRelease(mouseX, mouseY)
            dragging = false
            onCommit(speed())
        }

        private fun speed(): Double {
            if (max <= min) return min
            if (value >= 0.9995) return max
            val raw = min + value * (max - min)
            val snapped = (raw * 100.0).roundToInt() / 100.0
            return snapped.coerceIn(min, max)
        }
    }

    companion object {
        private const val OWNED_FLIGHT_SPEED = "owned_flight_speed"
        private const val TOOLTIP = "tamedphantoms.configuration.flight.owned_flight_speed.tooltip"
        private const val VALUE = "tamedphantoms.configuration.flight.owned_flight_speed.value"

        fun serverFlightCap(): Double {
            if (!ServerConfig.SPEC.isLoaded) return 1.25
            return ServerConfig.CONFIG.flightSpeed.get().coerceIn(PhantomFlightPace.MIN, PhantomFlightPace.MAX)
        }

        fun formatSpeed(value: Double): String {
            val text = String.format(Locale.ROOT, "%.2f", value)
            return text.trimEnd('0').trimEnd('.')
        }

        private fun fraction(speed: Double, min: Double, max: Double): Double {
            if (max <= min) return 0.0
            return ((speed - min) / (max - min)).coerceIn(0.0, 1.0)
        }
    }
}
