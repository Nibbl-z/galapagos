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
import xyz.nibblz.galapagos.core.GameStateHandler
import xyz.nibblz.galapagos.data.game.BattleBoxKit
import xyz.nibblz.galapagos.data.game.DeathCause
import xyz.nibblz.galapagos.features.XPInfo
import xyz.nibblz.galapagos.util.Glyphs
import xyz.nibblz.galapagos.util.mcciTextureComponent
import xyz.nibblz.galapagos.util.percentageToColor
import kotlin.collections.get
import kotlin.time.Instant

class BattleBoxHistory : BaseHistory() {
    data class DataPoint (
        val teamPlacement: Int,
        val indivPlacement: Int,
        var eliminations: Int,
        var deaths: Int
    )

    val mapData: HashMap<String, MutableList<DataPoint>> = hashMapOf()
    val kitData: HashMap<BattleBoxKit, MutableList<DataPoint>> = hashMapOf()
    val kills: HashMap<DeathCause, Int> = hashMapOf()
    val deaths: HashMap<DeathCause, Int> = hashMapOf()

    override val game = XPInfo.XPSource.BATTLE_BOX_QUADS

    override fun fillHistory(historyContainer: FlowLayout) {
        Galapagos.save.battleBoxHistory.sortedByDescending { it.timestamp }.forEach {
            val dataPoint = DataPoint(
                GameStateHandler.GameState.BattleBox.teamPlacement(it),
                it.getPlacement(true),
                it.getPlayer()?.kills ?: 0,
                it.getPlayer()?.deaths ?: 0
            )

            mapData.putIfAbsent(it.map, mutableListOf())
            mapData[it.map]?.add(dataPoint)
            if (it.kit != null) {
                kitData.putIfAbsent(it.kit!!, mutableListOf())
                kitData[it.kit]?.add(dataPoint)
            }

            it.killCauses.forEach { cause -> kills[cause] = (kills[cause] ?: 0) + 1 }
            it.deathCauses.forEach { cause -> deaths[cause] = (deaths[cause] ?: 0) + 1 }

            val container = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content())
            container.gap(5)
            container.verticalAlignment(VerticalAlignment.CENTER)

            container.child(UIComponents.texture(Identifier.fromNamespaceAndPath(Galapagos.MOD_ID, "textures/battle_box/${when(it.map) {
                "Canal" -> "canal"
                "Paint Factory" -> "paint_factory"
                "Train Station" -> "train_station"
                "Gold Mine" -> "gold_mine"
                "Pipes" -> "pipes"
                "Haunted House" -> "haunted_house"
                "Slay" -> "slay"
                "Watermill" -> "watermill"
                "Ships" -> "ships"
                "Prison" -> "prison"
                "Villa" -> "villa"
                "Pumpkin Patch" -> "pumpkin_patch"
                "Spa" -> "spa"
                "Courtyard" -> "courtyard"
                "Foundry" -> "foundry"
                "Classic" -> "classic"
                "Rig" -> "rig"
                "Spaceship" -> "spaceship"
                "Trenches" -> "trenches"
                "Graffiti" -> "graffiti"
                "Dojo" -> "dojo"
                "Forts" -> "forts"
                "Santa's Slay" -> "santas_slay"
                "Ice Fishing" -> "ice_fishing"
                else -> "canal"
            }}.png"),
                0, 0, 200, 60, 200, 60
            ).blend(true).positioning(Positioning.absolute(0,0)))

            val leftContent = UIContainers.verticalFlow(Sizing.fill(50), Sizing.content())
            leftContent.padding(Insets.of(5))
            container.child(leftContent)

            leftContent.child(UIComponents.label(Component.literal(it.map))
                .shadow(true)
            )

            var roundsComponent = Component.empty()

            repeat(3) { i ->
                val round = it.rounds.getOrNull(i)

                roundsComponent = roundsComponent
                    .append(Component.literal("[").withColor(ChatFormatting.DARK_GRAY.color!!))
                    .append(Component.literal(round?.scoreboardLetter?.toString() ?: "?").withColor(round?.color ?: ChatFormatting.GRAY.color!!))
                    .append(Component.literal("] ").withColor(ChatFormatting.DARK_GRAY.color!!))
            }

            val teamPlacement = GameStateHandler.GameState.BattleBox.teamPlacement(it)
            roundsComponent = roundsComponent.append(Component.literal("- ").withColor(ChatFormatting.DARK_GRAY.color!!))
            roundsComponent = roundsComponent.append(when(teamPlacement) {
                1 -> Component.literal("1st").withColor(ChatFormatting.YELLOW.color!!).withStyle(Style.EMPTY.withBold(true))
                2 -> Component.literal("2nd").withColor(ChatFormatting.GRAY.color!!)
                3 -> Component.literal("3rd").withColor(0x9e5b39)
                4 -> Component.literal("4th").withColor(ChatFormatting.DARK_GRAY.color!!)
                else -> Component.literal("?th").withColor(ChatFormatting.DARK_GRAY.color!!)
            })

            leftContent.child(UIComponents.label(roundsComponent)
                .shadow(true)
            )

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

            val rightContent = UIContainers.verticalFlow(Sizing.fill(50), Sizing.content())
            rightContent.padding(Insets.of(5))
            container.child(rightContent)

