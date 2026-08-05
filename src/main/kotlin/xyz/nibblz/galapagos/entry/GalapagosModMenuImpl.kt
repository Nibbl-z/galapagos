package xyz.nibblz.galapagos.entry

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.screens.Screen
import xyz.nibblz.galapagos.config.Config

// how it feels to steal from devcmb <3
class GalapagosModMenuImpl : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<Screen> {
        return ConfigScreenFactory(Config.Companion::getScreen)
    }
}