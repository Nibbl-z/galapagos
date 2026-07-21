package xyz.nibblz.galapagos.screens

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.OwoUIAdapter
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.Surface
import io.wispforest.owo.ui.core.VerticalAlignment
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import xyz.nibblz.galapagos.core.OOBE
import java.net.URI

class Intro : BaseOwoScreen<FlowLayout>() {
    override fun createAdapter(): OwoUIAdapter<FlowLayout?> {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow)
    }

    override fun shouldCloseOnEsc(): Boolean {
        return false
    }

    override fun build(rootComponent: FlowLayout) {
        rootComponent
            .surface(Surface.VANILLA_TRANSLUCENT)
            .horizontalAlignment(HorizontalAlignment.CENTER)
            .verticalAlignment(VerticalAlignment.CENTER)

        rootComponent.child(UIContainers.verticalFlow(Sizing.fill(60), Sizing.content())
            .child(UIComponents.label(Component.literal(
                "Thank you for installing Galapagos!!! Before you can start using the features of this mod, let's take a moment to confirm if your API settings are correctly configured!")
            ).horizontalTextAlignment(HorizontalAlignment.CENTER).maxWidth(300))

            .child(UIComponents.label(Component.literal(
                "First off: You'll need an API key! If you do not have one, you can generate one at ")
                .append(Component.literal("https://gateway.noxcrew.com/").withStyle(Style.EMPTY
                    .withColor(ChatFormatting.AQUA.color!!)
                    .withUnderlined(true)
                    .withClickEvent(ClickEvent.OpenUrl(URI("https://gateway.noxcrew.com/")))))
                .append(Component.literal(". If you can't generate an API key, that's okay! A custom endpoint will be used instead, however, uptime of this endpoint is not guaranteed!"))
            ).horizontalTextAlignment(HorizontalAlignment.CENTER).maxWidth(300))

            .child(UIContainers.horizontalFlow(Sizing.content(), Sizing.content())
                .child(UIComponents.button(Component.literal("I have an API key")) {
                    OOBE.hasAPIKey = true
                    super.onClose()
                })
                .child(UIComponents.button(Component.literal("I don't have an API key")) {
                    OOBE.hasAPIKey = false
                    super.onClose()
                })
                .gap(5)
            )

            .gap(15)
            .horizontalAlignment(HorizontalAlignment.CENTER)
            .verticalAlignment(VerticalAlignment.CENTER)
        )
    }
}