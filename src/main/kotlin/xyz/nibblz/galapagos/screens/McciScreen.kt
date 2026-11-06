package xyz.nibblz.galapagos.screens

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import xyz.nibblz.galapagos.ui.*

fun mcciScrollContainer(horizontal: Sizing, vertical: Sizing, content: Element) =
    ScrollContainer(horizontal, vertical, content)
        .scrollbarThickness(4)
        .scrollbarColor(0xFF1C2B46.toInt())
        .surface(Surface.MCCI_PANEL)

fun setupMcciScreen(rootComponent: Flow, content: Flow, title: String, icon: String) {
    val scrollContainer = mcciScrollContainer(Sizing.fixed(163), Sizing.fixed(195), content)
        .positioning(Positioning.relative(50, 50))

    rootComponent.child(scrollContainer)

    rootComponent.child(Flow.vertical()
        .child(
            Texture(
                Identifier.fromNamespaceAndPath("mcc", "textures/_fonts/chest_backgrounds/header/header.png"),
                0, 0, 176, 36, 176, 36))
        .child(
            Texture(
                Identifier.fromNamespaceAndPath("mcc", icon),
                0, 0, 16, 16, 16, 16)
                .positioning(Positioning.absolute(7, 7)))
        .child(
            Label(
                Component.literal(title).withStyle(Style.EMPTY.withFont(FontDescription.Resource(Identifier.fromNamespaceAndPath("mcc", "hud")))))
                .positioning(Positioning.absolute(37, 10))
        )

        .margins(Insets(top = 25, left = 2))
    )

    rootComponent.child(Flow.vertical(Sizing.fixed(1), Sizing.fixed(175)))

    rootComponent.child(Flow.vertical()
        .child(Texture(
            Identifier.fromNamespaceAndPath("mcc", "textures/_fonts/chest_backgrounds/footer/0.png"),
            0, 0, 176, 17, 176, 17))
        .margins(Insets(bottom = 40, left = 2)))
}
