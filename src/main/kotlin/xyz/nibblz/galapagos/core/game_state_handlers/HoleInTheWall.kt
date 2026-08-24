package xyz.nibblz.galapagos.core.game_state_handlers

import com.noxcrew.noxesium.core.mcc.ClientboundMccGameStatePacket
import com.noxcrew.noxesium.core.mcc.ClientboundMccStatisticPacket
import net.minecraft.client.Minecraft
import xyz.nibblz.galapagos.core.GameStateHandler
import xyz.nibblz.galapagos.core.GameStateHandler.GameState
import xyz.nibblz.galapagos.core.GameStateHandler.currentState
import xyz.nibblz.galapagos.core.GameStateHandler.usernameRegex
import xyz.nibblz.galapagos.data.game.WallType
import xyz.nibblz.galapagos.mixin.PlayerTabOverlayAccessor
import xyz.nibblz.galapagos.util.getScoreboardLines
import kotlin.math.ceil

object HoleInTheWall : Handler {
    val playerRegex = Regex("(?<score>\\b\\d+\\b)")

    override fun handleGameStatePacket(packet: ClientboundMccGameStatePacket) {
        val state: GameState.HoleInTheWall = currentState as GameState.HoleInTheWall
        if (state.map != "Unknown") return
        state.map = packet.mapName
    }

    override fun handleStatisticPacket(packet: ClientboundMccStatisticPacket) {
        val state: GameState.HoleInTheWall = currentState as GameState.HoleInTheWall

        when(packet.statistic) {
            "hole_in_the_wall_walls_dodged" -> state.wallsSurvived++
            "hole_in_the_wall_playtime" -> state.timeSurvived = ceil(packet.value.toDouble() / 20.0).toInt()
        }
    }

    override fun update() {
        val state: GameState.HoleInTheWall = currentState as GameState.HoleInTheWall

        val tabListPlayerIndexes: HashMap<Int, Int> = hashMapOf(
            0 to 20,
            1 to 21,
            2 to 22,
            3 to 23,
            4 to 24,
            5 to 25,
            40 to 60,
            41 to 61,
            42 to 62,
            43 to 63,
            44 to 64,
            45 to 65
        )

        // todo, this will prob be reused for other solo games with just score
        val tabList = (Minecraft.getInstance().gui.tabList as PlayerTabOverlayAccessor).`galapagos$getPlayerInfos`() ?: return
        val players: MutableList<GameStateHandler.BasicPlayerState> = mutableListOf()

        tabListPlayerIndexes.forEach { (usernameIndex, scoreIndex) ->
            val playerName = usernameRegex.find(tabList.getOrNull(usernameIndex)?.tabListDisplayName?.string ?: "")
                ?.groups?.get("username")?.value ?: return@forEach

            val score = playerRegex.find(tabList.getOrNull(scoreIndex)?.tabListDisplayName?.string ?: "")
                ?.groups?.get("score")?.value?.toIntOrNull() ?: return@forEach

            players.add(GameStateHandler.BasicPlayerState(playerName, score))
        }

        state.players = players

        val wallTypes: MutableList<WallType> = mutableListOf()

        getScoreboardLines().forEach { line ->
            val wallType = WallType.entries.find { line.contains(it.label) } ?: return@forEach
            wallTypes.add(wallType)
        }

        state.wallTypes = wallTypes
    }
}