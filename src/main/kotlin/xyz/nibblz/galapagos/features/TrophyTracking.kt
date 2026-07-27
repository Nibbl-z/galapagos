package xyz.nibblz.galapagos.features

import kotlinx.serialization.Serializable
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.item.ItemStack
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.data.ConstantIslandData
import xyz.nibblz.galapagos.data.CosmeticCollection
import xyz.nibblz.galapagos.data.FishingResearch
import xyz.nibblz.galapagos.data.FishingUpgrade
import xyz.nibblz.galapagos.data.repPerDonation
import xyz.nibblz.galapagos.events.ContainerCloseEvent
import xyz.nibblz.galapagos.events.RoyalReputationIncreaseEvent
import xyz.nibblz.galapagos.events.SlotClickEvent
import xyz.nibblz.galapagos.events.SystemChatEvent
import xyz.nibblz.galapagos.mixin.accessor.HoveredSlotAccessor
import xyz.nibblz.galapagos.screens.TrophyHistory
import xyz.nibblz.galapagos.util.Glyphs
import xyz.nibblz.galapagos.util.findLore
import xyz.nibblz.galapagos.util.playMcciSound
import kotlin.reflect.KMutableProperty0
import kotlin.time.Clock

object TrophyTracking : Feature {
    override val id: String = "trophy_tracking"
    override val name: String = "Trophy Tracking"
    override val description: List<Component> = listOf()
    override val enabledProperty: KMutableProperty0<Boolean> = Config.values::trophyTrackingEnabled
    override val image: Config.ConfigImage = Config.ConfigImage("quest_tracking.png", 1097, 465)

    override fun init() {
        SlotClickEvent.EVENT.register { screen, _, _, _ -> slotClick(screen) }
        SystemChatEvent.EVENT.register { packet -> systemChat(packet) }
        RoyalReputationIncreaseEvent.EVENT.register { cosmetic, count -> royalReputationIncrease(cosmetic, count) }
        ItemTooltipCallback.EVENT.register { stack, _, _, components -> tooltipAdd(stack, components) }
        ContainerCloseEvent.EVENT.register { containerClose() }
        ClientTickEvents.END_CLIENT_TICK.register {
            if (!openTrophyHistory) return@register
            openTrophyHistory = false
            Minecraft.getInstance().setScreen(TrophyHistory()) // A-A-AND I GET JUST WHAT I NEED !!!
        }
    }

    enum class TrophyType(val label: String, val sprite: String, val color: Int) {
        SKILL("Skill", "red", 0xff4a4a),
        STYLE("Style", "purple", 0xa347ff),
        ANGLER("Angler", "blue", 0x44aafc)
    }

    enum class TrophySource(val type: TrophyType, val sprite: String) {
        CLAIM_BADGE(TrophyType.SKILL, "_fonts/icon/badges_small"),

        CLAIM_COSMETIC(TrophyType.STYLE, "_fonts/icon/hat_small"),
        ROYAL_REPUTATION(TrophyType.STYLE, "_fonts/icon/royal_reputation"),
        MAX_CHROMA(TrophyType.STYLE, "_fonts/icon/chroma_pack_small"),
        COLLECTION_BONUS(TrophyType.STYLE, "_fonts/icon/arcane_gate"),

        CLAIM_FISH(TrophyType.ANGLER, "_fonts/icon/fish_fish"),
        CLAIM_RESEARCH(TrophyType.ANGLER, "island_interface/badges/general/loyalist_pink_parrots"), // uuuaaughhh
        UPGRADE_PURCHASE(TrophyType.ANGLER, "_fonts/icon/fishing_perk/xp_magnet")
    }

