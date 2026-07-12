package xyz.nibblz.galapagos.util

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.data.Cosmetic
import xyz.nibblz.galapagos.data.Item
import xyz.nibblz.galapagos.features.CoinTracking
import xyz.nibblz.galapagos.features.QuestTracking
import java.nio.file.Files

@Serializable
data class PlayerSave(
    var coinChanges: MutableList<CoinTracking.CoinChange> = mutableListOf(),
    var questHistory: MutableList<QuestTracking.QuestingReward> = mutableListOf(),
    var cosmetics: HashMap<String, Cosmetic> = hashMapOf(),
    var infinibag: HashMap<String, Item> = hashMapOf(),
    var infinivault: HashMap<String, Item> = hashMapOf(),
    var fusionForge: MutableList<Item> = mutableListOf(),
    var stylePerks: HashMap<PlayerData.StylePerk, Int> = hashMapOf(),
    var apiKey: String = ""
)

object Save {
    private val path = FabricLoader.getInstance().configDir.resolve(Galapagos.MOD_ID).resolve("save.json")
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    fun load() {
        if (!Files.exists(path)) return

        val jsonText = Files.readString(path) ?: return
        val loaded = json.decodeFromString<PlayerSave>(jsonText)
        Galapagos.save = loaded

        PlayerData.StylePerk.entries.forEach {
            if (Galapagos.save.stylePerks[it] == null) Galapagos.save.stylePerks[it] = 0
        }
    }

    fun save() {
        val saved = json.encodeToString(Galapagos.save)
        Galapagos.logger.info(saved)
        Galapagos.logger.info(path.toString())

        Files.createDirectories(path.parent)
        Files.writeString(path, saved)
    }
}