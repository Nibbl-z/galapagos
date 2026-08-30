package xyz.nibblz.galapagos.screens.history

import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.Insets
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
import xyz.nibblz.galapagos.data.game.DeathCause
import xyz.nibblz.galapagos.features.XPInfo
import xyz.nibblz.galapagos.util.Glyphs
import xyz.nibblz.galapagos.util.percentageToColor
import xyz.nibblz.galapagos.util.secondsToTimeString
import kotlin.time.Instant

class SkyBattleSoloHistory : BaseHistory() {
    override val game = XPInfo.XPSource.SKY_BATTLE_SOLOS

    data class DataPoint (
        val placement: Int,
        var eliminations: Int,
        val timeSurvived: Int
    )

    val mapData: HashMap<String, MutableList<DataPoint>> = hashMapOf()
    val kills: HashMap<DeathCause, Int> = hashMapOf()
    val deaths: HashMap<DeathCause, Int> = hashMapOf()

    override fun fillHistory(historyContainer: FlowLayout) {
        Galapagos.save.skyBattleSoloHistory.sortedByDescending { it.timestamp }.forEach {
            val dataPoint = DataPoint(
                it.getPlacement(),
                it.getPlayer()?.kills ?: 0,
                it.timeSurvived
            )

            mapData.putIfAbsent(it.map, mutableListOf())
            mapData[it.map]?.add(dataPoint)

            val gameKills: HashMap<DeathCause, Int> = hashMapOf()

            it.killCauses.forEach { cause ->
                kills[cause] = (kills[cause] ?: 0) + 1
                gameKills[cause] = (gameKills[cause] ?: 0) + 1
            }
            if (it.deathCause != DeathCause.UNKNOWN) deaths[it.deathCause] = (deaths[it.deathCause] ?: 0) + 1

            val container = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content())
            container.gap(5)
            container.verticalAlignment(VerticalAlignment.CENTER)

            container.child(UIComponents.texture(Identifier.fromNamespaceAndPath(Galapagos.MOD_ID, "textures/sky_battle_solos/${when(it.map) {
                "Air Port" -> "air_port"
                "Candy Land Solos" -> "candy_land_solos"
                "Grasslands Solos" -> "grasslands_solos"
                "Roundabout" -> "roundabout"
                "Sandcastle Solos" -> "sandcastle_solos"
                "Dirt House" -> "dirt_house"
                "Bastion" -> "bastion"
                else -> "roundabout"
            }}.png"),
                0, 0, 200, 60, 200, 60
            ).blend(true).positioning(Positioning.absolute(0,0)))

            val leftContent = UIContainers.verticalFlow(Sizing.fill(35), Sizing.content())
            leftContent.padding(Insets.of(5))
            container.child(leftContent)

            leftContent.child(UIComponents.label(Component.literal(it.map))
                .shadow(true)
            )

            leftContent.child(UIComponents.label(
                Component.literal("Placed ")
                    .append(when(it.getPlacement()) {
                        1 -> Component.literal("1st").withColor(ChatFormatting.YELLOW.color!!).withStyle(Style.EMPTY.withBold(true))
                        2 -> Component.literal("2nd").withColor(ChatFormatting.GRAY.color!!)
                        3 -> Component.literal("3rd").withColor(0x9e5b39)
                        else -> Component.literal("${it.getPlacement()}th").withColor(ChatFormatting.DARK_GRAY.color!!)
                    })
                ).shadow(true)
            )

            leftContent.child(UIComponents.label(
                Component.literal("Survived ${secondsToTimeString(it.timeSurvived)}")
            ))

            val date = Instant.fromEpochSeconds(it.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())

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

            leftContent.child(UIComponents.label(Component.literal(
                "${date.month.name.lowercase().replaceFirstChar { char -> char.uppercase() }} ${date.day}, ${date.year}\n$time"
            ).withColor(ChatFormatting.GRAY.color!!)).margins(Insets.top(10)))

            val rightContent = UIContainers.verticalFlow(Sizing.fill(65), Sizing.content())
            rightContent.padding(Insets.of(5))
            container.child(rightContent)

            val playerStats = it.getPlayer()

            rightContent.child(
                UIComponents.label(
                    with(Component.empty()) {
                        append(Component.literal("${playerStats?.kills} "))
                        append(Glyphs.getGlyphComponent("_fonts/icon/kills.png"))

                        if (playerStats?.kills != 0) {
                            append(Component.literal(" (").withColor(ChatFormatting.DARK_GRAY.color!!))

                            var i = 1
                            gameKills.forEach { (cause, count) ->
                                append(cause.createIconAndCountComponent(true, count))
                                if (i < gameKills.size) append(Component.literal(" "))
                                i++
                            }

                            append(Component.literal(")").withColor(ChatFormatting.DARK_GRAY.color!!))
                        }

                        append(Component.empty()) // ughhghghgh
                    }
                )
            )

            if (it.deathCause != DeathCause.UNKNOWN) {
                rightContent.child(
                    UIComponents.label(Component.literal("Died from ").append(it.deathCause.createIconAndLabelComponent()))
                )
            }

            container.surface(Surface.VANILLA_TRANSLUCENT)

            historyContainer.child(container)
        }
    }

    override fun fillOverview(overview: FlowLayout) {
        val mapWinRates: HashMap<String, Double> = hashMapOf()
        val mapsByWinRate = mapData.keys.sortedBy {
            val data = mapData[it]!!
            val wins = data.sumOf { data -> if (data.placement == 1) 1 else 0 }
            val losses = data.sumOf { data -> if (data.placement != 1) 1 else 0 }
            val winRate = wins.toDouble() / (wins + losses).toDouble()
            if (winRate.isInfinite()) { return@sortedBy -1.0 }

            mapWinRates[it] = winRate

            winRate
        }

        val mapKDR: HashMap<String, Double> = hashMapOf()
        val mapsByKDR = mapData.keys.sortedBy {
            val data = mapData[it]!!
            val kills = data.sumOf { data -> data.eliminations }
            val deaths = data.sumOf { data -> if (data.placement != 1) 1 else 0 }
            val kdr = kills.toDouble() / deaths.toDouble()
            if (kdr.isInfinite()) { return@sortedBy -1.0 }

            mapKDR[it] = kdr

            kdr
        }

        overview.child(
            UIComponents.label(
                Component.literal("Best Map (By WLR): ")
                    .append(Component.literal(mapsByWinRate.last()).withStyle(Style.EMPTY.withBold(true)))
                    .append(Component.literal(" - "))
                    .append(Component.literal(
                        "${Galapagos.decimalFormat.format(mapWinRates[mapsByWinRate.last()]!! * 100.0)}%")
                        .withColor(percentageToColor(mapWinRates[mapsByWinRate.last()]!!))
                    )
            ).shadow(true)
        )

        overview.child(
            UIComponents.label(
                Component.literal("Best Map (By KDR): ")
                    .append(Component.literal(mapsByKDR.last()).withStyle(Style.EMPTY.withBold(true)))
                    .append(Component.literal(" - "))
                    .append(Component.literal("${Galapagos.decimalFormat.format(mapKDR[mapsByKDR.last()]!!)}"))
            ).shadow(true)
        )

        overview.child(
            createCauseGraph(kills, "Kills: ")
                .margins(Insets.top(5))
        )

        overview.child(
            createCauseGraph(deaths, "Deaths: ")
                .margins(Insets.top(3))
        )
    }

}