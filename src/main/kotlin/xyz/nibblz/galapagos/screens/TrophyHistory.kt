package xyz.nibblz.galapagos.screens

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.HorizontalAlignment
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.OwoUIAdapter
import io.wispforest.owo.ui.core.Positioning
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
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.features.TrophyTracking
import xyz.nibblz.galapagos.util.Glyphs
import java.util.EnumMap
import kotlin.time.Instant

class TrophyHistory : BaseOwoScreen<FlowLayout>() {
    override fun createAdapter(): OwoUIAdapter<FlowLayout?> {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow)
    }

    fun updateContent(content: FlowLayout) {
        val sortedTrophyGains = Galapagos.save.trophyHistory.sortedByDescending { it.timestamp }
        var previousDay = -1
        var dayPerCategory = EnumMap<TrophyTracking.TrophySource, Int>(TrophyTracking.TrophySource::class.java)

        var dayHeader: LabelComponent? = null
        var dayBreakdown: FlowLayout? = null
        var typeBreakdown: LabelComponent? = null
        var categoryBreakdown: LabelComponent? = null

        sortedTrophyGains.forEach {
            val date = Instant.fromEpochSeconds(it.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())

            if (previousDay != date.day) {
                dayPerCategory = TrophyTracking.TrophySource.entries.associateWithTo(EnumMap(TrophyTracking.TrophySource::class.java)) { 0 }

                dayHeader = UIComponents.label(Component.empty())
                dayBreakdown = UIContainers.verticalFlow(Sizing.fill(), Sizing.fixed(80)).gap(3)
                typeBreakdown = UIComponents.label(Component.empty())
                categoryBreakdown = UIComponents.label(Component.empty())

                dayBreakdown.child(typeBreakdown.horizontalSizing(Sizing.fill()))
                dayBreakdown.child(UIComponents.box(Sizing.fill(), Sizing.fixed(1))
                    .color(Color(0.0f, 0.0f, 0.0f, 0.2f)))
                dayBreakdown.child(categoryBreakdown.horizontalSizing(Sizing.fill()))

                content.child(UIComponents.spacer().verticalSizing(Sizing.fixed(10)))
                content.child(dayHeader)
                content.child(dayBreakdown)
            }

            previousDay = date.day

            dayPerCategory[it.source] = dayPerCategory[it.source]!! + it.trophies

            if (dayBreakdown != null && dayHeader != null) {
                dayHeader.text(Component.literal(
                    "${date.month.name.lowercase().replaceFirstChar { char -> char.uppercase() }} ${date.day}, ${date.year} [").withColor(ChatFormatting.GRAY.color!!)
                    .append(Component.literal("+${"%,d".format(dayPerCategory.values.sum())} ").withColor(0x32ff32))
                    .append(Glyphs.getGlyphComponent("_fonts/icon/trophy/yellow.png"))
                    .append(Component.literal("]").withColor(ChatFormatting.GRAY.color!!))
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
                            .append(if (category == TrophyTracking.TrophySource.CLAIM_RESEARCH) {
                                // this sucks
                                Component.literal("\uE007").withColor(0xffffff).withStyle(Style.EMPTY.withFont(Galapagos.font))
                            } else {
                                Glyphs.getGlyphComponent(category.sprite + ".png")
                            })
                            .append(Component.literal(" "))
                    )
                }

                categoryBreakdown?.text(categoryBreakdownComponent)

                dayBreakdown.verticalSizing(Sizing.content())
            }

            val changeContainer = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content())
            changeContainer.gap(5)
            changeContainer.verticalAlignment(VerticalAlignment.CENTER)

            val sprite = UIContainers.verticalFlow(Sizing.content(), Sizing.content())
                .child(UIComponents.texture(
                    Identifier.fromNamespaceAndPath("mcc", "textures/${it.getIcon()}.png"),
                    0, 0, 16, 16, 16, 16
                ))

            var titleComponent = Component.literal(it.getLabel())

            if (it.type == TrophyTracking.TrophyType.SKILL) {
                sprite.child(UIComponents.texture(
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

                sprite.child(UIComponents.label(Glyphs.getGlyphComponent(starSprite))
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
                UIContainers.verticalFlow(Sizing.fill(80), Sizing.content())
                    .child(UIComponents.label(
                        Component.literal("+${it.trophies} ").withColor(it.type.color)
                            .append(Glyphs.getGlyphComponent("_fonts/icon/trophy/${it.type.sprite}.png"))
                    ))
                    .child(UIComponents.label(titleComponent).horizontalSizing(Sizing.fill()))
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

        setupMcciScreen(rootComponent, content, "TROPHY HISTORY", "textures/_fonts/icon/trophy/yellow.png")
    }
}