package xyz.nibblz.galapagos.core.game_state_handlers

import com.noxcrew.noxesium.core.mcc.ClientboundMccStatisticPacket
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import xyz.nibblz.galapagos.core.GameStateHandler
import xyz.nibblz.galapagos.core.GameStateHandler.GameState
import xyz.nibblz.galapagos.core.GameStateHandler.usernameRegex
import xyz.nibblz.galapagos.core.game_state_handlers.HoleInTheWall.playerRegex
import xyz.nibblz.galapagos.mixin.PlayerTabOverlayAccessor

object ParkourWarriorSurvivor : Handler {
    val leapCompletionRegex = Regex("Leap \\d complete in: (?<min>\\d\\d):(?<sec>\\d\\d).(?<ms>\\d\\d\\d)")
    val leapPlacementRegex = Regex("\\[\\d+\\w+]")

    override fun handleStatisticPacket(packet: ClientboundMccStatisticPacket) {
        val state: GameState.ParkourWarriorSurvivor = GameStateHandler.currentState as GameState.ParkourWarriorSurvivor

        when(packet.statistic) {
            "pw_survival_obstacles_completed" -> state.obstaclesCompleted++
            "pw_survival_players_eliminated" -> state.playersEliminated++
        }
    }

    override fun handleSystemChatPacket(packet: ClientboundSystemChatPacket) {
        val state: GameState.ParkourWarriorSurvivor = GameStateHandler.currentState as GameState.ParkourWarriorSurvivor
        val message = packet.content.string

        if (message.contains("complete in: ")) {
            val match = leapCompletionRegex.find(message)?.groups ?: return
            val minutes = match["min"]?.value?.toIntOrNull() ?: 0
            val seconds = match["sec"]?.value?.toIntOrNull() ?: 0
            val milliseconds = match["ms"]?.value?.toIntOrNull() ?: 0

            val time = minutes * 1200 + seconds * 20 + milliseconds / 50
            state.leapCompletionTimes.add(time)
        }
    }

    override fun update() {
        val state: GameState.ParkourWarriorSurvivor = GameStateHandler.currentState as GameState.ParkourWarriorSurvivor

        val tabListPlayerIndexes: HashMap<Int, Int> = hashMapOf(
            0 to 20,
            1 to 21,
            2 to 22,
            3 to 23,
            4 to 24,
            5 to 25,
            6 to 26,
            7 to 27,
            40 to 60,
            41 to 61,
            42 to 62,
            43 to 63,
            44 to 64,
            45 to 65,
            46 to 66,
            47 to 67,
        )

        val tabList = (Minecraft.getInstance().gui.tabList as PlayerTabOverlayAccessor).`galapagos$getPlayerInfos`()
        val players: MutableList<GameStateHandler.BasicPlayerState> = mutableListOf()

        tabListPlayerIndexes.forEach { (usernameIndex, scoreIndex) ->
            val playerName = usernameRegex.find(tabList.getOrNull(usernameIndex)?.tabListDisplayName?.string ?: "")
                ?.groups?.get("username")?.value ?: return@forEach

            val score = playerRegex.find(tabList.getOrNull(scoreIndex)?.tabListDisplayName?.string ?: "")
                ?.groups?.get("score")?.value?.toIntOrNull() ?: 1

            players.add(GameStateHandler.BasicPlayerState(playerName, score))
        }

        state.players = players

        val tabListFooter = (Minecraft.getInstance().gui.tabList as PlayerTabOverlayAccessor).`galapagos$getFooter`().string
        val leapPlacementMatches = leapPlacementRegex.findAll(tabListFooter)

        val placements: MutableList<Int> = mutableListOf()

        leapPlacementMatches.forEach {
            val placement = it.value.filter { char -> char.isDigit() }.toIntOrNull() ?: -1
            placements.add(placement)
        }

        state.leapPlacements = placements
    }
}