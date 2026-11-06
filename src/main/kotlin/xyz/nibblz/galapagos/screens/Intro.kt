package xyz.nibblz.galapagos.screens

import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import xyz.nibblz.galapagos.core.OOBE
import xyz.nibblz.galapagos.ui.*
import java.net.URI

class Intro : LayoutScreen() {
    override fun shouldCloseOnEsc(): Boolean {
        return false
    }

    private fun button(text: Component, onPress: () -> Unit) =
        WidgetElement(Button.builder(text) { onPress() }.width(font.width(text) + 20).build())

    override fun build(rootComponent: Flow) {
        rootComponent
            .horizontalAlignment(Alignment.CENTER)
            .verticalAlignment(Alignment.CENTER)

        rootComponent.child(Flow.vertical(Sizing.fill(60), Sizing.content())
            .child(Label(Component.literal(
                "Thank you for installing Galapagos!!! Before you can start using the features of this mod, let's take a moment to confirm if your API settings are correctly configured!")
            ).horizontalTextAlignment(Alignment.CENTER).maxWidth(300))

            .child(Label(Component.literal(
                "First off: You'll need an API key! If you do not have one, you can generate one at ")
                .append(Component.literal("https://gateway.noxcrew.com/").withStyle(Style.EMPTY
                    .withColor(TextColor.AQUA.value)
                    .withUnderlined(true)
                    .withClickEvent(ClickEvent.OpenUrl(URI("https://gateway.noxcrew.com/")))))
                .append(Component.literal(". If you can't generate an API key, that's okay! A custom endpoint will be used instead, however, uptime of this endpoint is not guaranteed!"))
            ).horizontalTextAlignment(Alignment.CENTER).maxWidth(300))

            .child(Flow.horizontal(Sizing.content(), Sizing.content())
                .child(button(Component.literal("I have an API key")) {
                    OOBE.hasAPIKey = true
                    super.onClose()
                })
                .child(button(Component.literal("I don't have an API key")) {
                    OOBE.hasAPIKey = false
                    super.onClose()
                })
                .gap(5)
            )

            .gap(15)
            .horizontalAlignment(Alignment.CENTER)
            .verticalAlignment(Alignment.CENTER)
        )
    }
}