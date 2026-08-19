package xyz.nibblz.galapagos.core.game_state_handlers

import com.noxcrew.noxesium.core.mcc.ClientboundMccGameStatePacket
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import xyz.nibblz.galapagos.core.GameStateHandler.BattleBoxPlayerState
import xyz.nibblz.galapagos.core.GameStateHandler.GameState
import xyz.nibblz.galapagos.core.GameStateHandler.currentState
import xyz.nibblz.galapagos.core.GameStateHandler.usernameRegex
import xyz.nibblz.galapagos.data.game.BattleBoxKit
import xyz.nibblz.galapagos.data.game.BattleBoxRound
import xyz.nibblz.galapagos.data.game.DeathCause
import xyz.nibblz.galapagos.mixin.PlayerTabOverlayAccessor
import xyz.nibblz.galapagos.util.findScoreboardLines

object BattleBox : Handler {
    override fun handleGameStatePacket(packet: ClientboundMccGameStatePacket) {
        val state: GameState.BattleBox = currentState as GameState.BattleBox
        if (state.map != "Unknown") return
        state.map = packet.mapName
    }

    override fun handleSystemChatPacket(packet: ClientboundSystemChatPacket) {
        val state: GameState.BattleBox = currentState as GameState.BattleBox
        val message = packet.content.string

        if (message.contains("You have chosen the")) {
            val kitName = Regex("You have chosen the (?<kit>[a-zA-Z]+) kit").find(message)
                ?.groups?.get("kit")?.value ?: return

            val kit = BattleBoxKit.entries.find { it.label == kitName } ?: return

            state.kit = kit
        }

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
        val state: GameState.BattleBox = currentState as GameState.BattleBox

        // The Concept Of Programming has filed a restraining order against me.
        val tabListTeamIndexes: HashMap<Int, HashMap<Int, Int>> = hashMapOf(
            0 to hashMapOf(1 to 21, 2 to 22, 3 to 23, 4 to 24),
            6 to hashMapOf(7 to 27, 8 to 28, 9 to 29, 10 to 30),
            40 to hashMapOf(41 to 61, 42 to 62, 43 to 63, 44 to 64),
            46 to hashMapOf(47 to 67, 48 to 68, 49 to 69, 50 to 70)
        )

        val tabList = (Minecraft.getInstance().gui.tabList as PlayerTabOverlayAccessor).`galapagos$getPlayerInfos`()

        val players: MutableList<BattleBoxPlayerState> = mutableListOf()

        tabListTeamIndexes.forEach { (teamIndex, playerIndexes) ->
            val teamName = Regex("(?<team>[a-zA-Z]+) Team").find(tabList.getOrNull(teamIndex)?.tabListDisplayName?.string ?: "")
                ?.groups?.get("team")?.value ?: return@forEach

            playerIndexes.forEach { (usernameIndex, statsIndex) ->
                val playerName = usernameRegex.find(tabList[usernameIndex].tabListDisplayName?.string ?: "")
                    ?.groups?.get("username")?.value ?: return@forEach

                val statsMatch = Regex("(?<kills>\\d+).+?(?<deaths>\\d+).+?(?<assists>\\d+).+?(?<score>\\d+)").find(tabList[statsIndex].tabListDisplayName?.string ?: "")
                    ?.groups ?: return@forEach

                val kills = statsMatch["kills"]?.value?.toIntOrNull() ?: return@forEach
                val deaths = statsMatch["deaths"]?.value?.toIntOrNull() ?: return@forEach
                val assists = statsMatch["assists"]?.value?.toIntOrNull() ?: return@forEach
                val score = statsMatch["score"]?.value?.toIntOrNull() ?: return@forEach

                players.add(BattleBoxPlayerState(playerName, score, kills, deaths, assists, teamName))
            }
        }

        state.players = players

        val roundsMatch = findScoreboardLines(Regex("\\[.]"))

        if (roundsMatch != null) {
            val rounds: MutableList<BattleBoxRound> = mutableListOf()

            roundsMatch.forEach {
                val letter = it.value[1]
                val round = BattleBoxRound.entries.find { r -> r.scoreboardLetter == letter } ?: return@forEach
                rounds.add(round)
            }

            state.rounds = rounds
        }
    }
}