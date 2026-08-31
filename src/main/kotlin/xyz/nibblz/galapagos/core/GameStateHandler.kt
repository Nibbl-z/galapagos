package xyz.nibblz.galapagos.core

import com.noxcrew.noxesium.core.mcc.ClientboundMccGameStatePacket
import com.noxcrew.noxesium.core.mcc.ClientboundMccServerPacket
import com.noxcrew.noxesium.core.mcc.ClientboundMccStatisticPacket
import kotlinx.serialization.Serializable
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.data.XP_TABLE
import xyz.nibblz.galapagos.data.game.*
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
            var killCauses: MutableList<DeathCause> = mutableListOf()
            var deathCauses: MutableList<DeathCause> = mutableListOf()

            override fun projectedXP(): Int {
                val player = players.firstOrNull { it.username == Minecraft.getInstance().user.name } ?: return 0

                val eliminations = player.kills + player.assists
                val roundsWon = rounds.sumOf { if (it == BattleBoxRound.WIN) 1 else 0 }
                val roundsPlayed = rounds.size

                return (eliminations * 10 + roundsPlayed * 30 + roundsWon * 45)
            }

            companion object {
                fun teamPlacement(game: BattleBox): Int {
                    val teamScores: HashMap<String, Int> = hashMapOf()
                    var team = ""

                    game.players.forEach {
                        teamScores[it.team] = (teamScores[it.team] ?: 0) + it.score
                        if (it.username == Minecraft.getInstance().user.name) team = it.team
                    }

                    val sorted = teamScores.entries.sortedByDescending { it.value }
                    return sorted.indexOfFirst { it.key == team } + 1
                }
            }
        }

        /*

        HATE. LET ME TELL YOU HOW MUCH I'VE COME TO HATE BATTLE BOX ARENA SINCE I BEGAN TO LIVE.
        THERE ARE 79.3 THOUSAND MILES OF VEINS AND ARTERIES IN WAFER THIN LAYERS THAT FILL MY BODY.
        IF THE WORD HATE WAS ENGRAVED ON EACH NANOANGSTROM OF THOSE TENS OF THOUSANDS OF MILES
        IT WOULD NOT EQUAL ONE ONE-BILLIONTH OF THE HATE I FEEL FOR BATTLE BOX ARENA AT THIS MICRO-INSTANT.
        HATE. HATE.

        */

        @Serializable
        class BattleBoxArena(override var players: List<BattleBoxPlayerState>) : GameState<BattleBoxPlayerState>() {
            var playerStates: HashMap<String, BattleBoxPlayerState> = hashMapOf()
            var map: String = "Unknown"
            var kits: MutableList<BattleBoxArenaKitChoice> = mutableListOf()
            var rounds: MutableList<BattleBoxRound> = mutableListOf()
            var roundStats: MutableList<BattleBoxRoundStats> = mutableListOf()
            var killCauses: MutableList<DeathCause> = mutableListOf()
            var deathCauses: MutableList<DeathCause> = mutableListOf()
            var rankPoints: Int = 0

            override fun projectedXP(): Int {
                val eliminations = (playerStates[Minecraft.getInstance().user.name]?.kills ?: 0) + (playerStates[Minecraft.getInstance().user.name]?.assists ?: 0)
                val roundsWon = rounds.sumOf { if (it == BattleBoxRound.WIN) 1 else 0 }
                val roundsPlayed = rounds.size

                return (eliminations * 10 + roundsPlayed * 30 + roundsWon * 45)
            }

            companion object {
                fun getScores(game: BattleBoxArena): Pair<Int, Int> = game.rounds.count { it == BattleBoxRound.WIN } to game.rounds.count { it == BattleBoxRound.LOSS }
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
        class ParkourWarriorSurvivor(override var players: List<BasicPlayerState> = listOf()) : GameState<BasicPlayerState>() {
            var leapPlacements: MutableList<Int> = mutableListOf()
            var leapCompletionTimes: MutableList<Int> = mutableListOf()
            var obstaclesCompleted: Int = 0
            var playersEliminated: Int = 0

            override fun projectedXP(): Int {
                /* todo: none of this is right, the wiki is out of date, i dont knowwwwww

                what ive gotten from testing is that for leap champions its actually
                1: 30
                2: 50
                3: 60
                4: 70
                5: 80
                6: 90
                7: 105 ??
                 */


                val leapReachedXP = XP_TABLE[XPInfo.XPSource.PW_SURVIVAL]!!.customStatisticTables["leap_reached"]!!.getHighest(leapPlacements.size + 2)

                return leapReachedXP + leapChampions() * 20
            }

            fun leapChampions() = leapPlacements.count { it == 1 } - if (leapPlacements.getOrNull(7) == 1) 1 else 0
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

        fun getPlayer(): T? {
            return players.find { it.username == Minecraft.getInstance().user.name }
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
    data class BattleBoxRoundStats(
        var kills: Int,
        var deaths: Int,
        var assists: Int,
        var score: Int
    )

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

                try {
                    currentGame?.stateHandler?.update()
                } catch (e: Exception) {
                    Galapagos.logger.warn("Encountered exception in ${currentGame?.name} generic update handling: ${e.message} ${e.cause}")
                }
            }
        }
    }

    fun saveState() {
        if (currentState != null) {
            when(currentState!!::class) {
                GameState.BattleBox::class -> Galapagos.save.battleBoxHistory.add(currentState as GameState.BattleBox)
                GameState.BattleBoxArena::class -> Galapagos.save.battleBoxArenaHistory.add(currentState as GameState.BattleBoxArena)
                GameState.HoleInTheWall::class -> Galapagos.save.hitwHistory.add(currentState as GameState.HoleInTheWall)
                GameState.SkyBattleSolo::class -> Galapagos.save.skyBattleSoloHistory.add(currentState as GameState.SkyBattleSolo)
                GameState.ParkourWarriorSurvivor::class -> Galapagos.save.parkourWarriorSurvivorHistory.add(currentState as GameState.ParkourWarriorSurvivor)
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

        if (currentGame == null) return

        currentState = when(currentGame) {
            XPInfo.XPSource.BATTLE_BOX_QUADS -> GameState.BattleBox(listOf())
            XPInfo.XPSource.BATTLE_BOX_ARENA -> GameState.BattleBoxArena(listOf())
            XPInfo.XPSource.HOLE_IN_THE_WALL -> GameState.HoleInTheWall(listOf())
            XPInfo.XPSource.SKY_BATTLE_SOLOS -> GameState.SkyBattleSolo(listOf())
            XPInfo.XPSource.PW_SURVIVAL -> GameState.ParkourWarriorSurvivor(listOf())
            else -> return
        }

        currentState!!.timestamp = Clock.System.now().epochSeconds
        currentGame?.stateHandler?.init()
    }

    fun mccGameState(packet: ClientboundMccGameStatePacket) {
        try {
            currentGame?.stateHandler?.handleGameStatePacket(packet)
        } catch (e: Exception) {
            Galapagos.logger.warn("Encountered exception in ${currentGame?.name} MccGameState handling: ${e.message} ${e.cause}")
        }
        updateState = true
    }

    fun mccStatistic(packet: ClientboundMccStatisticPacket) {
        try {
            currentGame?.stateHandler?.handleStatisticPacket(packet)
        } catch (e: Exception) {
            Galapagos.logger.warn("Encountered exception in ${currentGame?.name} MccStatistic handling: ${e.message} ${e.cause}")
        }
        updateState = true
    }

    fun systemChat(packet: ClientboundSystemChatPacket) {
        try {
            currentGame?.stateHandler?.handleSystemChatPacket(packet)
        } catch (e: Exception) {
            Galapagos.logger.warn("Encountered exception in ${currentGame?.name} SystemChat handling: ${e.message} ${e.cause}")
        }
        updateState = true
    }
}