package xyz.nibblz.galapagos.util

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier

val galapagosIconComponent: Component =
    Component.literal("<").withColor(ChatFormatting.GREEN.color!!)
        .append(Component.literal("\uA000").withStyle(Style.EMPTY
            .withFont(FontDescription.Resource(Identifier.fromNamespaceAndPath("galapagos", "main")))
            .withColor(0xFFFFFF)))
        .append(Component.literal("> ").withColor(ChatFormatting.GREEN.color!!))

fun sendChatMessage(message: Component) {
    if (Minecraft.getInstance().isSameThread) {
        Minecraft.getInstance().gui.chat.addClientSystemMessage(message)
    } else {
        Minecraft.getInstance().execute {
            Minecraft.getInstance().gui.chat.addClientSystemMessage(message)
        }
    }
}

fun sendGalapagosChatMessage(message: Component) {
    sendChatMessage(galapagosIconComponent.copy().append(message))
}