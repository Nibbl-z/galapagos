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
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.core.GameStateHandler
import xyz.nibblz.galapagos.util.Glyphs
import xyz.nibblz.galapagos.util.mcciTextureComponent
import kotlin.time.Instant

class GameHistory : BaseOwoScreen<FlowLayout>() {
    override fun createAdapter(): OwoUIAdapter<FlowLayout?> {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow)
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

        rootComponent.child(
            UIContainers.verticalFlow(Sizing.fill(75), Sizing.fixed(80))
                .surface { context: OwoUIGraphics?, component: ParentUIComponent? ->
                    NinePatchTexture.draw(Identifier.fromNamespaceAndPath("galapagos", "mcci_panel"), context, component)
                }
                .positioning(Positioning.absolute(192, 10))
        )

        val historyContainer = UIContainers.verticalFlow(Sizing.content(), Sizing.content())
        historyContainer.padding(Insets.of(5))
        historyContainer.gap(3)

        Galapagos.save.battleBoxHistory.forEach {
            Galapagos.logger.info("this is ${it.map}")
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

            leftContent.child(UIComponents.label(Component.literal(time)).margins(Insets.top(10)))

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