package xyz.nibblz.galapagos.core.game_state_handlers

import com.noxcrew.noxesium.core.mcc.ClientboundMccGameStatePacket
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.core.GameStateHandler
import xyz.nibblz.galapagos.core.GameStateHandler.BattleBoxPlayerState
import xyz.nibblz.galapagos.core.GameStateHandler.GameState
import xyz.nibblz.galapagos.core.GameStateHandler.currentState
import xyz.nibblz.galapagos.core.GameStateHandler.usernameRegex
import xyz.nibblz.galapagos.data.game.*
import xyz.nibblz.galapagos.mixin.PlayerTabOverlayAccessor
import xyz.nibblz.galapagos.util.Glyphs
import xyz.nibblz.galapagos.util.getScoreboardLinesComponents

object BattleBoxArena : Handler {
    val kitSelectRegexes: List<Regex> = listOf(
        Regex("You selected .+?(?<kit>\\w+)"),
        Regex("You have swapped to .+?(?<kit>\\w+)"),
        Regex("You swapped .+ to .+?(?<corekit>\\w+) (?<kit>\\w+)")
    )

    val roundsRegex = Regex("ROUND \\[(?<rounds>\\d+)/")
    val roundStartRegex = Regex("Round (?<round>\\d+) started")

    var chosenKit: Pair<BattleBoxKit, BattleBoxArenaCoreKits> = BattleBoxKit.SHARPSHOOTER to BattleBoxArenaCoreKits.NONE
    var storeRoundStats = false

    override fun handleGameStatePacket(packet: ClientboundMccGameStatePacket) {
        val state: GameState.BattleBoxArena = currentState as GameState.BattleBoxArena
        if (state.map != "Unknown") return
        state.map = packet.mapName
    }

    override fun handleSystemChatPacket(packet: ClientboundSystemChatPacket) {
        val state: GameState.BattleBoxArena = currentState as GameState.BattleBoxArena
        val message = packet.content.string

        if (message.contains("You")) {
            val match = kitSelectRegexes.firstNotNullOfOrNull {
                it.find(message)?.groups
            } ?: return

            val kitName = match["kit"]?.value
            val kit = BattleBoxKit.entries.find { it.label == kitName } ?: return


            val coreKitName = try { match["corekit"]?.value } catch(_: IllegalArgumentException) { "" }
            val coreKit = BattleBoxArenaCoreKits.entries.find { it.label == coreKitName } ?: BattleBoxArenaCoreKits.NONE

            chosenKit = kit to coreKit
        }

        if (message.contains("Rank Points")) {
            val points = message.filter { it.isDigit() }.toIntOrNull() ?: 0

            when {
                message.contains("lost") -> state.rankPoints = -points
                message.contains("gained") -> state.rankPoints = points
            }
        }

        if (message.contains("Round") && message.contains("started!")) {
            val round = roundStartRegex.find(message)?.groups?.get("round")?.value?.toIntOrNull() ?: -1
            state.kits.add(BattleBoxArenaKitChoice(chosenKit.first, chosenKit.second, round))
        }

        if (message.contains("Round") && message.contains("Over!")) storeRoundStats = true

        if (message.contains(Minecraft.getInstance().user.name, true)) {
            val cause = DeathCause.entries.find { it.messages.any { phrase -> message.contains(phrase, true) } } ?: DeathCause.UNKNOWN

            // is this stupid? yes. will it work? maybe.
            val nameLocation = message.indexOf(Minecraft.getInstance().user.name, 0, false)
            val causeLocation = cause.messages.firstNotNullOfOrNull {
                val index = message.indexOf(it)
                if (index == -1) null else index
            } ?: -1
            val isKill = nameLocation > causeLocation

            if (isKill) state.killCauses.add(cause) else state.deathCauses.add(cause)
        }
    }

