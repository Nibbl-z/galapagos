package xyz.nibblz.galapagos.screens

import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Checkbox
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.Identifier
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.features.CoinTracking
import xyz.nibblz.galapagos.features.CoinTracking.getIcon
import xyz.nibblz.galapagos.features.CoinTracking.getSource
import xyz.nibblz.galapagos.ui.*
import xyz.nibblz.galapagos.util.Glyphs
import kotlin.time.Instant

class CoinHistory : LayoutScreen() {
    fun updateContent(content: Flow) {
        val sortedChanges = Galapagos.save.coinChanges.sortedByDescending { it.timestamp }
        var previousDay = -1
        var dayGain = 0
        var dayLoss = 0
        var dayHeader: Label? = null
        var dayLowerHeader: Label? = null

        sortedChanges.forEach {
            if (it.amount == 0) return@forEach

            if (CoinTracking.filter[it.category] == false) return@forEach

            val date = Instant.fromEpochSeconds(it.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
            val newDayDate = if (Config.values::startDayAtQuestRefresh.get())
                Instant.fromEpochSeconds(it.timestamp - (60 * 60 * 10)).toLocalDateTime(TimeZone.UTC) else date

            if (previousDay != newDayDate.day) {
                dayGain = 0
                dayLoss = 0
                dayHeader = Label(Component.empty())
                dayLowerHeader = Label(Component.empty())

                content.child(Spacer(vertical = Sizing.fixed(10)))
                content.child(dayHeader)
                content.child(dayLowerHeader)
            }

            if (it.amount > 0) dayGain += it.amount
            if (it.amount < 0) dayLoss -= it.amount

            if (dayHeader != null && dayLowerHeader != null) {
                val total = dayGain - dayLoss

                dayHeader.text(Component.literal(
                    "${date.month.name.lowercase().replaceFirstChar { char -> char.uppercase() }} ${date.day}, ${date.year} [").withColor(TextColor.GRAY.value)
                    .append(Component.literal("${if (total > 0) "+" else ""}${"%,d".format(total)} ").withColor(if (total > 0) 0x32ff32 else 0xff3232))
                    .append(Glyphs.getGlyphComponent("_fonts/icon/coin.png"))
                    .append(Component.literal("]").withColor(TextColor.GRAY.value))
                )

                dayLowerHeader.text(Component.literal("+${"%,d".format(dayGain)} ").withColor(0x32ff32)
                    .append(Glyphs.getGlyphComponent("_fonts/icon/coin.png"))
                    .append(Component.literal(" -${"%,d".format(dayLoss)} ").withColor(0xff3232))
                    .append(Glyphs.getGlyphComponent("_fonts/icon/coin.png"))
                )
            }

            previousDay = newDayDate.day

            val changeContainer = Flow.horizontal(Sizing.fill(100), Sizing.content())
            changeContainer.gap(5)
            changeContainer.verticalAlignment(Alignment.CENTER)

            changeContainer.child(
                Texture(
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
                Flow.vertical(Sizing.fill(80), Sizing.content())
                    .child(Label(
                        Component.literal("${if (it.amount > 0) "+" else ""}${"%,d".format(it.amount)} ")
                            .withColor(if (it.amount > 0) 0x32ff32 else 0xff3232)
                            .append(Glyphs.getGlyphComponent("_fonts/icon/coin.png"))
                    ))
                    .child(Label(Component.literal(it.getSource())).horizontalSizing(Sizing.fill()))
                    .child(Label(Component.literal(time).withColor(TextColor.GRAY.value)))
            )

            changeContainer.padding(Insets.all(4))
            changeContainer.surface(Surface.VANILLA_TRANSLUCENT)

            content.child(changeContainer)
        }
        content.child(Spacer(vertical = Sizing.fixed(10)))
    }

    override fun build(rootComponent: Flow) {
        rootComponent
            .horizontalAlignment(Alignment.CENTER)
            .verticalAlignment(Alignment.CENTER)

        val content = Flow.vertical(Sizing.content(), Sizing.content())
        content.padding(Insets.all(5))
        content.gap(3)

        updateContent(content)

        setupMcciScreen(rootComponent, content, "COIN HISTORY", "textures/island_items/currency/non_premium.png")

        val filterPanel = Flow.vertical()
            .padding(Insets.all(4))
            .gap(2)
            .surface(Surface.VANILLA_TRANSLUCENT)
            .positioning(Positioning.absolute(10, 32))
            .apply { visible = false }

        CoinTracking.CoinChangeCategory.entries.filter { !it.hide }.forEach {
            filterPanel.child(WidgetElement(
                Checkbox.builder(Component.literal(it.label), font)
                    .selected(CoinTracking.filter[it] ?: true)
                    .onValueChange { _, toggle ->
                        CoinTracking.filter[it] = toggle
                        content.clearChildren()
                        updateContent(content)
                        layout()
                    }
                    .build()
            ))
        }

        val filterButton = Button.builder(Component.literal("Filter")) {
            filterPanel.visible = !filterPanel.visible
        }.width(50).build()

        rootComponent.child(WidgetElement(filterButton).positioning(Positioning.absolute(10, 10)))
        rootComponent.child(filterPanel)
    }
}