package xyz.nibblz.galapagos.screens.history

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.ScrollContainer
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.OwoUIAdapter
import io.wispforest.owo.ui.core.OwoUIGraphics
import io.wispforest.owo.ui.core.ParentUIComponent
import io.wispforest.owo.ui.core.Positioning
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.Surface
import io.wispforest.owo.ui.core.VerticalAlignment
import io.wispforest.owo.ui.util.NinePatchTexture
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import xyz.nibblz.galapagos.features.XPInfo
import xyz.nibblz.galapagos.util.mcciTextureComponent

abstract class BaseHistory : BaseOwoScreen<FlowLayout>() {
    override fun createAdapter(): OwoUIAdapter<FlowLayout?> {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow)
    }

    open val game: XPInfo.XPSource = XPInfo.XPSource.BATTLE_BOX_QUADS
    abstract fun fillHistory(historyContainer: FlowLayout)
    abstract fun fillOverview(overview: FlowLayout)

    override fun build(rootComponent: FlowLayout) {
        rootComponent
            .surface(Surface.blur(10f, 5f))
            .horizontalAlignment(HorizontalAlignment.CENTER)
            .verticalAlignment(VerticalAlignment.CENTER)

        rootComponent.child(
            UIContainers.verticalFlow(Sizing.fill(), Sizing.fill())
                .surface(Surface.VANILLA_TRANSLUCENT)
                .positioning(Positioning.absolute(0, 0))
        )

        val overview = UIContainers.verticalFlow(Sizing.fill(30), Sizing.fixed(110))

        val historyContainer = UIContainers.verticalFlow(Sizing.content(), Sizing.content())
        historyContainer.padding(Insets.of(5))
        historyContainer.gap(3)

        fillHistory(historyContainer)

        overview.child(
            UIComponents.label(
                Component.empty()
                    .append(mcciTextureComponent(game.sprite))
                    .append(Component.literal(" ${game.label} Stats")
                ).withStyle(Style.EMPTY.withBold(true)))
                .shadow(true)
                .horizontalTextAlignment(HorizontalAlignment.CENTER)
                .horizontalSizing(Sizing.fill())
                .margins(Insets.bottom(5))
        )

        fillOverview(overview)

        overview
            .surface { context: OwoUIGraphics?, component: ParentUIComponent? ->
                NinePatchTexture.draw(Identifier.fromNamespaceAndPath("galapagos", "mcci_panel"), context, component)
            }
            .padding(Insets.of(5))
            .margins(Insets.bottom(5))


        rootComponent.child(overview)

        rootComponent.child(
            UIContainers.verticalScroll(Sizing.fill(30), Sizing.fill(60),historyContainer)
                .scrollbarThiccness(4) // are we fr
                .scrollbar(ScrollContainer.Scrollbar.flat(Color.ofRgb(0x1c2b46)))
                .surface { context: OwoUIGraphics?, component: ParentUIComponent? ->
                    NinePatchTexture.draw(Identifier.fromNamespaceAndPath("galapagos", "mcci_panel"), context, component)
                }
        )
    }
}