package xyz.nibblz.galapagos.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import xyz.nibblz.galapagos.Galapagos

object ConstantIslandData {
    @Serializable
    data class CrateCosmetic(
        val name: String,
        val chance: Double
    )

    @Serializable
    data class Data(
        var crateEmporium: HashMap<String, List<CrateCosmetic>> = hashMapOf()
    )

    val data: Data = Data()

    fun load() {
        val crateEmporiumJson = this::class.java.getResourceAsStream("/crate_emporium.json")?.bufferedReader().use { it?.readText() }
            ?: throw NullPointerException("Failed to load crate emporium data")

        data.crateEmporium = Json.decodeFromString(crateEmporiumJson)
    }
}

