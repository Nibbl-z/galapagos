package xyz.nibblz.galapagos.core

import com.noxcrew.noxesium.core.mcc.ClientboundMccGameStatePacket
import com.noxcrew.noxesium.core.mcc.ClientboundMccServerPacket
import kotlinx.serialization.Serializable
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.data.BattleBoxArenaCoreKits
import xyz.nibblz.galapagos.data.BattleBoxKit
import xyz.nibblz.galapagos.data.BattleBoxRound
import xyz.nibblz.galapagos.events.MCCGameStateEvent
import xyz.nibblz.galapagos.events.MCCServerEvent
import xyz.nibblz.galapagos.events.MCCStatisticEvent
import xyz.nibblz.galapagos.events.SystemChatEvent
import xyz.nibblz.galapagos.features.XPInfo
import xyz.nibblz.galapagos.mixin.PlayerTabOverlayAccessor
import xyz.nibblz.galapagos.util.findScoreboardLines
import kotlin.time.Clock

object GameStateHandler : CoreFeature {
    // shoutout to devcmb
    // tonight im gonna fall asleep to kotlin documentation
    @Serializable
    sealed class GameState {
        abstract var players: List<PlayerState>
        var timestamp: Long = 0

        @Serializable
        class BattleBox(override var players: List<PlayerState> = listOf()) : GameState() {
            var playerStates: HashMap<String, BattleBoxPlayerState> = hashMapOf()
            var map: String = "Map"
            var kit: BattleBoxKit? = null
            var rounds: MutableList<BattleBoxRound> = mutableListOf()

            override fun projectedXP(): Int {
                val eliminations = (playerStates[Minecraft.getInstance().user.name]?.kills ?: 0) + (playerStates[Minecraft.getInstance().user.name]?.assists ?: 0)
                val roundsWon = rounds.sumOf { if (it == BattleBoxRound.WIN) 1 else 0 }
                val roundsPlayed = rounds.size

                return (eliminations * 10 + roundsPlayed * 30 + roundsWon * 45)
            }
        }

        class BattleBoxArena(override var players: List<PlayerState>) : GameState() {
            var playerStates: HashMap<String, BattleBoxPlayerState> = hashMapOf()
            var map: String = "Map"
            var kits: MutableList<Pair<BattleBoxKit, BattleBoxArenaCoreKits>> = mutableListOf()
            var rounds: MutableList<BattleBoxRound> = mutableListOf()

            override fun projectedXP(): Int {
                val eliminations = playerStates[Minecraft.getInstance().user.name]!!.kills + playerStates[Minecraft.getInstance().user.name]!!.assists
                val roundsWon = rounds.sumOf { if (it == BattleBoxRound.WIN) 1 else 0 }
                val roundsPlayed = rounds.size

                return (eliminations * 10 + roundsPlayed * 30 + roundsWon * 45)
            }
        }

        fun getScore(): Int {
            return players.find { it.username == Minecraft.getInstance().user.name }?.score ?: -1
        }

        fun getPlacement(): Int {
            var placement = 1
            val score = getScore()

            players.filter { it.username != Minecraft.getInstance().user.name }.forEach {
                if (it.score > score) placement++
            }

            return placement
        }

        abstract fun projectedXP(): Int
    }

    @Serializable
    data class PlayerState(
        val username: String,
        var score: Int
    )

    // BB/BBA

    @Serializable
    data class BattleBoxPlayerState(
        var kills: Int,
        var deaths: Int,
        var assists: Int,
        val team: String
    )

    var currentGame: XPInfo.XPSource? = null
    var currentState: GameState? = null
    var updateState = false
    var updateDelay = 5

    override fun init() {
        MCCServerEvent.EVENT.register { packet -> mccServer(packet) }
        MCCGameStateEvent.EVENT.register { packet -> mccGameState(packet) }
        MCCStatisticEvent.EVENT.register { mccStatistic() }
        SystemChatEvent.EVENT.register { packet -> systemChat(packet) }
        ClientTickEvents.END_CLIENT_TICK.register {
            if (!updateState) return@register
            updateDelay--

            if (updateDelay == 0) {
                updateDelay = 5
                updateState = false

                when(currentGame) {
                    XPInfo.XPSource.BATTLE_BOX_QUADS -> updateBattleBoxState()
                    else -> {}
                }
            }
        }
    }

