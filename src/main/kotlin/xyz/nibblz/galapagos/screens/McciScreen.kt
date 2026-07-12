package xyz.nibblz.galapagos.screens

import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.ScrollContainer
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.*
import io.wispforest.owo.ui.util.NinePatchTexture
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier

fun setupMcciScreen(rootComponent: FlowLayout, content: FlowLayout, title: String, icon: String) {
    val scrollContainer = UIContainers.verticalScroll(Sizing.fixed(163), Sizing.fixed(195), content)
        .scrollbarThiccness(4) // are we fr
        .scrollbar(ScrollContainer.Scrollbar.flat(Color.ofRgb(0x1c2b46)))
        .surface { context: OwoUIGraphics?, component: ParentUIComponent? ->
            NinePatchTexture.draw(Identifier.fromNamespaceAndPath("galapagos", "mcci_panel"), context, component)
        }
        .positioning(Positioning.relative(50, 50))

    rootComponent.child(scrollContainer)

    rootComponent.child(UIContainers.verticalFlow(Sizing.content(), Sizing.content())
        .child(
            UIComponents.texture(
                Identifier.fromNamespaceAndPath("mcc", "textures/_fonts/chest_backgrounds/header/header.png"),
                0, 0, 176, 36, 176, 36))
        .child(
            UIComponents.texture(
                Identifier.fromNamespaceAndPath("mcc", icon),
                0, 0, 16, 16, 16, 16)
                .positioning(Positioning.absolute(7, 7)))
        .child(
            UIComponents.label(
                Component.literal(title).withStyle(Style.EMPTY.withFont(FontDescription.Resource(Identifier.fromNamespaceAndPath("mcc", "hud")))))
                .positioning(Positioning.absolute(37, 10))
        )

        .margins(Insets.of(25, 0, 2, 0))
    )

    rootComponent.child(UIContainers.verticalFlow(Sizing.fixed(1 ), Sizing.fixed(175)))

    rootComponent.child(UIContainers.verticalFlow(Sizing.content(), Sizing.content())
        .child(UIComponents.texture(
            Identifier.fromNamespaceAndPath("mcc", "textures/_fonts/chest_backgrounds/footer/0.png"),
            0, 0, 176, 17, 176, 17))
        .margins(Insets.of(0, 40, 2, 0)))
}