    @Serializable
    data class TrophyGain(
        val type: TrophyType,
        val source: TrophySource,
        val trophies: Int,
        val timestamp: Long,
        val data: String,
        val dataCount: Int
    ) {
        fun getLabel(): String {
            return when(source) {
                TrophySource.CLAIM_BADGE -> "Claim $data (Stage $dataCount)"
                TrophySource.CLAIM_COSMETIC -> "Claim $data"
                TrophySource.ROYAL_REPUTATION -> "Scavenged${if (dataCount > 1) " x${dataCount} " else " "}$data"
                TrophySource.MAX_CHROMA -> "Obtained all chromas on $data"
                TrophySource.COLLECTION_BONUS -> "Collection bonus ($dataCount/5) for ${CosmeticCollection.valueOf(data).label}"
                TrophySource.CLAIM_FISH -> "Discovered $data"
                TrophySource.CLAIM_RESEARCH -> "Claimed ${FishingResearch.valueOf(data).label} (Level $dataCount)"
                TrophySource.UPGRADE_PURCHASE -> "Purchased ${FishingUpgrade.valueOf(data).label} (Level $dataCount)"
            }
        }

        fun getIcon(): String {
            return when(source) {
                TrophySource.CLAIM_BADGE -> ConstantIslandData.data.badgeSprites[data] ?: "island_interface/badges/general/loyalist_pink_parrots" // :pink:
                TrophySource.CLAIM_COSMETIC -> "island_interface/wardrobe/hat/icon"
                // maybe one day itd be cool to figure out how to get it to render the actual cosmetic model
                // but HOW would i even do that.
                TrophySource.ROYAL_REPUTATION -> "_fonts/icon/royal_reputation"
                TrophySource.MAX_CHROMA -> "_fonts/icon/chroma_pack_small"
                TrophySource.COLLECTION_BONUS -> CosmeticCollection.valueOf(data).sprite
                TrophySource.CLAIM_FISH -> ConstantIslandData.data.fishSprites[data] ?: "island_items/infinibag/fish/floral_forest/lime_sole" // our beloved
                TrophySource.CLAIM_RESEARCH -> FishingResearch.valueOf(data).sprite
                TrophySource.UPGRADE_PURCHASE -> FishingUpgrade.valueOf(data).sprite
            }
        }
    }

    var chromaCosmetic: String? = null
    var upgradingPerk: FishingUpgrade? = null
    var upgradingPerkLevel = 0

    var clickedTrophyHistory = false
    var openTrophyHistory = false

    fun slotClick(screen: ContainerScreen) {
        val slot = (screen as HoveredSlotAccessor).`galapagos$hoveredSlot`() ?: return

        if (slot.item.itemName.string == "Crown Level" && screen.title.string.contains("MY PROFILE")) {
            clickedTrophyHistory = true
            playMcciSound("ui.click_normal")
            playMcciSound("ui.experience_receive")
            Minecraft.getInstance().connection!!.send(ServerboundContainerClosePacket(Minecraft.getInstance().player!!.containerMenu.containerId))
        }

        // handles CLAIM_COSMETIC + COLLECTION_BONUS
        if (Galapagos.save.cosmetics[slot.item.itemName.string] != null && slot.item.findLore("Click to Claim!")) { // its so eager. Claim!
            handleClaimCosmetic(screen, slot.item)
        }

        // handles MAX_CHROMA
        if (Galapagos.save.cosmetics[slot.item.itemName.string] != null && slot.item.findLore("Click to Apply")) {
            chromaCosmetic = slot.item.itemName.string
        }

        // handles CLAIM_RESEARCH
        if (FishingResearch.entries.any { slot.item.itemName.string.contains(it.label) } && slot.item.findLore("Click to Claim")) {
            handleClaimResearch(slot.item)
        }

        // handles UPGRADE_PURCHASE
        if (FishingUpgrade.entries.any { slot.item.itemName.string.contains(it.label) } && slot.item.findLore("Click to Upgrade")) {
            upgradingPerk = FishingUpgrade.entries.find { slot.item.itemName.string.contains(it.label) }
            upgradingPerkLevel = Regex("(?<level>[\\d,]+)/").find(slot.item.itemName.string)?.groups["level"]?.value?.toIntOrNull()?.plus(1) ?: return
            Galapagos.logger.info("$upgradingPerk , $upgradingPerkLevel")
        }

        if (upgradingPerk != null && screen.title.string.contains("PURCHASE THIS UPGRADE?") && slot.index in 46..48) {
            handleUpgradePurchase()
        }

        // handles CLAIM_BADGE
        if (slot.item.findLore("Click to Claim!") && screen.title.string.contains("GAME PROGRESSION")) {
            handleClaimBadge(slot.item)
        }

        // handles CLAIM_FISH
        if (
            slot.item.findLore("Click to Claim Trophies") &&
            (
                slot.item.findLore(Glyphs.getGlyph("_fonts/icon/tooltips/fish.png"))
                || slot.item.findLore(Glyphs.getGlyph("_fonts/icon/tooltips/crab.png"))
            )
        ) {
            handleClaimFish(slot.item)
        }
    }

