package xyz.nibblz.galapagos.screens.history

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import xyz.nibblz.galapagos.features.XPInfo
import xyz.nibblz.galapagos.screens.mcciScrollContainer
import xyz.nibblz.galapagos.ui.*
import xyz.nibblz.galapagos.util.mcciTextureComponent

abstract class BaseHistory : LayoutScreen() {
    open val game: XPInfo.XPSource = XPInfo.XPSource.BATTLE_BOX_QUADS
    abstract fun fillHistory(historyContainer: Flow)
    abstract fun fillOverview(overview: Flow)

    override fun extractScreenBackground(graphics: GuiGraphicsExtractor) {
        extractBlurredBackground(graphics)
        extractTransparentBackground(graphics)
    }

    override fun build(rootComponent: Flow) {
        rootComponent
            .horizontalAlignment(Alignment.CENTER)
            .verticalAlignment(Alignment.CENTER)

        val overview = Flow.vertical(Sizing.fill(30), Sizing.fixed(110))

        val historyContainer = Flow.vertical(Sizing.content(), Sizing.content())
        historyContainer.padding(Insets.all(5))
        historyContainer.gap(3)

        fillHistory(historyContainer)

        overview.child(
            Label(
                Component.empty()
                    .append(mcciTextureComponent(game.sprite))
                    .append(Component.literal(" ${game.label} Stats")
                ).withStyle(Style.EMPTY.withBold(true)))
                .shadow(true)
                .horizontalTextAlignment(Alignment.CENTER)
                .horizontalSizing(Sizing.fill())
                .margins(Insets.bottom(5))
        )

        fillOverview(overview)

        overview
            .surface(Surface.MCCI_PANEL)
            .padding(Insets.all(5))
            .margins(Insets.bottom(5))


        rootComponent.child(overview)

        rootComponent.child(mcciScrollContainer(Sizing.fill(30), Sizing.fill(60), historyContainer))
    }
}