package xyz.nibblz.galapagos.features

import net.minecraft.network.chat.Component
import xyz.nibblz.galapagos.config.Config

interface Feature {
    val id: String
    val name: String
    val description: List<Component>
    var enabled: Boolean
        get() = Config.handler.instance().features.getOrDefault(id, true)
        set(value) {
            Config.handler.instance().features[id] = value
        }
    val image: Config.ConfigImage

    fun init()
}