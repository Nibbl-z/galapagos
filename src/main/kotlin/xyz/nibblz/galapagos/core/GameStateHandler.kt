package xyz.nibblz.galapagos.core

import com.noxcrew.noxesium.core.mcc.ClientboundMccGameStatePacket
import com.noxcrew.noxesium.core.mcc.ClientboundMccServerPacket
import com.noxcrew.noxesium.core.mcc.ClientboundMccStatisticPacket
import kotlinx.serialization.Serializable
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.data.game.BattleBoxArenaCoreKits
import xyz.nibblz.galapagos.data.game.BattleBoxKit
import xyz.nibblz.galapagos.data.game.BattleBoxRound
import xyz.nibblz.galapagos.data.game.WallType
import xyz.nibblz.galapagos.data.XP_TABLE
import xyz.nibblz.galapagos.data.game.DeathCause
import xyz.nibblz.galapagos.events.MCCGameStateEvent
import xyz.nibblz.galapagos.events.MCCServerEvent
import xyz.nibblz.galapagos.events.MCCStatisticEvent
import xyz.nibblz.galapagos.events.SystemChatEvent
import xyz.nibblz.galapagos.features.XPInfo
import xyz.nibblz.galapagos.util.getHighest
import xyz.nibblz.galapagos.util.getLowest
import kotlin.math.max
import kotlin.time.Clock

object GameStateHandler : CoreFeature {
    // shoutout to devcmb
    // tonight im gonna fall asleep to kotlin documentation
    @Serializable
    sealed class GameState<T : PlayerState> {
        abstract var players: List<T>
        var timestamp: Long = 0

        @Serializable
        class BattleBox(override var players: List<BattleBoxPlayerState> = listOf()) : GameState<BattleBoxPlayerState>() {
            var map: String = "Unknown"
            var kit: BattleBoxKit? = null
            var rounds: MutableList<BattleBoxRound> = mutableListOf()

            override fun projectedXP(): Int {
                val player = players.firstOrNull { it.username == Minecraft.getInstance().user.name } ?: return 0

                val eliminations = player.kills + player.assists
                val roundsWon = rounds.sumOf { if (it == BattleBoxRound.WIN) 1 else 0 }
                val roundsPlayed = rounds.size

                return (eliminations * 10 + roundsPlayed * 30 + roundsWon * 45)
            }
        }

        class BattleBoxArena(override var players: List<BattleBoxPlayerState>) : GameState<BattleBoxPlayerState>() {
            var playerStates: HashMap<String, BattleBoxPlayerState> = hashMapOf()
            var map: String = "Unknown"
            var kits: MutableList<Pair<BattleBoxKit, BattleBoxArenaCoreKits>> = mutableListOf()
            var rounds: MutableList<BattleBoxRound> = mutableListOf()

            override fun projectedXP(): Int {
                val eliminations = playerStates[Minecraft.getInstance().user.name]!!.kills + playerStates[Minecraft.getInstance().user.name]!!.assists
                val roundsWon = rounds.sumOf { if (it == BattleBoxRound.WIN) 1 else 0 }
                val roundsPlayed = rounds.size

                return (eliminations * 10 + roundsPlayed * 30 + roundsWon * 45)
            }
        }

        @Serializable
        class HoleInTheWall(override var players: List<BasicPlayerState> = listOf()) : GameState<BasicPlayerState>() {
            var map: String = "Unknown"
            var wallTypes: MutableList<WallType> = mutableListOf()
            var timeSurvived: Int = 0
            var wallsSurvived: Int = 0

            override fun projectedXP(): Int {
                val wallsSurvivedXP = XP_TABLE[XPInfo.XPSource.HOLE_IN_THE_WALL]!!.customStatisticTables["walls_survived"]!!.getHighest(wallsSurvived)
                val placementXP = XP_TABLE[XPInfo.XPSource.HOLE_IN_THE_WALL]!!.customStatisticTables["placement"]!!.getLowest(getPlacement(true))

                return wallsSurvivedXP + placementXP
            }
        }

        @Serializable
        class SkyBattleSolo(override var players: List<SkyBattleSoloPlayerState> = listOf()) : GameState<SkyBattleSoloPlayerState>() {
            var map: String = "Unknown"
            var timeSurvived: Int = 0
            var deathCause: DeathCause = DeathCause.UNKNOWN
            var killCauses: MutableList<DeathCause> = mutableListOf()

