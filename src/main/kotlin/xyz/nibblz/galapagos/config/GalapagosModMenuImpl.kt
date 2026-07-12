package xyz.nibblz.galapagos.config

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.screens.Screen

// how it feels to steal from devcmb <3
class GalapagosModMenuImpl : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<Screen> {
        return ConfigScreenFactory(Config.Companion::getScreen)
    }
}