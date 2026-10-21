package xyz.nibblz.galapagos.screens

import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.Identifier
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.ui.*
import kotlin.time.Instant

class QuestHistory : LayoutScreen() {
    fun updateContent(content: Flow) {
        val sortedQuests = Galapagos.save.questHistory.sortedByDescending { it.timestamp }
        var previousDay = -1

        sortedQuests.forEach {
            val date = Instant.fromEpochSeconds(it.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
            val newDayDate = if (Config.values::startDayAtQuestRefresh.get())
                Instant.fromEpochSeconds(it.timestamp + (60 * 60 * 10)).toLocalDateTime(TimeZone.UTC) else date

            if (previousDay != newDayDate.day) {
                content.child(Spacer(vertical = Sizing.fixed(10)))
                content.child(Label(Component.literal("${date.month.name.lowercase().replaceFirstChar { char -> char.uppercase() }} ${date.day}, ${date.year}").withColor(TextColor.GRAY.value)))
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
                    .child(Label(Component.literal(it.getLabel()).withColor(it.rarity.color)).horizontalSizing(Sizing.fill()))
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

        setupMcciScreen(rootComponent, content, "QUEST HISTORY", "textures/island_interface/quest_log/packages/icon.png")
    }
}