            rightContent.child(
                UIComponents.label(
                    Component.literal("Kit: ")
                        .append(mcciTextureComponent(it.kit?.bbSprite() ?: ""))
                        .append(Component.literal(" ${it.kit?.label}")
                            .withStyle(Style.EMPTY.withBold(true).withColor(it.kit?.bbColor ?: 0xffffff))
                        )
                ).shadow(true)
            )

            val playerStats = it.getPlayer()

            rightContent.child(
                UIComponents.label(
                    Component.literal("${playerStats?.kills} ")
                        .append(Glyphs.getGlyphComponent("_fonts/icon/kills.png"))
                        .append(Component.literal(" ${playerStats?.deaths} "))
                        .append(Glyphs.getGlyphComponent("_fonts/icon/skull_small.png"))
                        .append(Component.literal(" ${playerStats?.assists} "))
                        .append(Glyphs.getGlyphComponent("_fonts/icon/assists.png"))
                )
            )

            val indivPlacement = it.getPlacement(true)

            rightContent.child(
                UIComponents.label(
                    Component.literal("${playerStats?.score} ")
                        .append(Glyphs.getGlyphComponent("_fonts/icon/score_point.png"))
                        .append(Component.literal(" - ").withColor(ChatFormatting.DARK_GRAY.color!!))
                        .append(when(indivPlacement) {
                            1 -> Component.literal("1st").withColor(ChatFormatting.YELLOW.color!!).withStyle(Style.EMPTY.withBold(true))
                            2 -> Component.literal("2nd").withColor(0x95879c).withStyle(Style.EMPTY.withBold(true))
                            3 -> Component.literal("3rd").withColor(0xe69840).withStyle(Style.EMPTY.withBold(true))
                            else -> Component.literal("${indivPlacement}th").withColor(ChatFormatting.WHITE.color!!)
                        })
                )
            )

            container.surface(Surface.VANILLA_TRANSLUCENT)

            historyContainer.child(container)
        }
    }

    override fun fillOverview(overview: FlowLayout) {
        val mapWinRates: HashMap<String, Double> = hashMapOf()
        val mapsByWinRate = mapData.keys.sortedBy {
            val data = mapData[it]!!
            val wins = data.sumOf { data -> if (data.teamPlacement == 1) 1 else 0 }
            val losses = data.sumOf { data -> if (data.teamPlacement != 1) 1 else 0 }
            val winRate = wins.toDouble() / (wins + losses).toDouble()
            if (winRate.isInfinite()) { return@sortedBy -1.0 }

            mapWinRates[it] = winRate

            winRate
        }

        val kitWinRates: HashMap<BattleBoxKit, Double> = hashMapOf()
        val kitsByWinRate = kitData.keys.sortedBy {
            val data = kitData[it]!!
            val wins = data.sumOf { data -> if (data.teamPlacement == 1) 1 else 0 }
            val losses = data.sumOf { data -> if (data.teamPlacement != 1) 1 else 0 }
            val winRate = wins.toDouble() / (wins + losses).toDouble()
            if (winRate.isInfinite()) { return@sortedBy -1.0 }

            kitWinRates[it] = winRate

            winRate
        }

        val mapKDR: HashMap<String, Double> = hashMapOf()
        val mapsByKDR = mapData.keys.sortedBy {
            val data = mapData[it]!!
            val kills = data.sumOf { data -> data.eliminations }
            val deaths = data.sumOf { data -> data.deaths }
            val kdr = kills.toDouble() / deaths.toDouble()
            if (kdr.isInfinite()) { return@sortedBy -1.0 }

            mapKDR[it] = kdr

            kdr
        }

        val kitKDR: HashMap<BattleBoxKit, Double> = hashMapOf()
        val kitsByKDR = kitData.keys.sortedBy {
            val data = kitData[it]!!
            val kills = data.sumOf { data -> data.eliminations }
            val deaths = data.sumOf { data -> data.deaths }
            val kdr = kills.toDouble() / deaths.toDouble()
            if (kdr.isInfinite()) { return@sortedBy -1.0 }

            kitKDR[it] = kdr

            kdr
        }

        if (!mapsByWinRate.isEmpty()) {
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
        }

        if (!kitsByWinRate.isEmpty()) {
            overview.child(
                UIComponents.label(
                    Component.literal("Best Kit (By WLR): ")
                        .append(mcciTextureComponent(kitsByWinRate.last().bbSprite()))
                        .append(Component.literal(" ${kitsByWinRate.last().label}")
                            .withStyle(Style.EMPTY.withBold(true).withColor(kitsByWinRate.last().bbColor))
                        )
                        .append(Component.literal(" - "))
                        .append(Component.literal(
                            "${Galapagos.decimalFormat.format(kitWinRates[kitsByWinRate.last()]!! * 100.0)}%")
                            .withColor(percentageToColor(kitWinRates[kitsByWinRate.last()]!!))
                        )
                ).shadow(true)
            )

            overview.child(
                UIComponents.label(
                    Component.literal("Best Kit (By KDR): ")
                        .append(mcciTextureComponent(kitsByKDR.last().bbSprite()))
                        .append(Component.literal(" ${kitsByKDR.last().label}")
                            .withStyle(Style.EMPTY.withBold(true).withColor(kitsByKDR.last().bbColor))
                        )
                        .append(Component.literal(" - "))
                        .append(Component.literal("${Galapagos.decimalFormat.format(kitKDR[kitsByKDR.last()]!!)}"))
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
}