    fun containerClose() {
        if (!clickedTrophyHistory) return
        clickedTrophyHistory = false
        openTrophyHistory = true
    }

    fun tooltipAdd(stack: ItemStack, components: MutableList<Component>) {
        val screen = Minecraft.getInstance().screen ?: return
        if (stack.itemName.string != "Crown Level") return
        if (!screen.title.string.contains("MY PROFILE")) return

        var index = components.indexOfFirst { it.string.contains("minecraft:") } // if you have f3+h on :P
        if (index == -1) { index = components.size - 1 } // if you dont !

        components.add(index, Component.empty())

        components.add(index + 1, Component.empty()
            .append(Glyphs.getGlyphComponent("_fonts/icon/click_action_left.png"))
            .append(Component.literal(" > ").withColor(ChatFormatting.DARK_GRAY.color!!))
            .append(Component.literal("Click to ").withColor(0xecd584))
            .append(Component.literal("View Trophy History").withColor(0xfee761)))
    }

    fun systemChat(packet: ClientboundSystemChatPacket) {
        // handles MAX_CHROMA
        if (packet.content.string.contains("You unlocked all Chromas on your cosmetic!")) {
            handleChroma()
        }
    }

    fun handleClaimCosmetic(screen: ContainerScreen, item: ItemStack) {
        val cosmetic = Galapagos.save.cosmetics[item.itemName.string] ?: return

        val trophyGain = TrophyGain(
            type = TrophyType.STYLE,
            source = TrophySource.CLAIM_COSMETIC,
            trophies = cosmetic.rarity.trophies,
            timestamp = Clock.System.now().epochSeconds,
            data = cosmetic.name,
            dataCount = 0
        )

        Galapagos.logger.info("$trophyGain")

        Galapagos.save.trophyHistory.add(trophyGain)

        val collectionItem = screen.menu.slots[4].item
        val collection = CosmeticCollection.entries.find { it.label == collectionItem.itemName.string.dropLast(" Collection Completion Reward".length) } ?: return
        Galapagos.logger.info(collection.name)

        if (collection == CosmeticCollection.SPECIAL) return

        val requiredForBonus = collectionItem.findLore(Regex("Unlock (?<req>\\d+) more"))?.get("req")?.value?.toIntOrNull() ?: return

        if (requiredForBonus != 1) return

        val currentBonus = collectionItem.findLore(
            Regex("Collection Bonus: (?<current>[\\d,]+)/")
        )?.get("current")?.value?.replace(",", "")?.toIntOrNull() ?: (collection.bonus * 5)

        val nextLevel = (currentBonus / (collection.bonus)) + 1
        if (nextLevel > 5) return // already maxed..?

        val collectionBonusTrophyGain = TrophyGain(
            type = TrophyType.STYLE,
            source = TrophySource.COLLECTION_BONUS,
            trophies = collection.bonus,
            timestamp = Clock.System.now().epochSeconds,
            data = collection.name,
            dataCount = nextLevel
        )

        Galapagos.save.trophyHistory.add(collectionBonusTrophyGain)
    }

    fun handleChroma() {
        if (chromaCosmetic == null) {
            Galapagos.logger.warn("Received max chroma message, but no cosmetic was clicked?")
            return
        }

        val maxChromaTrophyGain = TrophyGain(
            type = TrophyType.STYLE,
            source = TrophySource.MAX_CHROMA,
            trophies = 10,
            timestamp = Clock.System.now().epochSeconds,
            data = chromaCosmetic!!,
            dataCount = 0
        )

        Galapagos.save.trophyHistory.add(maxChromaTrophyGain)

        chromaCosmetic = null
    }

