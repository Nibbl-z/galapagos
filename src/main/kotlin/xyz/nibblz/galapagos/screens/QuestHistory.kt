package xyz.nibblz.galapagos.screens

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.OwoUIAdapter
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.Surface
import io.wispforest.owo.ui.core.VerticalAlignment
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
import kotlin.time.Instant

class QuestHistory : BaseOwoScreen<FlowLayout>() {
    override fun createAdapter(): OwoUIAdapter<FlowLayout?> {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow)
    }

    fun updateContent(content: FlowLayout) {
        val sortedQuests = Galapagos.save.questHistory.sortedByDescending { it.timestamp }
        var previousDay = -1

        sortedQuests.forEach {
            val date = Instant.fromEpochSeconds(it.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())

            if (previousDay != date.day) {
                content.child(UIComponents.spacer().verticalSizing(Sizing.fixed(10)))
                content.child(UIComponents.label(Component.literal("${date.month.name.lowercase().replaceFirstChar { char -> char.uppercase() }} ${date.day}, ${date.year}").withColor(ChatFormatting.GRAY.color!!)))
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

            val time = if (true) { // todo make this a config thingy later
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
                    .child(UIComponents.label(Component.literal(it.getLabel()).withColor(it.rarity.color)).horizontalSizing(Sizing.fill()))
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

        setupMcciScreen(rootComponent, content, "QUEST HISTORY", "textures/island_interface/quest_log/packages/icon.png")
    }
}