    override fun update() {
        val state: GameState.BattleBoxArena = currentState as GameState.BattleBoxArena

        val tabListTeamIndexes: HashMap<Int, HashMap<Int, Int>> = hashMapOf(
            0 to hashMapOf(1 to 21, 2 to 22, 3 to 23, 4 to 24),
            40 to hashMapOf(41 to 61, 42 to 62, 43 to 63, 44 to 64),
        )

        val tabList = (Minecraft.getInstance().gui.tabList as PlayerTabOverlayAccessor).`galapagos$getPlayerInfos`()

        val players: MutableList<BattleBoxPlayerState> = mutableListOf()

        tabListTeamIndexes.forEach { (teamIndex, playerIndexes) ->
            val teamName = Regex("(?<team>[a-zA-Z]+) Team").find(tabList.getOrNull(teamIndex)?.tabListDisplayName?.string ?: "")
                ?.groups?.get("team")?.value ?: return@forEach

            playerIndexes.forEach { (usernameIndex, statsIndex) ->
                val playerName = usernameRegex.find(tabList[usernameIndex].tabListDisplayName?.string ?: "")
                    ?.groups?.get("username")?.value ?: return@forEach

                val statsMatch = Regex("(?<kills>\\d+).+?(?<deaths>\\d+).+?(?<assists>\\d+).+?(?<score>\\d+)").find(tabList.getOrNull(statsIndex)?.tabListDisplayName?.string ?: "")
                    ?.groups ?: return@forEach

                val kills = statsMatch["kills"]?.value?.toIntOrNull() ?: return@forEach
                val deaths = statsMatch["deaths"]?.value?.toIntOrNull() ?: return@forEach
                val assists = statsMatch["assists"]?.value?.toIntOrNull() ?: return@forEach
                val score = statsMatch["score"]?.value?.toIntOrNull() ?: return@forEach

                players.add(BattleBoxPlayerState(playerName, score, kills, deaths, assists, teamName))
            }
        }

        state.players = players

        if (storeRoundStats) {
            storeRoundStats = false
            val previous = state.roundStats.getOrNull(state.roundStats.size - 1)
            val stats = state.getPlayer()

            if (previous == null) {
                state.roundStats.add(GameStateHandler.BattleBoxRoundStats(
                    kills = stats?.kills ?: 0,
                    deaths = stats?.deaths ?: 0,
                    assists = stats?.assists ?: 0,
                    score = stats?.score ?: 0,
                ))
            } else {
                state.roundStats.add(GameStateHandler.BattleBoxRoundStats(
                    kills = (stats?.kills ?: 0) - previous.kills,
                    deaths = (stats?.deaths ?: 0) - previous.deaths,
                    assists = (stats?.assists ?: 0) - previous.assists,
                    score = (stats?.score ?: 0) - previous.score,
                ))
            }
        }

        val rounds: MutableList<BattleBoxRound> = mutableListOf()
        val tabListFooter = (Minecraft.getInstance().gui.tabList as PlayerTabOverlayAccessor).`galapagos$getFooter`() ?: return

        val roundsPlayed = roundsRegex.find(tabListFooter.string)?.groups?.get("rounds")?.value?.toIntOrNull() ?: 99 // better to have excess DRAW data than nothing ig idk blehhhhhhh

        val drawGlyph = Glyphs.getGlyphs("_fonts/icon/point_tracker/team_gray.png")
        val winLossGlyph = Glyphs.getGlyphs("_fonts/icon/point_tracker/team.png")

        val scoreboardPlayerComponent = getScoreboardLinesComponents().firstOrNull { it.string.contains(Minecraft.getInstance().user.name) } ?: return
        val teamColor = scoreboardPlayerComponent.toFlatList().find { it.string.contains(Minecraft.getInstance().user.name) }?.style?.color?.value ?: return
        Galapagos.logger.info("your team color is $teamColor")
        Galapagos.logger.info("ive played $roundsPlayed $drawGlyph $winLossGlyph")

        tabListFooter.toFlatList().forEach {
            Galapagos.logger.info("${it.string} - $drawGlyph / $winLossGlyph ${rounds.size}")
            if (rounds.size == roundsPlayed) return@forEach
            when {
                drawGlyph.any { glyph -> it.string.contains(glyph) } -> rounds.add(BattleBoxRound.DRAW)
                winLossGlyph.any { glyph -> it.string.contains(glyph) } -> {
                    Galapagos.logger.info("this team color of this square is ${it.style.color?.value}")
                    val color = it.style.color?.value ?: return@forEach
                    rounds.add(if (color == teamColor) BattleBoxRound.WIN else BattleBoxRound.LOSS)
                }
            }
        }

        state.rounds = rounds
    }
}