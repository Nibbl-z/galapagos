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
import xyz.nibblz.galapagos.data.game.BATTLE_BOX_ARENA_MAP_DEFAULT_KITS
import xyz.nibblz.galapagos.data.game.BattleBoxArenaCoreKitType
import xyz.nibblz.galapagos.data.game.BattleBoxKit
import xyz.nibblz.galapagos.data.game.BattleBoxRound
import xyz.nibblz.galapagos.data.game.DeathCause
import xyz.nibblz.galapagos.data.game.bbaKitComponent
import xyz.nibblz.galapagos.features.XPInfo
import xyz.nibblz.galapagos.util.Glyphs
import xyz.nibblz.galapagos.util.percentageToColor
import kotlin.time.Instant

class BattleBoxArenaHistory : BaseHistory() {
    data class DataPoint (
        val win: Boolean,
        var eliminations: Int,
        var deaths: Int
    )

    val mapData: HashMap<String, MutableList<DataPoint>> = hashMapOf()
    val kills: HashMap<DeathCause, Int> = hashMapOf()
    val deaths: HashMap<DeathCause, Int> = hashMapOf()

    override val game = XPInfo.XPSource.BATTLE_BOX_ARENA

    override fun fillHistory(historyContainer: FlowLayout) {
        Galapagos.save.battleBoxArenaHistory.sortedByDescending { it.timestamp }.forEach {

            val dataPoint = DataPoint(
                it.rounds.count { round -> round == BattleBoxRound.WIN } == 5,
                it.getPlayer()?.kills ?: 0,
                it.getPlayer()?.deaths ?: 0
            )

            mapData.putIfAbsent(it.map, mutableListOf())
            mapData[it.map]?.add(dataPoint)
//
//            it.kits.forEach { kit ->
//                kitData.putIfAbsent(it.kit!!, mutableListOf())
//                kitData[it.kit]?.add(dataPoint)
//            }
//
            it.killCauses.forEach { cause -> kills[cause] = (kills[cause] ?: 0) + 1 }
            it.deathCauses.forEach { cause -> deaths[cause] = (deaths[cause] ?: 0) + 1 }

            val container = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content())
            container.gap(5)
            container.verticalAlignment(VerticalAlignment.CENTER)

            container.child(UIComponents.texture(Identifier.fromNamespaceAndPath(Galapagos.MOD_ID, "textures/battle_box_arena/${when(it.map) {
                "Cargo" -> "cargo"
                "Cherry Blossom" -> "cherry_blossom"
                "Classic Unboxed" -> "classic_unboxed"
                "Courtyard Unboxed" -> "courtyard_unboxed" // Love!
                "Fusion Core" -> "fusion_core"
                "Gold Mine Unboxed" -> "gold_mine_unboxed"
                "Heater" -> "heater"
                "Penthouse" -> "penthouse"
                "Platform" -> "platform" // Hate.
                "Santorini" -> "santorini"
                "Slay Unboxed" -> "slay_unboxed"
                "Spaceship Unboxed" -> "spaceship_unboxed"
                "Street" -> "street"
                "Train Station Unboxed" -> "train_station_unboxed"
                else -> "courtyard_unboxed"
            }}.png"),
                0, 0, 200, 60, 200, 60
            ).blend(true).positioning(Positioning.absolute(0,0)))

            val leftContent = UIContainers.verticalFlow(Sizing.fill(35), Sizing.content())
            leftContent.padding(Insets.of(5))
            container.child(leftContent)

            leftContent.child(UIComponents.label(Component.literal(it.map))
                .shadow(true)
            )

            val scores = GameStateHandler.GameState.BattleBoxArena.getScores(it)
            val roundsComponent = Component.empty()
                    .append(Component.literal("${scores.first}").withColor(if (scores.first == 5) ChatFormatting.GREEN.color!! else 0xFFFFFF))
                    .append(Component.literal(" - "))
                    .append(Component.literal("${scores.second}").withColor(if (scores.second == 5) ChatFormatting.RED.color!! else 0xFFFFFF))
                    .append(Component.literal(" - ").withColor(ChatFormatting.DARK_GRAY.color!!))
                    .append(
                        if (it.rankPoints != 0) Component.literal("${if (it.rankPoints > 0) "+" else ""}${it.rankPoints}")
                            .withColor(if (it.rankPoints > 0) ChatFormatting.GREEN.color!! else ChatFormatting.RED.color!!)
                        else Component.literal("?").withColor(ChatFormatting.GRAY.color!!)
                    )
                    .append(Component.literal(" RP").withColor(ChatFormatting.GRAY.color!!))


            leftContent.child(UIComponents.label(roundsComponent).shadow(true))

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

            val kits: HashMap<Pair<BattleBoxKit, BattleBoxArenaCoreKitType>, Int> = hashMapOf()

            it.kits.forEach { kit ->
                val type = BATTLE_BOX_ARENA_MAP_DEFAULT_KITS[it.map]?.get(kit.kit) ?: kit.core
                kits[kit.kit to type] = (kits[kit.kit to type] ?: 0) + 1
            }

            val mainKit = kits.entries.maxByOrNull { kit -> kit.value }

            if (mainKit != null) {
                rightContent.child(
                    UIComponents.label(
                        with(Component.literal("Kit: ")) {
                            append(bbaKitComponent(mainKit.key.first, mainKit.key.second))
                            append(Component.literal(" ${mainKit.key.second.label} ${mainKit.key.first.label}")
                                .withStyle(Style.EMPTY.withColor(mainKit.key.first.bbaColor))
                            )

                            if (kits.size > 1) append(Component.literal("\n + "))

                            kits.forEach { kit ->
                                if (kit != mainKit) append(bbaKitComponent(kit.key.first, kit.key.second).append(Component.literal(" ")))
                            }

                            append(Component.empty())
                        }
                    ).shadow(true)
                )
            }

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
            val wins = data.sumOf { data -> if (data.win) 1 else 0 }
            val losses = data.sumOf { data -> if (!data.win) 1 else 0 }
            val winRate = wins.toDouble() / (wins + losses).toDouble()
            if (winRate.isInfinite()) { return@sortedBy -1.0 }

            mapWinRates[it] = winRate

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

            if (!kills.isEmpty()) {
                overview.child(
                    createCauseGraph(kills, "Kills: ")
                        .margins(Insets.top(5))
                )
            }

            if (!deaths.isEmpty()) {
                overview.child(
                    createCauseGraph(deaths, "Deaths: ")
                        .margins(Insets.top(3))
                )
            }
        }
    }
}