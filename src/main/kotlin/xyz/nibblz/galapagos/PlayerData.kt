package xyz.nibblz.galapagos

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import net.minecraft.client.Minecraft
import xyz.nibblz.galapagos.data.Rarity
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse


object PlayerData {
    @Serializable
    enum class CosmeticTag {
        STANDARD,
        EXCLUSIVE,
        ARCANE
    }

    @Serializable
    enum class Collection(val label: String, val tag: CosmeticTag?) {
        ELEMENTAL("Elemental", CosmeticTag.STANDARD),
        STANDARD_GAME("Standard Game", CosmeticTag.STANDARD),
        EXCLUSIVE_GAME("Exclusive Game", CosmeticTag.EXCLUSIVE),
        EXCLUSIVE_SEASON("Exclusive Season", CosmeticTag.EXCLUSIVE),
        EXCLUSIVE_VARIETY("Exclusive Variety", CosmeticTag.EXCLUSIVE),
        GATE("Gate", null), // this will have to be determined via the obtainmentHint because. Theres standards AND arcanes...
    } // rn only including ones that are obtained from odds (cosmetic machine or crates)

    @Serializable
    data class Cosmetic(
        val name: String,
        val collection: Collection,
        val tag: CosmeticTag,
        val isOwned: Boolean,
        val donations: Int,
        val rarity: Rarity,
        val isColorable: Boolean,
        val isColored: Boolean
    )

    fun Cosmetic.getRep(): Int {
        return this.donations * when(this.tag) {
            CosmeticTag.STANDARD -> this.rarity.trophies / 10
            CosmeticTag.EXCLUSIVE -> this.rarity.trophies / 5
            CosmeticTag.ARCANE -> 30
        }
    }

    @Serializable
    data class APICosmeticData(
        val trophies: Int,
        val name: String,
        val collection: String,
        val obtainmentHint: String
    )

    @Serializable
    data class APICosmetic(
        val cosmetic: APICosmeticData,
        val chromaPacks: List<String>? = null,
        val owned: Boolean,
        val donationsMade: Int? = null
    )

    val client: HttpClient? = HttpClient.newHttpClient()
    
    fun fetchAPI() {
        if (Galapagos.save.apiKey.isEmpty()) {
            Galapagos.logger.warn("No API key provided. Some functions of Galapagos will not function!")
            return
        }

        val graphQL = """
            query fetchPlayerData {
              player(uuid: \"${Minecraft.getInstance().gameProfile.id}\") {
                collections {
                  cosmetics {
                    cosmetic {
                      trophies
                      name
                      collection
                      obtainmentHint
                    }
                    chromaPacks
                    owned
                    donationsMade
                  }
                }
              }
            }
        """.trimIndent().replace("\n", "\\n")

        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.mccisland.net/graphql"))
            .POST(HttpRequest.BodyPublishers.ofString("{\"query\" : \"$graphQL\"}"))
            .header("Accept", "application/json")
            .header("content-type", "application/json")
            .header("X-API-Key", Galapagos.save.apiKey)
            .header("User-Agent", "galapagos-mc-mod/${Minecraft.getInstance().gameProfile.id}")
            .build()

        val response = client?.send(request, HttpResponse.BodyHandlers.ofString()) ?: return
        val jsonElement = Json.parseToJsonElement(response.body()).jsonObject

        val apiCosmeticsString = jsonElement["data"]?.jsonObject["player"]?.jsonObject["collections"]?.jsonObject["cosmetics"]?.jsonArray.toString()
        val apiCosmetics: List<APICosmetic> = Json.decodeFromString(apiCosmeticsString)

        apiCosmetics.forEach {
            val collection = Collection.entries.find { entry -> entry.label == it.cosmetic.collection } ?: return@forEach
            val tag = if (collection != Collection.GATE) collection.tag else {
               if (it.cosmetic.obtainmentHint.contains("Arcane Gate")) CosmeticTag.ARCANE else CosmeticTag.STANDARD
            } ?: throw IllegalStateException("Could not determine cosmetic tag for ${it.cosmetic.name}")

            val cosmetic = Cosmetic(
                name = it.cosmetic.name,
                collection = collection,
                tag = tag,
                isOwned = it.owned,
                donations = it.donationsMade ?: 0,
                rarity = Rarity.entries.find { entry -> entry.trophies == it.cosmetic.trophies } ?: Rarity.COMMON,
                isColorable = it.chromaPacks != null,
                isColored = it.chromaPacks?.size == 4
            )

            Galapagos.logger.info(cosmetic.toString())

            Galapagos.save.cosmetics[it.cosmetic.name] = cosmetic
        }
    }
}