package xyz.nibblz.galapagos.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.data.*
import xyz.nibblz.galapagos.events.*
import xyz.nibblz.galapagos.features.CraftingInstructions
import xyz.nibblz.galapagos.features.XPInfo
import xyz.nibblz.galapagos.util.findLore
import xyz.nibblz.galapagos.util.sendGalapagosChatMessage
import xyz.nibblz.galapagos.util.toDataItem
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object PlayerData : CoreFeature {
    @Serializable
    data class APICosmeticData(
        val trophies: Int,
        val name: String,
        val collection: String,
        val type: String,
        val isBonusTrophies: Boolean? = false
    )

    @Serializable
    data class APICosmetic(
        val cosmetic: APICosmeticData,
        val chromaPacks: List<String>? = null,
        val owned: Boolean,
        val donationsMade: Int? = null,
    )

    @Serializable
    data class APIItem(
        val amount: Int,
        val asset: APIItemAsset
    )

    @Serializable
    data class APIItemAsset(
        val name: String,
        @SerialName("__typename") val typename: String? = null
    )

    @Serializable
    data class APIFaction(
        val selected: Boolean,
        val totalExperience: Int,
        val name: String
    )

    val client: HttpClient? = HttpClient.newHttpClient()

    fun fetchAPI(): Boolean {
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
                      isBonusTrophies
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
                statistics {
                  battle_box_xp_earned: rotationValue(statisticKey: \"battle_box_xp_earned\")
                  battle_box_quads_xp_earned: rotationValue(statisticKey: \"battle_box_quads_xp_earned\")
                  battle_box_arena_xp_earned: rotationValue(statisticKey: \"battle_box_arena_xp_earned\")
                  dynaball_xp_earned: rotationValue(statisticKey: \"dynaball_xp_earned\")
                  hole_in_the_wall_xp_earned: rotationValue(statisticKey: \"hole_in_the_wall_xp_earned\")
                  pw_xp_earned: rotationValue(statisticKey: \"pw_xp_earned\")
                  pw_survival_xp_earned: rotationValue(statisticKey: \"pw_survival_xp_earned\")
                  pw_solo_xp_earned: rotationValue(statisticKey: \"pw_solo_xp_earned\")
                  rocket_spleef_xp_earned: rotationValue(statisticKey: \"rocket_spleef_xp_earned\")
                  sky_battle_xp_earned: rotationValue(statisticKey: \"sky_battle_xp_earned\")
                  sky_battle_quads_xp_earned: rotationValue(statisticKey: \"sky_battle_quads_xp_earned\")
                  sky_battle_solos_xp_earned: rotationValue(statisticKey: \"sky_battle_solos_xp_earned\")
                  tgttos_xp_earned: rotationValue(statisticKey: \"tgttos_xp_earned\")
                  
                  battle_box_quads_games_played: rotationValue(statisticKey: \"battle_box_quads_games_played\")
                  battle_box_arena_games_played: rotationValue(statisticKey: \"battle_box_arena_games_played\")
                  dynaball_games_played: rotationValue(statisticKey: \"dynaball_games_played\")
                  hole_in_the_wall_games_played: rotationValue(statisticKey: \"hole_in_the_wall_games_played\")
                  pw_survival_games_played: rotationValue(statisticKey: \"pw_survival_games_played\")
                  rocket_spleef_games_played: rotationValue(statisticKey: \"rocket_spleef_games_played\")
                  sky_battle_quads_games_played: rotationValue(statisticKey: \"sky_battle_quads_games_played\")
                  sky_battle_solos_games_played: rotationValue(statisticKey: \"sky_battle_solos_games_played\")
                  tgttos_games_played: rotationValue(statisticKey: \"tgttos_games_played\")
                }
                factions {
                  selected
                  totalExperience
                  name
                }
                ranks
              }
            }
        """.trimIndent().replace("\n", "\\n")

        val request = if (Config.values::usePersonalApiKey.get()) {
            HttpRequest.newBuilder()
                .uri(URI.create("https://api.mccisland.net/graphql"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"query\" : \"$graphQL\"}"))
                .header("Accept", "application/json")
                .header("content-type", "application/json")
                .header("X-API-Key", Galapagos.save.apiKey)
                .header("User-Agent", "galapagos-mc-mod/${Minecraft.getInstance().gameProfile.id} (discord/@nibbl_z)")
                .build()
        } else {
            HttpRequest.newBuilder()
                .uri(URI.create("https://galapagos.nibbles.hackclub.app/fetch_api/${Minecraft.getInstance().gameProfile.id}"))
                .GET()
                .header("X-MC-UUID", Minecraft.getInstance().gameProfile.id.toString())
                .build()
        }

        val response = try {
            client?.send(request, HttpResponse.BodyHandlers.ofString()) ?: return false
        } catch(exception: Exception) {
            sendGalapagosChatMessage(
                Component.literal("${if (Config.values::usePersonalApiKey.get()) "MCCI's API" else "Custom API endpoint"} appears to be down. Try again later, and check logs for more information.")
                    .withColor(ChatFormatting.RED.color!!)
            )
            Galapagos.logger.error("API request error: ${exception.message}, ${exception.cause}")
            return false
        }

        val jsonElement = try {
            Json.parseToJsonElement(response.body()).jsonObject
        } catch(exception: Exception) {
            sendGalapagosChatMessage(
                Component.literal("${if (Config.values::usePersonalApiKey.get()) "MCCI's API" else "Custom API endpoint"} appears to be down. Try again later, and check logs for more information.")
                    .withColor(ChatFormatting.RED.color!!)
            )
            Galapagos.logger.error("API request error: ${exception.message}, ${exception.cause}")
            return false
        }

        if (jsonElement["message"]?.jsonPrimitive?.content == "Unauthorized") {
            if (!Config.values::usePersonalApiKey.get()) {
                sendGalapagosChatMessage(
                    Component.literal("Something went wrong when fetching the custom endpoint. Please report this issue to the developers!")
                        .withColor(ChatFormatting.RED.color!!)
                )
            } else if (Galapagos.save.apiKey.isEmpty()) {
                sendGalapagosChatMessage(
                    Component.literal("You do not have an API key set! Please set one using /galapagos api set <API_KEY>")
                        .withColor(ChatFormatting.RED.color!!)
                )
            } else {
                sendGalapagosChatMessage(
                    Component.literal("Your API key is invalid! Please set a valid API key using /galapagos api set <API_KEY>")
                        .withColor(ChatFormatting.RED.color!!)
                )
            }

            return false
        }

        if (jsonElement["errors"] != null) {
            sendGalapagosChatMessage(
                Component.literal("Something went wrong when fetching the MCC Island API. Check log for more information.")
                    .withColor(ChatFormatting.RED.color!!)
            )
            Galapagos.logger.error("MCC Island API error: ${response.body()}")
            return false
        }

        if (jsonElement["data"]?.jsonObject["player"]?.jsonObject["collections"] == null) {
            sendGalapagosChatMessage(
                Component.literal("You have Collections disabled in your API settings! Please navigate to Pocket Menu -> Settings -> API Settings, and enable Collections. This may take a few minute to update!")
                    .withColor(ChatFormatting.RED.color!!)
            )
            return false
        }

        if (jsonElement["data"]?.jsonObject["player"]?.jsonObject["infinibag"] == null || jsonElement["data"]?.jsonObject["player"]?.jsonObject["infinivault"] == null) {
            sendGalapagosChatMessage(
                Component.literal("You have Infinibag disabled in your API settings! Please navigate to Pocket Menu -> Settings -> API Settings, and enable Infinibag. This may take a few minute to update!")
                    .withColor(ChatFormatting.RED.color!!)
            )
            return false
        }

        val apiCosmeticsString = jsonElement["data"]?.jsonObject["player"]?.jsonObject["collections"]?.jsonObject["cosmetics"]?.jsonArray.toString()
        val apiCosmetics: List<APICosmetic> = Json.Default.decodeFromString(apiCosmeticsString)

        apiCosmetics.forEach {
            val collection = CosmeticCollection.entries.find { entry -> entry.label == it.cosmetic.collection } ?: return@forEach
            if (it.cosmetic.isBonusTrophies == true) return@forEach
            if (it.cosmetic.trophies == 0) return@forEach
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

            Galapagos.save.cosmetics[it.cosmetic.name] = cosmetic
        }

        Galapagos.save.infinibag.clear()
        Galapagos.save.infinivault.clear()

        listOf("infinibag", "infinivault").forEach { location ->
            val apiItemsString = jsonElement["data"]?.jsonObject["player"]?.jsonObject[location]?.jsonArray.toString()
            val apiItems: List<APIItem> = Json.Default.decodeFromString(apiItemsString)

            apiItems.forEach {
                val item = Item(
                    name = it.asset.name,
                    count = it.amount,
                    isCosmeticToken = it.asset.typename == "CosmeticToken"
                )

                if (location == "infinibag") Galapagos.save.infinibag[item.name] = item
                else Galapagos.save.infinivault[item.name] = item
            }
        }

        Galapagos.logger.info(jsonElement.toString())

        val ranks: List<String> = Json.Default.decodeFromString(
            jsonElement["data"]?.jsonObject["player"]?.jsonObject["ranks"]?.jsonArray.toString()
        )

        ranks.forEach {
            val rank = Rank.valueOf(it)

            if (Galapagos.save.rank == null || (Galapagos.save.rank?.ordinal ?: -1) < rank.ordinal) {
                Galapagos.save.rank = rank
            }
        }

        val statistics: HashMap<String, Int> = Json.Default.decodeFromString(
            jsonElement["data"]?.jsonObject["player"]?.jsonObject["statistics"]?.jsonObject.toString()
        )

        statistics.forEach { (statistic, stat) ->
            val starLevelGame = StarLevelGame.entries.find { it.statistic == statistic }
            if (starLevelGame != null) Galapagos.save.starLevelXP[starLevelGame] = stat

            val xpSource = XPInfo.XPSource.entries.find { it.xpStatistic == statistic }
            if (xpSource != null) Galapagos.save.gameXP[xpSource] = stat

            val gamePlayedSource = XPInfo.XPSource.entries.find { it.gamesPlayedStatistic == statistic }
            if (gamePlayedSource != null) Galapagos.save.gamesPlayed[gamePlayedSource] = stat
        }

        val factions: List<APIFaction> = Json.Default.decodeFromString(
            jsonElement["data"]?.jsonObject["player"]?.jsonObject["factions"]?.jsonArray.toString()
        )

        factions.forEach { 
            val faction = Faction.valueOf(it.name)
            if (it.selected) Galapagos.save.selectedFaction = faction
            Galapagos.save.factionXP[faction] = it.totalExperience
        }

        return true
    }

    //// INFINIBAG/COSMETIC UPDATING

    val itemsInScavenging: MutableList<Item> = mutableListOf()
    val itemsInCraftedBlueprint: MutableList<Item> = mutableListOf()
    var cancellingForging: Int? = null
    var cancellingAssembly: Int? = null
    var craftedBlueprint: String? = null
    var activatingQuestScroll: String? = null

    override fun init() {
        ContainerOpenEvent.EVENT.register { packet -> containerOpen(packet) }
        ContainerSetSlotEvent.EVENT.register { packet -> containerSetSlot(packet) }
        SlotClickEvent.EVENT.register { slot, screen, input, _, _ -> slotClick(slot, screen, input) }
        SystemChatEvent.EVENT.register { packet -> systemChat(packet) }
        JoinMCCIEvent.EVENT.register {
            if (!Galapagos.save.finishedOOBE) return@register
            fetchAPI()
        }
    }

    fun decrementItem(name: String, amount: Int) {
        val item = Galapagos.save.infinibag[name] ?: return
        item.count -= amount

        if (item.isCosmeticToken) {
            val cosmetic = Galapagos.save.cosmetics[name.dropLast(6)]
            if (cosmetic != null && cosmetic.donations != cosmetic.tag.maxDonations) {
                RoyalReputationIncreaseEvent.EVENT.invoker().invoke(cosmetic.name, amount.coerceIn(0, cosmetic.tag.maxDonations - cosmetic.donations))
                cosmetic.donations = (cosmetic.donations + amount).coerceIn(0, cosmetic.tag.maxDonations)
            }
        }

        if (item.count <= 0) {
            Galapagos.save.infinibag.remove(name)
        }
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

        if (screen.title.string.contains("ISLAND REWARDS")) {
            val favorites = packet.items[43]
            Galapagos.save.mccPlus = favorites.findLore("Click to Select Favorites")
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

                Galapagos.save.fusionForge.add(
                    Item(
                        name = item.itemName.string,
                        count = item.count,
                        isCosmeticToken = false
                    )
                )
            }
        }

        if (screen.title.string.contains("BLUEPRINT ASSEMBLER")) {
            val slots = listOf(19, 20, 21, 22, 23, 24, 25)

            Galapagos.save.blueprintAssembler.clear()

            slots.forEach {
                val item = packet.items[it]
                if (item.isEmpty) return@forEach
                if (item.itemName.string == "Select a Blueprint") return@forEach
                if (item.itemName.string == "Locked Assember Slot") return@forEach // noxcrew... come on...
                if (item.itemName.string == "Locked Assembler Slot") return@forEach // Be So for real.

                Galapagos.save.blueprintAssembler.add(
                    Item(
                        name = item.itemName.string,
                        count = item.count,
                        isCosmeticToken = false
                    )
                )
            }
        }

        if (screen.title.string.contains("STYLE PERKS")) {
            StylePerk.entries.forEach {
                val item = packet.items[it.slotID]
                if (!item.itemName.string.contains(it.label)) throw IllegalStateException("Style perk ${it.name} has incorrect slot ID")

                val regex = Regex("${it.label} \\((?<upgrades>\\d+)")
                var upgrades = regex.find(item.itemName.string)?.groups["upgrades"]?.value?.toIntOrNull() ?: 0

                it.arcanes.forEach { arcane ->
                    if (Galapagos.save.cosmetics[arcane]?.isOwned == true) {
                        upgrades++
                    }
                }

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

    fun slotClick(slot: Slot, screen: ContainerScreen, input: ContainerInput) {
        val item = slot.item

        if (screen.title.string.contains("INFINIBAG")) {
            handleBlueprintAssemblerInfinibag(item)
            handleInfinibag(item)
            handleVault(item, input)
        }

        if (screen.title.string.contains("ASSEMBLE THIS BLUEPRINT?")) {
            handleBlueprintAssembly(slot)
        }

        if (screen.title.string.contains("INFINIVAULT")) {
            handleVault(item, input)
        }

        if (screen.title.string.contains("BLUEPRINT ASSEMBLER") ) {
            handleMaterialGloopTimeskip(item, input)
            handleCraftingClaim(item, input, false)
        }

        if (screen.title.string.contains("PURCHASE THIS ITEM?")) {
            handleMaterialGloopSpending(item)
        }

        if (screen.title.string.contains("FUSION FORGE")) {
            handleMaterialGloopTimeskip(item, input)
            handleFusionForgeCraft(item, input)
            handleCraftingClaim(item, input, true)
        }

        if (screen.title.string.contains("CANCEL FORGING?") || screen.title.string.contains("CANCEL ASSSEMBLY?")) {
            handleCraftCancel(slot)
        }

        // also handles rep gain from scavenging
        if (screen.title.string.contains("SCAVENGING WILL PERMANENTLY")) {
            handleScavengeConfirm(slot)
        } else if (screen.title.string.contains("SCAVENGING")) {
            handleScavengeMenu(item, input)
        }

        if (screen.title.string.contains("STYLE PERKS")) {
            handleStylePerkPurchase(item)
        }

        if (screen.title.string.contains("ACTIVATE THIS QUEST SCROLL?")) {
            handleActivateQuestScroll(item)
        }

        InfinibagUpdateEvent.EVENT.invoker().invoke()
    }

    // Handles:
    // - Any item gain
    // - Cosmetic claiming
    // - Faction switching

    fun systemChat(packet: ClientboundSystemChatPacket) {
        handleItemGain(packet)
        handleFactionSwitch(packet)
    }

    fun handleItemGain(packet: ClientboundSystemChatPacket) {
        val regex = Regex("You receive: \\[(?<name>.+)](?: x(?<count>[\\d,]+))?")
        val match = regex.find(packet.content.string) ?: return

        val name = match.groups["name"]?.value ?: return
        val count = match.groups["count"]?.value?.toIntOrNull() ?: 1

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

    fun handleFactionSwitch(packet: ClientboundSystemChatPacket) {
        val match = Regex("You are now a part of the (?<faction>.+)\\.").find(packet.content.string)
            ?.groups?.get("faction")?.value ?: return

        val faction = Faction.entries.find { it.label == match } ?: return
        Galapagos.save.selectedFaction = faction
    }

    fun handleBlueprintAssemblerInfinibag(item: ItemStack) {
        if (!item.findLore("Click to Assemble")) return
        if (item.findLore("(Missing materials)")) return

        val materials = CraftingInstructions.fetchCraftingMaterials(item)
        if (materials.isEmpty()) return

        // TODO: i have no idea if shift-click to assemble 5x blueprints is a real thing. confirm later!!!
        // ok I KNOW it exists but i dont know what the tooltip says so i cant implement it rn
        // and im getting SICK OF IT!!!!!!!!!

        itemsInCraftedBlueprint.clear()
        craftedBlueprint = item.itemName.string

        materials.forEach {
            itemsInCraftedBlueprint.add(
                Item(
                    name = it.first,
                    count = it.second,
                    isCosmeticToken = false
                )
            )
        }
    }

    fun handleBlueprintAssembly(slot: Slot) {
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

    fun handleVault(item: ItemStack, input: ContainerInput) {
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

        moveItem(item.itemName.string, amount, if (vault) ItemLocation.INFINIBAG else ItemLocation.INFINIVAULT)
    }

    fun handleMaterialGloopTimeskip(item: ItemStack, input: ContainerInput) {
        if (item.itemName.string != "Material Gloop") return
        if (item.findLore("You do not have any active")) return

        if (input == ContainerInput.QUICK_MOVE) decrementItem("Material Gloop", 6)
        else decrementItem("Material Gloop", 1)
    }

    fun handleMaterialGloopSpending(item: ItemStack) {
        val regex = Regex("Cost: [\\d,]+/(?<cost>[\\d,]+) \\[Material Gloop]")
        val cost = item.findLore(regex)?.get("cost")?.value?.toIntOrNull() ?: return

        decrementItem("Material Gloop", cost)
    }

    fun handleFusionForgeCraft(item: ItemStack, input: ContainerInput) {
        if (!item.findLore("Click to Forge")) return
        if (item.findLore("Click to Forge (Missing materials)")) return
        if (input == ContainerInput.QUICK_MOVE && item.findLore("(Missing materials)")) return

        Galapagos.save.fusionForge.add(
            Item(
                name = item.itemName.string,
                count = if (input == ContainerInput.QUICK_MOVE) 5 else 1,
                isCosmeticToken = false
            )
        )

        val materials = CraftingInstructions.fetchCraftingMaterials(item)
        if (materials.isEmpty()) return

        materials.forEach {
            decrementItem(it.first, it.second * if (input == ContainerInput.QUICK_MOVE) 5 else 1)
        }
    }

    fun handleCraftingClaim(item: ItemStack, input: ContainerInput, isForge: Boolean) {
        if (item.findLore("Click to Claim Item")) {
            val index = (if (isForge) Galapagos.save.fusionForge else Galapagos.save.blueprintAssembler).indexOfFirst {
                it.name == item.itemName.string && it.count == item.count
            }

            if (index == -1) return

            (if (isForge) Galapagos.save.fusionForge else Galapagos.save.blueprintAssembler).removeAt(index)
        }

        if (item.findLore("Shift-Click to Cancel ${if (isForge) "Forging" else "Assembly"}") && input == ContainerInput.QUICK_MOVE) {
            val index = (if (isForge) Galapagos.save.fusionForge else Galapagos.save.blueprintAssembler).indexOfFirst {
                it.name == item.itemName.string && it.count == item.count
            }

            if (index == -1) return

            if (isForge) cancellingForging = index
            else cancellingAssembly = index
        }
    }

    fun handleCraftCancel(slot: Slot) {
        if (slot.index !in 46..48) return

        if (cancellingForging != null) {
            Galapagos.save.fusionForge.removeAt(cancellingForging!!)
            cancellingForging = null
            return
        }

        if (cancellingAssembly != null) {
            Galapagos.save.blueprintAssembler.removeAt(cancellingAssembly!!)
            cancellingAssembly = null
            return
        }
    }

    fun handleScavengeConfirm(slot: Slot) {
        if (slot.index !in 46..48) return

        itemsInScavenging.forEach {
            decrementItem(it.name, it.count)
        }

        itemsInScavenging.clear()
    }

    fun handleScavengeMenu(item: ItemStack, input: ContainerInput) {
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

    fun handleStylePerkPurchase(item: ItemStack) {
        if (!item.findLore("Left-Click to Upgrade Perk")) return

        val perkName = Regex("(?<perk>.+) ").find(item.itemName.string)?.groups["perk"]?.value
        val stylePerk = StylePerk.entries.find { it.label == perkName } ?: return
        val materials = CraftingInstructions.fetchCraftingMaterials(item)

        materials.forEach { (material, count) ->
            decrementItem(material, count)
        }

        Galapagos.save.stylePerks[stylePerk] = Galapagos.save.stylePerks[stylePerk]!! + 1
    }

    fun handleInfinibag(item: ItemStack) {
        if (item.findLore("Click to Use") && item.itemName.string.contains("Quest Scroll")) {
            activatingQuestScroll = item.itemName.string
        }
    }

    fun handleActivateQuestScroll(item: ItemStack) {
        if (activatingQuestScroll == null) return // erm
        if (!item.findLore("Click to activate")) return

        decrementItem(activatingQuestScroll!!, 1)
        activatingQuestScroll = null
    }
}