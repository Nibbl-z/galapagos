package xyz.nibblz.galapagos.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object ConstantIslandData {
    @Serializable
    data class CrateCosmetic(
        val name: String,
        val chance: Double
    )

    @Serializable
    data class Data(
        var crateEmporium: HashMap<String, List<CrateCosmetic>> = hashMapOf(),
        var badgeSprites: HashMap<String, String> = hashMapOf(),
        var fishSprites: HashMap<String, String> = hashMapOf()
    )

    val data: Data = Data()

    fun load() {
        val crateEmporiumJson = this::class.java.getResourceAsStream("/crate_emporium.json")?.bufferedReader().use { it?.readText() }
            ?: throw NullPointerException("Failed to load crate emporium data")

        data.crateEmporium = Json.decodeFromString(crateEmporiumJson)

        val badgeSpritesJson = this::class.java.getResourceAsStream("/badge_sprites.json")?.bufferedReader().use { it?.readText() }
            ?: throw NullPointerException("Failed to load badge sprite data")

        data.badgeSprites = Json.decodeFromString(badgeSpritesJson)

        val fishSpritesJson = this::class.java.getResourceAsStream("/fish_sprites.json")?.bufferedReader().use { it?.readText() }
            ?: throw NullPointerException("Failed to load fish sprite data")

        data.fishSprites = Json.decodeFromString(fishSpritesJson)
    }
}

