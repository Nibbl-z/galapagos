package xyz.nibblz.islandeconomist

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files

@Serializable
data class CoinChange(
    var amount: Int = 0,
    var timestamp: Long = 0,
    var source: String = "Unknown"
)

@Serializable
data class PlayerSave(
    var coinChanges: MutableList<CoinChange> = mutableListOf()
)

object Save {
    private val path = FabricLoader.getInstance().configDir.resolve("island-economist").resolve("save.json")
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
    fun load() {
        if (!Files.exists(path)) return

        val jsonText = Files.readString(path) ?: return
        val loaded = json.decodeFromString<PlayerSave>(jsonText)
        IslandEconomist.save = loaded
    }

    fun save() {
        val saved = json.encodeToString(IslandEconomist.save)
        IslandEconomist.logger.info(saved)
        IslandEconomist.logger.info(path.toString())

        Files.createDirectories(path.parent)
        Files.writeString(path, saved)
    }
}