package xyz.nibblz.galapagos

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import xyz.nibblz.galapagos.data.Rarity
import xyz.nibblz.galapagos.events.ContainerOpenEvent
import xyz.nibblz.galapagos.events.ContainerSetSlotEvent
import xyz.nibblz.galapagos.events.InfinibagUpdateEvent
import xyz.nibblz.galapagos.events.SlotClickEvent
import xyz.nibblz.galapagos.events.SystemChatEvent
import xyz.nibblz.galapagos.features.CraftingInstructions.fetchCraftingMaterials
import xyz.nibblz.galapagos.mixin.accessor.HoveredSlotAccessor
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse


object PlayerData {
    //// COSMETICS

    @Serializable
    enum class CosmeticTag(val maxDonations: Int) {
        STANDARD(10),
        EXCLUSIVE(5),
        ARCANE(5)
    }

    @Serializable
    enum class Collection(val label: String) {
        ELEMENTAL("Elemental"),
        STANDARD_GAME("Standard Game"),
        EXCLUSIVE_GAME("Exclusive Game"),
        EXCLUSIVE_SEASON("Exclusive Season"),
        EXCLUSIVE_VARIETY("Exclusive Variety"),
        GATE("Gate"),
        FISHING("Fishing")
    }

    @Serializable
    data class Cosmetic(
        val name: String,
        val collection: Collection,
        val tag: CosmeticTag,
        var isOwned: Boolean,
        var donations: Int,
        val rarity: Rarity,
        val isColorable: Boolean,
        var isColored: Boolean
    )

    fun Cosmetic.getRep(): Int {
        return this.donations * this.repPerDonation()
    }

    fun Cosmetic.repPerDonation(): Int {
        return when(this.tag) {
            CosmeticTag.STANDARD -> this.rarity.trophies / 10
            CosmeticTag.EXCLUSIVE -> this.rarity.trophies / 5
            CosmeticTag.ARCANE -> 30
        }
    }

    fun Cosmetic.bonusCoresPerScavenge(): Double {
        return when(this.tag) {
            CosmeticTag.STANDARD -> when(this.rarity) {
                Rarity.COMMON -> 0.03
                Rarity.UNCOMMON -> 0.1
                Rarity.RARE -> 0.25
                Rarity.EPIC -> 0.5
                Rarity.LEGENDARY -> 1.0
                Rarity.MYTHIC -> 2.0
            } * if (this.donations == 10) 2.0 else 1.0
            CosmeticTag.EXCLUSIVE -> when(this.rarity) {
                Rarity.RARE -> 0.06
                Rarity.EPIC -> 0.15
                Rarity.LEGENDARY -> 0.30
                Rarity.MYTHIC -> 1.0
                else -> 0.0
            } * if (this.donations == 5) 2.0 else 1.0
            CosmeticTag.ARCANE -> 1.0 * if (this.donations == 5) 2.0 else 1.0
        }
    }

    @Serializable
    data class APICosmeticData(
        val trophies: Int,
        val name: String,
        val collection: String,
        val type: String
    )

    @Serializable
    data class APICosmetic(
        val cosmetic: APICosmeticData,
        val chromaPacks: List<String>? = null,
        val owned: Boolean,
        val donationsMade: Int? = null
    )

    //// ITEMS/INFINIBAG

    enum class ItemLocation {
        INFINIBAG,
        INFINIVAULT
    }

    @Serializable
    data class Item(
        val name: String,
        var count: Int,
        val isCosmeticToken: Boolean
    ) : Cloneable {
        public override fun clone(): Item {
            return super.clone() as Item
        }
    }

    @Serializable
    data class APIItem(
        val amount: Int,
        val asset: APIItemAsset
    )

    @Serializable
    data class APIItemAsset(
        val name: String,
        val __typename: String? = null
    )

    //// STYLE PERKS

