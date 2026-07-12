package xyz.nibblz.galapagos.features

import net.minecraft.network.chat.Component
import xyz.nibblz.galapagos.config.Config
import kotlin.reflect.KMutableProperty0

interface Feature {
    val id: String
    val name: String
    val description: List<Component>
    val enabledProperty: KMutableProperty0<Boolean>
    val image: Config.ConfigImage

    fun init()
}