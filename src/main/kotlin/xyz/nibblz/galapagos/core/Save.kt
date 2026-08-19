package xyz.nibblz.galapagos.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.data.*
import xyz.nibblz.galapagos.features.CoinTracking
import xyz.nibblz.galapagos.features.QuestTracking
import xyz.nibblz.galapagos.features.TrophyTracking
import xyz.nibblz.galapagos.features.XPInfo
import java.nio.file.Files

object Save : CoreFeature {
    private val path = FabricLoader.getInstance().configDir.resolve(Galapagos.MOD_ID).resolve("save.json")
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Serializable
    data class PlayerSave(
        var trophyHistory: MutableList<TrophyTracking.TrophyGain> = mutableListOf(),
        var coinChanges: MutableList<CoinTracking.CoinChange> = mutableListOf(),
        var questHistory: MutableList<QuestTracking.QuestingReward> = mutableListOf(),
        var weeklyVaultHistory: MutableList<QuestTracking.WeeklyVault> = mutableListOf(),

        var cosmetics: HashMap<String, Cosmetic> = hashMapOf(),
        var infinibag: HashMap<String, Item> = hashMapOf(),
        var infinivault: HashMap<String, Item> = hashMapOf(),
        var fusionForge: MutableList<Item> = mutableListOf(),
        var blueprintAssembler: MutableList<Item> = mutableListOf(),
        var stylePerks: HashMap<StylePerk, Int> = hashMapOf(),

        var finishedOOBE: Boolean = false,
        var mccPlus: Boolean = false,
        var rank: Rank? = null,
        var selectedFaction: Faction? = null,
        var factionXP: HashMap<Faction, Int> = hashMapOf(),
        var apiKey: String = "",
        var starLevelXP: HashMap<StarLevelGame, Int> = hashMapOf(),
        var gameXP: HashMap<XPInfo.XPSource, Int> = hashMapOf(),
        var gamesPlayed: HashMap<XPInfo.XPSource, Int> = hashMapOf(),

        var battleBoxHistory: MutableList<GameStateHandler.GameState.BattleBox> = mutableListOf(),
        var hitwHistory: MutableList<GameStateHandler.GameState.HoleInTheWall> = mutableListOf(),
        var skyBattleSoloHistory: MutableList<GameStateHandler.GameState.SkyBattleSolo> = mutableListOf(),
        var parkourWarriorSurvivorHistory: MutableList<GameStateHandler.GameState.ParkourWarriorSurvivor> = mutableListOf(),
        var battleBoxArenaHistory: MutableList<GameStateHandler.GameState.BattleBoxArena> = mutableListOf(),

        var xpGains: MutableList<XPInfo.XPGain> = mutableListOf()
    )

    override fun init() {
        load()
    }

    fun load() {
        if (Files.exists(path)) {
            val jsonText = Files.readString(path) ?: return
            val loaded = json.decodeFromString<PlayerSave>(jsonText)
            Galapagos.save = loaded
        }

        StylePerk.entries.forEach {
            if (Galapagos.save.stylePerks[it] == null) Galapagos.save.stylePerks[it] = 0
        }
    }

    fun save() {
        val saved = json.encodeToString(Galapagos.save)

        Files.createDirectories(path.parent)
        Files.writeString(path, saved)
    }
}