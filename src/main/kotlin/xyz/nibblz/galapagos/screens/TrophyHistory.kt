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
import net.minecraft.util.ARGB
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.features.TrophyTracking
import xyz.nibblz.galapagos.ui.*
import xyz.nibblz.galapagos.util.Glyphs
import xyz.nibblz.galapagos.util.mcciTextureComponent
import java.util.*
import kotlin.time.Instant

class TrophyHistory : LayoutScreen() {
    fun updateContent(content: Flow) {
        val sortedTrophyGains = Galapagos.save.trophyHistory.sortedByDescending { it.timestamp }
        var previousDay = -1
        var dayPerCategory = EnumMap<TrophyTracking.TrophySource, Int>(TrophyTracking.TrophySource::class.java)

        var dayHeader: Label? = null
        var dayBreakdown: Flow? = null
        var typeBreakdown: Label? = null
        var categoryBreakdown: Label? = null

        sortedTrophyGains.forEach {
            if (it.trophies == 0) return@forEach

            val date = Instant.fromEpochSeconds(it.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())
            val newDayDate = if (Config.values::startDayAtQuestRefresh.get())
                Instant.fromEpochSeconds(it.timestamp + (60 * 60 * 10)).toLocalDateTime(TimeZone.UTC) else date

            if (previousDay != newDayDate.day) {
                dayPerCategory = TrophyTracking.TrophySource.entries.associateWithTo(EnumMap(TrophyTracking.TrophySource::class.java)) { 0 }

                dayHeader = Label(Component.empty())
                dayBreakdown = Flow.vertical(Sizing.fill(), Sizing.fixed(80)).gap(3)
                typeBreakdown = Label(Component.empty())
                categoryBreakdown = Label(Component.empty())

                if (Config.values::trophyTrackingShowTypeBreakdown.get()) {
                    dayBreakdown.child(typeBreakdown.horizontalSizing(Sizing.fill()))
                }

                if (Config.values::trophyTrackingShowTypeBreakdown.get() && Config.values::trophyTrackingShowCategoryBreakdown.get()) {
                    dayBreakdown.child(Box(Sizing.fill(), Sizing.fixed(1), ARGB.color(0.2f, 0x000000)))
                }

                if (Config.values::trophyTrackingShowCategoryBreakdown.get()) {
                    dayBreakdown.child(categoryBreakdown.horizontalSizing(Sizing.fill()))
                }

                content.child(Spacer(vertical = Sizing.fixed(10)))
                content.child(dayHeader)
                content.child(dayBreakdown)
            }

            previousDay = newDayDate.day

            dayPerCategory[it.source] = dayPerCategory[it.source]!! + it.trophies

            if (dayBreakdown != null && dayHeader != null) {
                dayHeader.text(Component.literal(
                    "${date.month.name.lowercase().replaceFirstChar { char -> char.uppercase() }} ${date.day}, ${date.year} [").withColor(TextColor.GRAY.value)
                    .append(Component.literal("+${"%,d".format(dayPerCategory.values.sum())} ").withColor(0x32ff32))
                    .append(Glyphs.getGlyphComponent("_fonts/icon/trophy/yellow.png"))
                    .append(Component.literal("]").withColor(TextColor.GRAY.value))
                )

                val totalSkill = dayPerCategory.entries.sumOf { (key, value) -> if (key.type == TrophyTracking.TrophyType.SKILL) value else 0 }
                val totalStyle = dayPerCategory.entries.sumOf { (key, value) -> if (key.type == TrophyTracking.TrophyType.STYLE) value else 0 }
                val totalAngler = dayPerCategory.entries.sumOf { (key, value) -> if (key.type == TrophyTracking.TrophyType.ANGLER) value else 0 }

                var typeBreakdownComponent = Component.empty()

                if (totalSkill > 0) {
                    typeBreakdownComponent = typeBreakdownComponent.append(
                        Component.literal("+$totalSkill ").withColor(TrophyTracking.TrophyType.SKILL.color)
                            .append(Glyphs.getGlyphComponent("_fonts/icon/trophy/red.png"))
                            .append(Component.literal(" "))
                    )
                }

                if (totalStyle > 0) {
                    typeBreakdownComponent = typeBreakdownComponent.append(
                        Component.literal("+$totalStyle ").withColor(TrophyTracking.TrophyType.STYLE.color)
                            .append(Glyphs.getGlyphComponent("_fonts/icon/trophy/purple.png"))
                            .append(Component.literal(" "))
                    )
                }

                if (totalAngler > 0) {
                    typeBreakdownComponent = typeBreakdownComponent.append(
                        Component.literal("+$totalAngler ").withColor(TrophyTracking.TrophyType.ANGLER.color)
                            .append(Glyphs.getGlyphComponent("_fonts/icon/trophy/blue.png"))
                            .append(Component.literal(" "))
                    )
                }

                typeBreakdown?.text(typeBreakdownComponent)

                var categoryBreakdownComponent = Component.empty()

                dayPerCategory.forEach category@{ (category, trophies) ->
                    if (trophies == 0) return@category
                    categoryBreakdownComponent = categoryBreakdownComponent.append(
                        Component.literal("+$trophies ").withColor(category.type.color)
                            .append(if (category.sprite.contains("_fonts")) {
                                Glyphs.getGlyphComponent(category.sprite + ".png")
                            } else {
                                mcciTextureComponent(category.sprite).withColor(0xffffff)
                            })
                            .append(Component.literal(" "))
                    )
                }

                categoryBreakdown?.text(categoryBreakdownComponent)

                dayBreakdown.verticalSizing(Sizing.content())
            }

            val changeContainer = Flow.horizontal(Sizing.fill(100), Sizing.content())
            changeContainer.gap(5)
            changeContainer.verticalAlignment(Alignment.CENTER)

            val sprite = Flow.vertical(Sizing.content(), Sizing.content())
                .child(Texture(
                    Identifier.fromNamespaceAndPath("mcc", "textures/${it.getIcon()}.png"),
                    0, 0, 16, 16, 16, 16
                ))

            var titleComponent = Component.literal(it.getLabel())

            if (it.type == TrophyTracking.TrophyType.SKILL) {
                sprite.child(Texture(
                    Identifier.fromNamespaceAndPath("mcc", "textures/island_interface/badges/border.png"),
                    0, 0, 16, 16, 16, 16
                ).positioning(Positioning.absolute(0, 0)))
            }

            if (it.source == TrophyTracking.TrophySource.CLAIM_FISH) {
                val starSprite = "_fonts/icon/fishing/${when (it.dataCount) {
                    1 -> "average_bubble"
                    2 -> "large_bubble"
                    3 -> "massive_bubble"
                    4 -> "gargantuan_bubble"
                    else -> "average_bubble"
                }}.png"

                sprite.child(Label(Glyphs.getGlyphComponent(starSprite))
                    .positioning(Positioning.absolute(8, 8))
                )

                val repeats = if (it.data.contains("Crab")) {
                    if (it.dataCount == 4) 3 else it.dataCount // erm
                } else it.dataCount

                titleComponent = titleComponent.append(
                    Component.literal(" ")
                )

                repeat(repeats) {
                    titleComponent = titleComponent.append(Glyphs.getGlyphComponent(starSprite))
                }
            }

            changeContainer.child(sprite)

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
                        Component.literal("+${it.trophies} ").withColor(it.type.color)
                            .append(Glyphs.getGlyphComponent("_fonts/icon/trophy/${it.type.sprite}.png"))
                    ))
                    .child(Label(titleComponent).horizontalSizing(Sizing.fill()))
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

        setupMcciScreen(rootComponent, content, "TROPHY HISTORY", "textures/_fonts/icon/trophy/yellow.png")
    }
}