    fun mccServer(packet: ClientboundMccServerPacket) {
        currentGame = if (packet.server != "game") null
        else XPInfo.XPSource.entries.firstOrNull {
            it.serverTypes.all { type ->
                if (type.startsWith("!")) !packet.types.contains(type.removePrefix("!")) else packet.types.contains(type)
            }
        }

        Galapagos.logger.info("$currentGame")

        if (currentGame == null) {
            if (currentState != null) {
                when(currentState!!::class) {
                    GameState.BattleBox::class -> Galapagos.save.battleBoxHistory.add(currentState as GameState.BattleBox)
                }
            }
            currentState = null
            return
        }
        currentState = when(currentGame) {
            XPInfo.XPSource.BATTLE_BOX_QUADS -> GameState.BattleBox(listOf())
            else -> GameState.BattleBox(listOf())
        }

        currentState!!.timestamp = Clock.System.now().epochSeconds
    }

    fun mccGameState(packet: ClientboundMccGameStatePacket) {
        when(currentGame) {
            XPInfo.XPSource.BATTLE_BOX_QUADS -> updateBattleBoxState(packet)
            else -> {}
        }
    }

    fun mccStatistic() {
        updateState = true
    }

    fun systemChat(packet: ClientboundSystemChatPacket) {
        when(currentGame) {
            XPInfo.XPSource.BATTLE_BOX_QUADS -> updateBattleBoxState(packet)
            else -> {}
        }
    }

    fun updateBattleBoxState(packet: ClientboundMccGameStatePacket) {
        val state: GameState.BattleBox = currentState as GameState.BattleBox

        state.map = packet.mapName

        updateState = true
    }

    fun updateBattleBoxState() {
        val state: GameState.BattleBox = currentState as GameState.BattleBox

        // The Concept Of Programming has filed a restraining order against me.
        val tabListTeamIndexes: HashMap<Int, List<Pair<Int, Int>>> = hashMapOf(
            0 to listOf(1 to 21, 2 to 22, 3 to 23, 4 to 24),
            6 to listOf(7 to 27, 8 to 28, 9 to 29, 10 to 30),
            40 to listOf(41 to 61, 42 to 62, 43 to 63, 44 to 64),
            46 to listOf(47 to 67, 48 to 68, 49 to 69, 50 to 70)
        )

        val tabList = (Minecraft.getInstance().gui.tabList as PlayerTabOverlayAccessor).`galapagos$getPlayerInfos`()

        val players: MutableList<PlayerState> = mutableListOf()
        val playerStates: HashMap<String, BattleBoxPlayerState> = hashMapOf()

        tabListTeamIndexes.forEach { (teamIndex, playerIndexes) ->
            val teamName = Regex("(?<team>[a-zA-Z]+) Team").find(tabList.getOrNull(teamIndex)?.tabListDisplayName?.string ?: "")
                ?.groups?.get("team")?.value ?: return@forEach

            playerIndexes.forEach {
                val usernameIndex = it.first
                val statsIndex = it.second

                val playerName = Regex("(?<username>[a-zA-Z0-9_]+)").find(tabList[usernameIndex].tabListDisplayName?.string ?: "")
                    ?.groups?.get("username")?.value ?: return@forEach

                val statsMatch = Regex("(?<kills>\\d+).+?(?<deaths>\\d+).+?(?<assists>\\d+).+?(?<score>\\d+)").find(tabList[statsIndex].tabListDisplayName?.string ?: "")
                    ?.groups ?: return@forEach

                val kills = statsMatch["kills"]?.value?.toIntOrNull() ?: return@forEach
                val deaths = statsMatch["deaths"]?.value?.toIntOrNull() ?: return@forEach
                val assists = statsMatch["assists"]?.value?.toIntOrNull() ?: return@forEach
                val score = statsMatch["score"]?.value?.toIntOrNull() ?: return@forEach

                players.add(PlayerState(playerName, score))
                playerStates[playerName] = BattleBoxPlayerState(kills, deaths, assists, teamName)
            }
        }

        state.players = players
        state.playerStates = playerStates

        val roundsMatch = findScoreboardLines(Regex("\\[.]"))

        if (roundsMatch != null) {
            val rounds: MutableList<BattleBoxRound> = mutableListOf()

            roundsMatch.forEach {
                Galapagos.logger.info("bb round says ${it.value}")
                val letter = it.value[1]
                val round = BattleBoxRound.entries.find { r -> r.scoreboardLetter == letter } ?: return@forEach
                rounds.add(round)
            }

            state.rounds = rounds
        }
    }

    fun updateBattleBoxState(packet: ClientboundSystemChatPacket) {
        val state: GameState.BattleBox = currentState as GameState.BattleBox
        val message = packet.content.string

        if (message.contains("You have chosen the")) {
            val kitName = Regex("You have chosen the (?<kit>[a-zA-Z]+) kit").find(message)
                ?.groups?.get("kit")?.value ?: return

            val kit = BattleBoxKit.entries.find { it.label == kitName } ?: return

            state.kit = kit
        }

        updateState = true
    }
}