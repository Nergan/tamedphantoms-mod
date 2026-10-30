package com.tamedphantoms.mod.client

import com.tamedphantoms.mod.network.PhantomRosterPayload
import com.tamedphantoms.mod.platform.ModNetwork
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.neoforged.fml.ModContainer
import net.neoforged.neoforge.client.event.ScreenEvent
import net.neoforged.neoforge.client.gui.ConfigurationScreen
import java.util.Collections
import java.util.WeakHashMap

private val rosterScreens = Collections.newSetFromMap(WeakHashMap<Screen, Boolean>())

fun modConfigurationScreen(mod: ModContainer, parent: Screen): Screen {
    val screen = ConfigurationScreen(mod, parent)
    rosterScreens.add(screen)
    return screen
}

/** Кнопка на экране настроек мода: ConfigurationScreen в 26.3 финальный, поэтому кнопка вешается после init. */
fun onConfigScreenInit(event: ScreenEvent.Init.Post) {
    val screen = event.screen
    if (screen !in rosterScreens) return
    event.addListener(
        Button.builder(Component.translatable("tamedphantoms.configuration.roster")) { _ ->
            ModNetwork.sendToServer(PhantomRosterPayload)
        }.bounds(screen.width / 2 - 100, screen.height - 52, 200, 20).build(),
    )
}
