package xyz.nibblz.galapagos.screens

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.*
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.features.CoinTracking
import xyz.nibblz.galapagos.features.CoinTracking.getIcon
import xyz.nibblz.galapagos.features.CoinTracking.getSource
import xyz.nibblz.galapagos.util.Glyphs
import kotlin.time.Instant

class CoinHistory : BaseOwoScreen<FlowLayout>() {
    override fun createAdapter(): OwoUIAdapter<FlowLayout?> {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow)
    }

    fun updateContent(content: FlowLayout) {
        val sortedChanges = Galapagos.save.coinChanges.sortedByDescending { it.timestamp }
        var previousDay = -1
        var dayGain = 0
        var dayLoss = 0
        var dayHeader: LabelComponent? = null
        var dayLowerHeader: LabelComponent? = null

        sortedChanges.forEach {
            if (it.amount == 0) return@forEach

            if (CoinTracking.filter[it.category] == false) return@forEach

            val date = Instant.fromEpochSeconds(it.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
            if (previousDay != date.day) {
                dayGain = 0
                dayLoss = 0
                dayHeader = UIComponents.label(Component.empty())
                dayLowerHeader = UIComponents.label(Component.empty())

                content.child(UIComponents.spacer().verticalSizing(Sizing.fixed(10)))
                content.child(dayHeader)
                content.child(dayLowerHeader)
            }

            if (it.amount > 0) dayGain += it.amount
            if (it.amount < 0) dayLoss -= it.amount

            if (dayHeader != null && dayLowerHeader != null) {
                val total = dayGain - dayLoss

                dayHeader.text(Component.literal(
                    "${date.month.name.lowercase().replaceFirstChar { char -> char.uppercase() }} ${date.day}, ${date.year} [").withColor(ChatFormatting.GRAY.color!!)
                    .append(Component.literal("${if (total > 0) "+" else ""}${"%,d".format(total)} ").withColor(if (total > 0) 0x32ff32 else 0xff3232))
                    .append(Glyphs.getGlyphComponent("_fonts/icon/coin.png"))
                    .append(Component.literal("]").withColor(ChatFormatting.GRAY.color!!))
                )

                dayLowerHeader.text(Component.literal("+${"%,d".format(dayGain)} ").withColor(0x32ff32)
                    .append(Glyphs.getGlyphComponent("_fonts/icon/coin.png"))
                    .append(Component.literal(" -${"%,d".format(dayLoss)} ").withColor(0xff3232))
                    .append(Glyphs.getGlyphComponent("_fonts/icon/coin.png"))
                )
            }

            previousDay = date.day

            val changeContainer = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content())
            changeContainer.gap(5)
            changeContainer.verticalAlignment(VerticalAlignment.CENTER)

            changeContainer.child(
                UIComponents.texture(
                    Identifier.fromNamespaceAndPath("mcc", "textures/${it.getIcon()}"),
                    0, 0, 16, 16, 16, 16
                )
            )

            val time = if (!Config.values::twentyFourHourTime.get()) {
                date.time.format(LocalTime.Format {
                    amPmHour(Padding.NONE)
                    char(':')
                    minute()
                    char(':')
                    second()
                    char(' ')
                    amPmMarker("AM", "PM")
                })
            } else {
                date.time.toString()
            }

            changeContainer.child(
                UIContainers.verticalFlow(Sizing.fill(80), Sizing.content())
                    .child(UIComponents.label(
                        Component.literal("${if (it.amount > 0) "+" else ""}${"%,d".format(it.amount)} ")
                            .withColor(if (it.amount > 0) 0x32ff32 else 0xff3232)
                            .append(Glyphs.getGlyphComponent("_fonts/icon/coin.png"))
                    ))
                    .child(UIComponents.label(Component.literal(it.getSource())).horizontalSizing(Sizing.fill()))
                    .child(UIComponents.label(Component.literal(time).withColor(ChatFormatting.GRAY.color!!)))
            )

            changeContainer.padding(Insets.of(4))
            changeContainer.surface(Surface.VANILLA_TRANSLUCENT)

            content.child(changeContainer)
        }
        content.child(UIComponents.spacer().verticalSizing(Sizing.fixed(10)))
    }

    override fun build(rootComponent: FlowLayout) {
        rootComponent
            .surface(Surface.VANILLA_TRANSLUCENT)
            .horizontalAlignment(HorizontalAlignment.CENTER)
            .verticalAlignment(VerticalAlignment.CENTER)

        val content = UIContainers.verticalFlow(Sizing.content(), Sizing.content())
        content.padding(Insets.of(5))
        content.gap(3)

        updateContent(content)

        setupMcciScreen(rootComponent, content, "COIN HISTORY", "textures/island_items/currency/non_premium.png")

        val filter = UIComponents.dropdown(Sizing.fixed(50))
            .nested(Component.literal("Filter"), Sizing.content()) { submenu ->
                CoinTracking.CoinChangeCategory.entries.forEach {
                    if (it.hide) return@forEach
                    submenu.checkbox(Component.literal(it.label), CoinTracking.filter[it] ?: true) { toggle ->
                        CoinTracking.filter[it] = toggle
                        content.clearChildren()
                        updateContent(content)
                    }
                }
            }

            .closeWhenNotHovered(false)
            .positioning(Positioning.absolute(10, 10))

        rootComponent.child(filter)
    }
}