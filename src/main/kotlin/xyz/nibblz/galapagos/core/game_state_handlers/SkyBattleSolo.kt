package xyz.nibblz.galapagos.core.game_state_handlers

import com.noxcrew.noxesium.core.mcc.ClientboundMccGameStatePacket
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import xyz.nibblz.galapagos.core.GameStateHandler
import xyz.nibblz.galapagos.core.GameStateHandler.GameState
import xyz.nibblz.galapagos.core.GameStateHandler.currentState
import xyz.nibblz.galapagos.core.GameStateHandler.usernameRegex
import xyz.nibblz.galapagos.data.game.DeathCause
import xyz.nibblz.galapagos.mixin.PlayerTabOverlayAccessor
import xyz.nibblz.galapagos.util.getBossbarLines
import kotlin.math.max

object SkyBattleSolo : Handler {
    val playerRegex = Regex("(?:(?<placement>\\d+)\\D+)?(?<kills>\\d)\\D*$")
    val timerRegex = Regex("0(?<min>\\d):(?<sec>\\d\\d)")
    var dead = false

    override fun init() {
        dead = false
    }

    override fun handleGameStatePacket(packet: ClientboundMccGameStatePacket) {
        val state: GameState.SkyBattleSolo = currentState as GameState.SkyBattleSolo
        if (state.map != "Unknown") return
        state.map = packet.mapName
    }

    override fun handleSystemChatPacket(packet: ClientboundSystemChatPacket) {
        val state: GameState.SkyBattleSolo = currentState as GameState.SkyBattleSolo
        val message = packet.content.string

        if (message.contains("you were eliminated")) {
            dead = true
            return
        }

        if (!message.contains(Minecraft.getInstance().user.name, true)) return
        val cause = DeathCause.entries.find { it.messages.any { phrase -> message.contains(phrase, true) } } ?: DeathCause.UNKNOWN
        val isDeath = !message.contains("[+") // when you die, there is no score gain. sooo...

        if (isDeath) {
            state.deathCause = cause
        } else {
            state.killCauses.add(cause)
        }
    }

    override fun update() {
        val state: GameState.SkyBattleSolo = currentState as GameState.SkyBattleSolo

        val tabListPlayerIndexes: HashMap<Int, Int> = hashMapOf(
            1 to 21,
            2 to 22,
            3 to 23,
            4 to 24,
            41 to 61,
            42 to 62,
            43 to 63,
            44 to 64
        )

        val tabList = (Minecraft.getInstance().gui.tabList as PlayerTabOverlayAccessor).`galapagos$getPlayerInfos`()
        val players: MutableList<GameStateHandler.SkyBattleSoloPlayerState> = mutableListOf()
        var alivePlayers = 0

        tabListPlayerIndexes.forEach { (usernameIndex, statsIndex) ->
            val playerName = usernameRegex.find(tabList.getOrNull(usernameIndex)?.tabListDisplayName?.string ?: "")
                ?.groups?.get("username")?.value ?: return@forEach

            val statMatch = playerRegex.find(tabList.getOrNull(statsIndex)?.tabListDisplayName?.string ?: "")?.groups ?: return@forEach
            val placement = statMatch["placement"]?.value?.toIntOrNull() ?: -1
            if (placement == -1) alivePlayers++
            val kills = statMatch["kills"]?.value?.toIntOrNull() ?: 0

            players.add(GameStateHandler.SkyBattleSoloPlayerState(playerName, placement, kills, placement))
        }

        players.forEach {
            if (it.placement == -1) it.placement = alivePlayers
        }

        state.players = players

        if (dead) return

        val timeMatch = getBossbarLines().firstNotNullOfOrNull {
            if (it.contains("STARTING IN")) return@firstNotNullOfOrNull null
            if (it.contains("HUB")) return@firstNotNullOfOrNull null
            timerRegex.find(it)?.groups
        }

        val minutes = timeMatch?.get("min")?.value?.toIntOrNull() ?: 5
        val seconds = timeMatch?.get("sec")?.value?.toIntOrNull() ?: 0

        state.timeSurvived = max(state.timeSurvived, 300 - (minutes * 60 + seconds))
    }
}