    fun handleClaimResearch(item: ItemStack) {
        val research = FishingResearch.entries.find { item.itemName.string.contains(it.label) } ?: return
        val level = Regex("(?<level>[\\d,]+)/").find(item.itemName.string)?.groups["level"]?.value?.toIntOrNull()?.plus(1) ?: return

        val researchTrophyGain = TrophyGain(
            type = TrophyType.ANGLER,
            source = TrophySource.CLAIM_RESEARCH,
            trophies = if (level > 90) 20 else 10,
            timestamp = Clock.System.now().epochSeconds,
            data = research.name,
            dataCount = level
        )

        Galapagos.save.trophyHistory.add(researchTrophyGain)
    }

    fun handleUpgradePurchase() {
        if (upgradingPerk == null) {
            Galapagos.logger.warn("Clicked confirm on upgrade window, but there's no perk?")
            return
        }

        val upgradeTrophyGain = TrophyGain(
            type = TrophyType.ANGLER,
            source = TrophySource.UPGRADE_PURCHASE,
            trophies = 10,
            timestamp = Clock.System.now().epochSeconds,
            data = upgradingPerk!!.name,
            dataCount = upgradingPerkLevel
        )

        Galapagos.save.trophyHistory.add(upgradeTrophyGain)

        upgradingPerkLevel = 0
        upgradingPerk = null
    }

    fun handleClaimBadge(item: ItemStack) {
        // im not writing a damn function to convert any roman numeral when it only goes up to 10 okayyy :P
        val romanNumerals: HashMap<String, Int> = hashMapOf(
            "I" to 1,
            "II" to 2,
            "III" to 3,
            "IV" to 4,
            "V" to 5,
            "VI" to 6,
            "VII" to 7,
            "VIII" to 8,
            "IX" to 9,
            "X" to 10
        )

        var stage = 1
        var badge = item.itemName.string

        romanNumerals.forEach { (roman, number) ->
            if (item.itemName.string.endsWith(" $roman")) {
                stage = number
                badge = item.itemName.string.dropLast(" $roman".length)
            }
        }

        val trophies = item.findLore(Regex("Stage Trophies:.+?(?<trophies>\\d+)"))?.get("trophies")?.value?.toIntOrNull() ?: 0

        val badgeTrophyGain = TrophyGain(
            type = TrophyType.SKILL,
            source = TrophySource.CLAIM_BADGE,
            trophies = trophies,
            timestamp = Clock.System.now().epochSeconds,
            data = badge,
            dataCount = stage
        )

        Galapagos.save.trophyHistory.add(badgeTrophyGain)
    }

    fun handleClaimFish(item: ItemStack) {
        val fishName = ConstantIslandData.data.fishSprites.keys.find { item.itemName.string.contains(it) } ?: return
        val weight = when(item.findLore(Regex("New Weight: (?<weight>.+)"))?.get("weight")?.value) {
            "Average" -> 1
            "Large" -> 2
            "Massive" -> 3
            "Gargantuan" -> 4
            "Colossal" -> 4
            else -> 1
        }
        val trophies = item.findLore(Regex("Trophies Earned:.+?(?<trophies>\\d+)"))?.get("trophies")?.value?.toIntOrNull() ?: 0

        val fishTrophyGain = TrophyGain(
            type = TrophyType.ANGLER,
            source = TrophySource.CLAIM_FISH,
            trophies = trophies,
            timestamp = Clock.System.now().epochSeconds,
            data = fishName,
            dataCount = weight
        )

        Galapagos.save.trophyHistory.add(fishTrophyGain)
    }

    fun royalReputationIncrease(cosmeticName: String, count: Int) {
        val cosmetic = Galapagos.save.cosmetics[cosmeticName] ?: return

        val repTrophyGain = TrophyGain(
            type = TrophyType.STYLE,
            source = TrophySource.ROYAL_REPUTATION,
            trophies = cosmetic.repPerDonation() * count,
            timestamp = Clock.System.now().epochSeconds,
            data = cosmetic.name,
            dataCount = count
        )

        Galapagos.save.trophyHistory.add(repTrophyGain)
    }


}