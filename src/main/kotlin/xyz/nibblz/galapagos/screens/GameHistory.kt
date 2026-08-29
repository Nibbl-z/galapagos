package xyz.nibblz.galapagos.screens

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.ScrollContainer
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.*
import io.wispforest.owo.ui.util.NinePatchTexture
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.core.GameStateHandler
import xyz.nibblz.galapagos.data.game.BattleBoxKit
import xyz.nibblz.galapagos.data.game.DeathCause
import xyz.nibblz.galapagos.util.Glyphs
import xyz.nibblz.galapagos.util.mcciTextureComponent
import xyz.nibblz.galapagos.util.percentageToColor
import kotlin.time.Instant

class GameHistory : BaseOwoScreen<FlowLayout>() {
    override fun createAdapter(): OwoUIAdapter<FlowLayout?> {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow)
    }

    val graphColors: List<Int> = listOf(
        0x77ff6f,
        0x6fef67,
        0x67df5f,
        0x5fcf57,
        0x5bbf4f,
        0x53af47,
        0x4b9f3f,
        0x439337,
        0x3f832f,
        0x37732b,
        0x2f6323,
        0x27531b,
        0x1f4317,
        0x17330f,
        0x13230b,
        0x0b1707
    )

    fun createCauseGraph(data: HashMap<DeathCause, Int>, label: String): FlowLayout {
        val percents: HashMap<DeathCause, Double> = hashMapOf()
        val sum = data.values.sumOf { it }

        data.forEach { (cause, count) ->
            percents[cause] = (count.toDouble() / sum.toDouble())
        }

        val sorted = percents.entries.sortedByDescending { it.value }

        val mult = graphColors.size / sorted.size

        val graph = UIContainers.horizontalFlow(Sizing.expand(), Sizing.fixed(10))
        sorted.forEachIndexed { index, (_, percent) ->
            graph.child(
                UIComponents.box(Sizing.fill((percent * 100).toInt()), Sizing.fill())
                    .fill(true)
                    .color(Color.ofRgb(graphColors.getOrNull(index * mult) ?: 0xFFFFFF))
            )
        }

        val breakdownComponent = with(Component.empty()) {
            sorted.forEachIndexed { index, (cause, _) ->
                if (cause.sprite.contains("_fonts")) append(Glyphs.getGlyphComponent("${cause.sprite}.png"))
                else if (cause.sprite.contains("/")) append(mcciTextureComponent(cause.sprite))
                else append(Component.literal(cause.sprite).withColor(0xffffff).withStyle(Style.EMPTY.withFont(Galapagos.font).withHoverEvent(
                    HoverEvent.ShowText(Component.literal(cause.label))
                )))
                append(Component.literal(" ${data[cause]} ").withColor(graphColors.getOrNull(index * mult) ?: 0xFFFFFF).withStyle(Style.EMPTY.withHoverEvent(
                    HoverEvent.ShowText(Component.literal(cause.label))
                )))
            }

            append(Component.empty()) // goog
        }

        val root = UIContainers.verticalFlow(Sizing.fill(), Sizing.content())
            .child(UIContainers.horizontalFlow(Sizing.fill(), Sizing.fixed(10))
                .child(UIComponents.label(Component.literal(label)).shadow(true).horizontalSizing(Sizing.fill(15)))
                .child(graph))
            .child(UIComponents.label(breakdownComponent).shadow(true))
            .gap(2)


        return root
    }

    override fun build(rootComponent: FlowLayout) {
        rootComponent
            .surface(Surface.blur(10f, 5f))
            .horizontalAlignment(HorizontalAlignment.CENTER)
            .verticalAlignment(VerticalAlignment.CENTER)

        rootComponent.child(
            UIContainers.verticalFlow(Sizing.fill(), Sizing.fill())
                .surface(Surface.VANILLA_TRANSLUCENT)
                .positioning(Positioning.absolute(0, 0))
        )

        rootComponent.child(
            UIComponents.texture(Identifier.fromNamespaceAndPath("mcc", "textures/island_lobby/game_lobby/bb_regular.png"),
                0, 0, 192, 100, 192, 384
            ).positioning(Positioning.absolute(10, 10))
        )

        val overview = UIContainers.verticalFlow(Sizing.fill(30), Sizing.fixed(110))

        val historyContainer = UIContainers.verticalFlow(Sizing.content(), Sizing.content())
        historyContainer.padding(Insets.of(5))
        historyContainer.gap(3)

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
                "${date.month.name.lowercase().replaceFirstChar { char -> char.uppercase() }} ${date.day}, ${date.year}, ${time}"
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

        overview.child(
            UIComponents.label(Component.literal("- Statistics -").withStyle(Style.EMPTY.withBold(true)))
                .shadow(true)
                .horizontalTextAlignment(HorizontalAlignment.CENTER)
                .horizontalSizing(Sizing.fill())
                .margins(Insets.bottom(5))
        )

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
                //UIComponents.label(Component.literal("Best Map (By KDR): ${mapsByKDR.last()} - ${mapKDR[mapsByKDR.last()]}")).shadow(true)
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



        overview
            .surface { context: OwoUIGraphics?, component: ParentUIComponent? ->
                NinePatchTexture.draw(Identifier.fromNamespaceAndPath("galapagos", "mcci_panel"), context, component)
            }
            .padding(Insets.of(5))
            .margins(Insets.bottom(5))


        rootComponent.child(overview)

        rootComponent.child(
            UIContainers.verticalScroll(Sizing.fill(30), Sizing.fill(60),historyContainer)
            .scrollbarThiccness(4) // are we fr
            .scrollbar(ScrollContainer.Scrollbar.flat(Color.ofRgb(0x1c2b46)))
            .surface { context: OwoUIGraphics?, component: ParentUIComponent? ->
                NinePatchTexture.draw(Identifier.fromNamespaceAndPath("galapagos", "mcci_panel"), context, component)
            }
        )
    }
}