    @Serializable
    enum class StylePerk(val label: String, val slotID: Int) {
        LUCKY_METER("Lucky Meter", 11),
        GLITCHED_CLAIMS("Glitched Claims", 12),
        EXPANDED_METER("Expanded Meter", 13),
        EXPANDED_VAULT("Expanded Vault", 14),
        ARCANE_CLAIMS("Arcane Claims", 15),
        LUCKY_QUESTS("Lucky Quests", 20),
        BOOSTED_QUESTS("Boosted Quests", 21),
        EXPANDED_DAILIES("Expanded Dailies", 22),
        EXPANDED_WEEKLIES("Expanded Weeklies", 23),
        ARCANE_QUESTS("Arcane Quests", 24),
        EFFICIENT_FUSION("Efficient Fusion", 29),
        EFFICIENT_ASSEMBLY("Efficient Assembly", 30),
        EXPANDED_FORGE("Expanded Forge", 31),
        EXPANDED_ASSEMBLER("Expanded Assembler", 32),
        ARCANE_ANOMALY("Arcane Anomaly", 33) // I love this perk! this perk is cool.this is my favorite  perk. :3  ilove.arcane   anomaly! anomalyyy:3
    }

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
                      type
                    }
                    chromaPacks
                    owned
                    donationsMade
                  }
                }
                infinibag {
                  amount
                  asset {
                    name
                    ... on CosmeticToken {
                      __typename
                    }
                  }
                }
                infinivault {
                  amount
                  asset {
                    name
                    ... on CosmeticToken {
                      __typename
                    }
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
            val tag = CosmeticTag.valueOf(it.cosmetic.type)

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

        Galapagos.save.infinibag.clear()
        Galapagos.save.infinivault.clear()

        listOf("infinibag", "infinivault").forEach { location ->
            val apiItemsString = jsonElement["data"]?.jsonObject["player"]?.jsonObject[location]?.jsonArray.toString()
            val apiItems: List<APIItem> = Json.decodeFromString(apiItemsString)

            apiItems.forEach {
                val item = Item(
                    name = it.asset.name,
                    count = it.amount,
                    isCosmeticToken = it.asset.__typename == "CosmeticToken"
                )

                if (location == "infinibag") Galapagos.save.infinibag[item.name] = item
                else Galapagos.save.infinivault[item.name] = item
            }
        }
    }

    //// INFINIBAG/COSMETIC UPDATING

    val itemsInScavenging: MutableList<Item> = mutableListOf()
    val itemsInCraftedBlueprint: MutableList<Item> = mutableListOf()
    var cancellingForging: Int? = null
    var craftedBlueprint: String? = null

    fun init() {
        fetchAPI()

        ContainerOpenEvent.EVENT.register { packet -> containerOpen(packet) }
        ContainerSetSlotEvent.EVENT.register { packet -> containerSetSlot(packet) }
        SlotClickEvent.EVENT.register { screen, input, _, _ -> slotClick(screen, input) }
        SystemChatEvent.EVENT.register { packet -> systemChat(packet) }
    }

    fun decrementItem(name: String, amount: Int) {
        val item = Galapagos.save.infinibag[name] ?: return
        item.count -= amount

        if (item.isCosmeticToken) {
            val cosmetic = Galapagos.save.cosmetics[name.dropLast(6)]
            if (cosmetic != null) {
                Galapagos.logger.info("updating ${cosmetic.name}'s rep, previous: ${cosmetic.donations}")
                cosmetic.donations = (cosmetic.donations + amount).coerceIn(0, cosmetic.tag.maxDonations)
                Galapagos.logger.info("updated ${cosmetic.name}'s rep! it is now ${cosmetic.donations}")
            }
        }

        if (item.count <= 0) {
            Galapagos.save.infinibag.remove(name)
        }

        Galapagos.logger.info("decrementing $name by $amount")
    }

    fun moveItem(name: String, amount: Int, where: ItemLocation) {
        val from = if (where == ItemLocation.INFINIBAG) Galapagos.save.infinivault else Galapagos.save.infinibag
        val to = if (where == ItemLocation.INFINIBAG) Galapagos.save.infinibag else Galapagos.save.infinivault

        if (from[name] == null) {
            Galapagos.logger.warn("Attempted to move item $name from a location it doesn't exist in")
            return
        }

        val existsAtDestination = to[name] != null

        if (existsAtDestination) {
            to[name]!!.count += amount
        } else {
            to[name] = Item(
                name = name,
                count = amount,
                isCosmeticToken = from[name]!!.isCosmeticToken
            )
        }

        from[name]!!.count -= amount

        if (from[name]!!.count <= 0) {
            from.remove(name)
        }
    }

    fun containerOpen(packet: ClientboundContainerSetContentPacket) {
        val screen = Minecraft.getInstance().screen ?: return

        packet.items.forEach {
            updateItemState(it)
        }

        if (screen.title.string.contains("INFINIBAG")) InfinibagUpdateEvent.EVENT.invoker().invoke()

        if (screen.title.string.contains("SCAVENGING") && !screen.title.string.contains("WILL PERMANENTLY")) {
            itemsInScavenging.clear()
            val slots = listOf(11, 12, 13, 14, 15, 20, 21, 22, 23, 24)

            slots.forEach {
                val item = packet.items[it]
                if (item.isEmpty) return@forEach
                if (item.itemName.string == "Select item") return@forEach

                itemsInScavenging.add(item.toDataItem())
            }

            Galapagos.logger.info(itemsInScavenging.toString())
        }

        if (screen.title.string.contains("FUSION FORGE")) {
            if (!packet.items[28].isEmpty) return // makes sure its on the forge page and not the recipe page

            val slots = listOf(19, 20, 21, 22, 23, 24, 25)

            Galapagos.save.fusionForge.clear()

            slots.forEach {
                val item = packet.items[it]
                if (item.isEmpty) return@forEach
                if (item.itemName.string == "Select a Recipe") return@forEach
                if (item.itemName.string == "Locked Forge Slot") return@forEach

                Galapagos.save.fusionForge.add(Item(
                    name = item.itemName.string,
                    count = item.count,
                    isCosmeticToken = false
                ))
            }
        }

        if (screen.title.string.contains("STYLE PERKS")) {
            StylePerk.entries.forEach {
                val item = packet.items[it.slotID]
                if (!item.itemName.string.contains(it.label)) throw IllegalStateException("Style perk ${it.name} has incorrect slot ID")

                val regex = Regex("${it.label} \\((?<upgrades>\\d+)")
                val upgrades = regex.find(item.itemName.string)?.groups["upgrades"]?.value?.toIntOrNull() ?: 0

                Galapagos.save.stylePerks[it] = upgrades
            }
        }
    }

    fun containerSetSlot(packet: ClientboundContainerSetSlotPacket) {
        updateItemState(packet.item)
    }

    fun updateItemState(item: ItemStack) {
        val screen = Minecraft.getInstance().screen ?: return

        val location = (if (screen.title.string.contains("INFINIBAG"))
            Galapagos.save.infinibag
        else if (screen.title.string.contains("INFINVAULT"))
            Galapagos.save.infinivault
        else null) ?: return

        val data = item.toDataItem()

        if (location[data.name] != null) {
            location[data.name]!!.count = data.count
        } else {
            location[data.name] = data
        }


    }

    fun slotClick(screen: ContainerScreen, input: ContainerInput) {
        val slot = (screen as HoveredSlotAccessor).`galapagos$hoveredSlot`() ?: return
        val item = slot.item

        if (screen.title.string.contains("INFINIBAG")) {
            handleBlueprintAssemblerInfinibag(slot, item, input)
            handleVault(slot, item, input)
        }

        if (screen.title.string.contains("ASSEMBLE THIS BLUEPRINT?")) {
            handleBlueprintAssembly(slot, item, input)
        }

        if (screen.title.string.contains("INFINIVAULT")) {
            handleVault(slot, item, input)
        }

        if (screen.title.string.contains("BLUEPRINT ASSEMBLER") ) {
            handleMaterialGloopTimeskip(slot, item, input)
        }

        if (screen.title.string.contains("PURCHASE THIS ITEM?")) {
            handleMaterialGloopSpending(slot, item, input)
        }

        if (screen.title.string.contains("FUSION FORGE")) {
            handleMaterialGloopTimeskip(slot, item, input)
            handleFusionForgeCraft(slot, item, input)
            handleFusionForgeClaim(slot, item, input)
        }

        if (screen.title.string.contains("CANCEL FORGING?")) {
            handleFusionForgeCancel(slot, item, input)
        }

        // also handles rep gain from scavenging
        if (screen.title.string.contains("SCAVENGING WILL PERMANENTLY")) {
            handleScavengeConfirm(slot, item, input)
        } else if (screen.title.string.contains("SCAVENGING")) {
            handleScavengeMenu(slot, item, input)
        }

        InfinibagUpdateEvent.EVENT.invoker().invoke()
    }

    // Handles:
    // - Any item gain
    // - Cosmetic claiming

    fun systemChat(packet: ClientboundSystemChatPacket) {
        val regex = Regex("You receive: \\[(?<name>.+)](?: x(?<count>[\\d,]+))?")
        val match = regex.find(packet.content.string) ?: return

        val name = match.groups["name"]?.value ?: return
        val count = match.groups["count"]?.value?.toIntOrNull() ?: 1

        Galapagos.logger.info("obtained $name x$count")

        if (Galapagos.save.cosmetics[name] != null) {
            Galapagos.save.cosmetics[name]!!.isOwned = true
            return
        }

        if (Galapagos.save.infinibag[name] == null) {
            Galapagos.save.infinibag[name] = Item(
                name = name,
                count = count,
                isCosmeticToken = (name.contains("Token") && !name.contains("Blueprint:") && !name.contains("MCC+"))
            )
        } else {
            Galapagos.save.infinibag[name]!!.count += count
        }

        InfinibagUpdateEvent.EVENT.invoker().invoke()
    }

    fun handleBlueprintAssemblerInfinibag(slot: Slot, item: ItemStack, input: ContainerInput) {
        if (!item.findLore("Click to Assemble")) return
        if (item.findLore("(Missing materials)")) return

        val materials = fetchCraftingMaterials(item)
        if (materials.isEmpty()) return

        // TODO: i have no idea if shift-click to assemble 5x blueprints is a real thing. confirm later!!!

        itemsInCraftedBlueprint.clear()
        craftedBlueprint = item.itemName.string

        materials.forEach {
            itemsInCraftedBlueprint.add(Item(
                name = it.first,
                count = it.second,
                isCosmeticToken = false
            ))
        }
    }

    fun handleBlueprintAssembly(slot: Slot, item: ItemStack, input: ContainerInput) {
        if (slot.index !in 46..48) return
        if (craftedBlueprint == null) return
        if (itemsInCraftedBlueprint.isEmpty()) return

        itemsInCraftedBlueprint.forEach {
            decrementItem(it.name, it.count)
        }

        decrementItem(craftedBlueprint!!, 1)

        itemsInCraftedBlueprint.clear()
        craftedBlueprint = null
    }

    fun handleVault(slot: Slot, item: ItemStack, input: ContainerInput) {
        val bag = item.findLore("Left-Click to Vault")
        val vault = item.findLore("Left-Click to Withdraw")

        if (!bag && !vault) return

        var amount = 1

        if (input == ContainerInput.QUICK_MOVE) {
            val regex = Regex("Amount: (?<amount>[\\d,]+)")
            val amountString = item.findLore(regex)?.get("amount")?.value ?: item.count.toString()
            val cleanedString = amountString.replace(",", "")
            amount = cleanedString.toInt()
        }

        Galapagos.logger.info("${if (vault) "withdraw" else "vault"} $amount ${item.itemName.string} ${if (vault) "from" else "to"} vault")
        moveItem(item.itemName.string, amount, if (vault) ItemLocation.INFINIBAG else ItemLocation.INFINIVAULT)
    }

    fun handleMaterialGloopTimeskip(slot: Slot, item: ItemStack, input: ContainerInput) {
        if (item.itemName.string != "Material Gloop") return
        if (item.findLore("You do not have any active")) return

        if (input == ContainerInput.QUICK_MOVE) decrementItem("Material Gloop", 6)
        else decrementItem("Material Gloop", 1)
    }

    fun handleMaterialGloopSpending(slot: Slot, item: ItemStack, input: ContainerInput) {
        val regex = Regex("Cost: [\\d,]+/(?<cost>[\\d,]+) \\[Material Gloop]")
        val cost = item.findLore(regex)?.get("cost")?.value?.toIntOrNull() ?: return

        decrementItem("Material Gloop", cost)
    }

    fun handleFusionForgeCraft(slot: Slot, item: ItemStack, input: ContainerInput) {
        if (!item.findLore("Click to Forge")) return
        if (item.findLore("Click to Forge (Missing materials)")) return
        if (input == ContainerInput.QUICK_MOVE && item.findLore("(Missing materials)")) return

        Galapagos.save.fusionForge.add(Item(
            name = item.itemName.string,
            count = if (input == ContainerInput.QUICK_MOVE) 5 else 1,
            isCosmeticToken = false
        ))

        val materials = fetchCraftingMaterials(item)
        if (materials.isEmpty()) return

        materials.forEach {
            decrementItem(it.first, it.second * if (input == ContainerInput.QUICK_MOVE) 5 else 1)
        }
    }

    fun handleFusionForgeClaim(slot: Slot, item: ItemStack, input: ContainerInput) {
        if (item.findLore("Click to Claim Item")) {
            val index = Galapagos.save.fusionForge.indexOfFirst {
                it.name == item.itemName.string && it.count == item.count
            }

            if (index == -1) return

            Galapagos.save.fusionForge.removeAt(index)
        }

        if (item.findLore("Shift-Click to Cancel Forging") && input == ContainerInput.QUICK_MOVE) {
            val index = Galapagos.save.fusionForge.indexOfFirst {
                it.name == item.itemName.string && it.count == item.count
            }

            if (index == -1) return

            cancellingForging = index
        }
    }

    fun handleFusionForgeCancel(slot: Slot, item: ItemStack, input: ContainerInput) {
        if (slot.index !in 46..48) return
        if (cancellingForging == null) return

        Galapagos.save.fusionForge.removeAt(cancellingForging!!)
    }

    fun handleScavengeConfirm(slot: Slot, item: ItemStack, input: ContainerInput) {
        if (slot.index !in 46..48) return

        itemsInScavenging.forEach {
            decrementItem(it.name, it.count)
        }

        itemsInScavenging.clear()
    }

    fun handleScavengeMenu(slot: Slot, item: ItemStack, input: ContainerInput) {
        if (item.itemName.string == "Cosmetic Bulk Scavenge") {
            if (item.findLore("Grand Champ")) return

            Galapagos.save.infinibag.values.toList().forEach {
                if (!it.isCosmeticToken) return@forEach
                if (Galapagos.save.cosmetics[it.name.dropLast(6)] == null) return@forEach

                decrementItem(it.name, it.count)
            }
        }

        if (input != ContainerInput.QUICK_MOVE) return
        val dataItem = item.toDataItem()

        val index = itemsInScavenging.indexOfFirst {
            it.name == dataItem.name && it.count == dataItem.count
        }
        if (index == -1) return

        itemsInScavenging.removeAt(index)
    }
}