            override fun projectedXP(): Int {
                val killXP = kills() * 15
                val placementXP = XP_TABLE[XPInfo.XPSource.SKY_BATTLE_SOLOS]!!.customStatisticTables["placement"]!!.getLowest(getPlacement())
                val survivalXP = XP_TABLE[XPInfo.XPSource.SKY_BATTLE_SOLOS]!!.customStatisticTables["survival"]!!.getHighest(timeSurvived)

                return killXP + max(placementXP, survivalXP)
            }

            fun kills() = players.find { it.username == Minecraft.getInstance().user.name }?.kills ?: 0
            fun getPlacement() = players.find { it.username == Minecraft.getInstance().user.name }?.placement ?: 8
        }

        fun getScore(): Int {
            return players.find { it.username == Minecraft.getInstance().user.name }?.score ?: -1
        }

        fun getPlacement(incrementTies: Boolean): Int {
            var placement = 1
            val score = getScore()

            players.filter { it.username != Minecraft.getInstance().user.name }.forEach {
                if (incrementTies) {
                    if (it.score >= score) placement++
                } else {
                    if (it.score > score) placement++
                }
            }

            return placement
        }

        abstract fun projectedXP(): Int
    }

    interface PlayerState {
        var username: String
        var score: Int
    }

    @Serializable
    data class BasicPlayerState(
        override var username: String,
        override var score: Int
    ) : PlayerState

    @Serializable
    data class BattleBoxPlayerState(
        override var username: String,
        override var score: Int,
        var kills: Int,
        var deaths: Int,
        var assists: Int,
        val team: String,
    ) : PlayerState

    @Serializable
    data class SkyBattleSoloPlayerState(
        override var username: String,
        override var score: Int,
        var kills: Int,
        var placement: Int
    ) : PlayerState

    var currentGame: XPInfo.XPSource? = null
    var currentState: GameState<out PlayerState>? = null
    var updateState = false
    var updateDelay = 5

    val usernameRegex = Regex("(?<username>[a-zA-Z0-9_]+)")

    override fun init() {
        MCCServerEvent.EVENT.register { packet -> mccServer(packet) }
        MCCGameStateEvent.EVENT.register { packet -> mccGameState(packet) }
        MCCStatisticEvent.EVENT.register { packet -> mccStatistic(packet) }
        SystemChatEvent.EVENT.register { packet -> systemChat(packet) }
        ClientTickEvents.END_CLIENT_TICK.register {
            if (!updateState) return@register
            updateDelay--

            if (updateDelay == 0) {
                updateDelay = 5
                updateState = false

                currentGame?.stateHandler?.update()
            }
        }
    }

    fun saveState() {
        if (currentState != null) {
            when(currentState!!::class) {
                GameState.BattleBox::class -> Galapagos.save.battleBoxHistory.add(currentState as GameState.BattleBox)
                GameState.HoleInTheWall::class -> Galapagos.save.hitwHistory.add(currentState as GameState.HoleInTheWall)
                GameState.SkyBattleSolo::class -> Galapagos.save.skyBattleSoloHistory.add(currentState as GameState.SkyBattleSolo)
            }
        }
    }

    fun mccServer(packet: ClientboundMccServerPacket) {
        saveState()
        currentState = null

        currentGame = if (packet.server != "game") null
        else XPInfo.XPSource.entries.firstOrNull {
            it.serverTypes.all { type ->
                if (type.startsWith("!")) !packet.types.contains(type.removePrefix("!")) else packet.types.contains(type)
            }
        }

        Galapagos.logger.info("$currentGame")

        if (currentGame == null) return

        currentState = when(currentGame) {
            XPInfo.XPSource.BATTLE_BOX_QUADS -> GameState.BattleBox(listOf())
            XPInfo.XPSource.HOLE_IN_THE_WALL -> GameState.HoleInTheWall(listOf())
            XPInfo.XPSource.SKY_BATTLE_SOLOS -> GameState.SkyBattleSolo(listOf())
            else -> return
        }

        currentState!!.timestamp = Clock.System.now().epochSeconds
        currentGame?.stateHandler?.init()
    }

    fun mccGameState(packet: ClientboundMccGameStatePacket) {
        currentGame?.stateHandler?.handleGameStatePacket(packet)
        updateState = true
    }

    fun mccStatistic(packet: ClientboundMccStatisticPacket) {
        currentGame?.stateHandler?.handleStatisticPacket(packet)
        updateState = true
    }

    fun systemChat(packet: ClientboundSystemChatPacket) {
        currentGame?.stateHandler?.handleSystemChatPacket(packet)
        updateState = true
    }
}