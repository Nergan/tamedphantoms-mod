package com.tamedphantoms.mod.client

import net.minecraft.client.gui.screens.Screen
import net.neoforged.fml.ModContainer
import net.neoforged.neoforge.client.gui.ConfigurationScreen

fun modConfigurationScreen(mod: ModContainer, parent: Screen): Screen = ConfigurationScreen(mod, parent)
