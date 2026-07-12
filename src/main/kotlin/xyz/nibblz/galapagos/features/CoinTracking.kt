package xyz.nibblz.galapagos.features

import kotlinx.serialization.Serializable
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.events.ContainerCloseEvent
import xyz.nibblz.galapagos.events.ContainerOpenEvent
import xyz.nibblz.galapagos.events.SlotClickEvent
import xyz.nibblz.galapagos.events.SystemChatEvent
import xyz.nibblz.galapagos.mixin.accessor.HoveredSlotAccessor
import xyz.nibblz.galapagos.screens.CoinHistory
import xyz.nibblz.galapagos.util.Glyphs
import xyz.nibblz.galapagos.util.PlayerData
import xyz.nibblz.galapagos.util.findLore
import xyz.nibblz.galapagos.util.playMccSound
import java.util.*
import kotlin.reflect.KMutableProperty0
import kotlin.time.Clock

object CoinTracking : Feature {
    override val id = "coin_tracking"
    override val name = "Coin Tracking"
    override val description: List<Component> = listOf(
        Component.literal("Logs all gains and losses of coins across many aspects of the server, including: "),
        Component.literal("- Reward Crates"),
        Component.literal("- Island Exchange"),
        Component.literal("- Trading"),
        Component.literal("- Purchasing items from vendors"),
        Component.literal("- Mailbox"),
        Component.literal("- Scavenging"),
        Component.empty(),
        Component.literal("To view your coin history, click the ")
            .append(Component.literal("Coins").withColor(ChatFormatting.YELLOW.color!!))
            .append(Component.literal(" item in your Infinibag.")),
        Component.empty(),
        Component.literal("Note: Disabling this feature will NOT disable coin tracking, but will disable the coin history menu.")
    )
    override val enabledProperty: KMutableProperty0<Boolean> = Config.values::coinTrackingEnabled
    override val image: Config.ConfigImage = Config.ConfigImage("coin_tracking.png", 1021, 456)


    override fun init() {
        ContainerCloseEvent.EVENT.register { containerClose() }
        SlotClickEvent.EVENT.register { screen, input, _, button -> slotClick(screen, input, button) }
        SystemChatEvent.EVENT.register { packet -> systemChat(packet) }
        ContainerOpenEvent.EVENT.register { packet -> containerOpen(packet) }
        ItemTooltipCallback.EVENT.register { stack, _, _, components -> tooltipAdd(stack, components) }
        ClientTickEvents.END_CLIENT_TICK.register {
            if (!openCoinHistory) return@register
            openCoinHistory = false
            Minecraft.getInstance().setScreen(CoinHistory())
        }
    }

    var price = 0
    var category = CoinChangeCategory.UNKNOWN
    var data = ""
    var dataCount = 0
    var clickedCoinHistory = false
    var openCoinHistory = false

    val filter = CoinChangeCategory.entries.associateWithTo(EnumMap(CoinChangeCategory::class.java)) {true}

    fun resetData() {
        price = 0
        category = CoinChangeCategory.UNKNOWN
        data = ""
        dataCount = 0
    }

    fun containerOpen(packet: ClientboundContainerSetContentPacket) {
        val screen = Minecraft.getInstance().screen ?: return
        if (screen.title.string.contains("USING COINS?")) {
            handleCoinPurchase(packet)
        }

        if (screen.title.string.contains("SUMMARY")) {
            handleCoinGain(packet)
        }

        if (screen.title.string.contains("PLAYER TRADE")) {
            category = CoinChangeCategory.TRADING
        }
    }

    fun systemChat(packet: ClientboundSystemChatPacket) {
        if (packet.content.string.contains("trade request")) {
            handleTradeStart(packet.content.string)
        }

        if (packet.content.string.contains("Trade Completed!")) {
            if (price == 0) return

            val change = CoinChange(
                amount = price,
                timestamp = Clock.System.now().epochSeconds,
                category = category,
                data = data,
                dataCount = dataCount
            )

            Galapagos.save.coinChanges.add(change)
            resetData()
        }
    }

    fun handleTradeStart(content: String) {
        val regex = if (content.contains("Click to Accept"))
            Regex("You have received a trade request from (?<player>.+)\\.")
        else Regex("You have sent a trade request to (?<player>.+)\\.")

        val player = regex.find(content)?.groups["player"]?.value ?: return
        Galapagos.logger.info("Trade with $player !!!")

        data = player
    }

    fun handleCoinPurchase(packet: ClientboundContainerSetContentPacket) {
        for ((i, it) in packet.items.withIndex()) {
            if (i == 28) {
                data = it.itemName.string
            }

            if (!it.`is`(Items.ECHO_SHARD)) continue

            val regex =
                if (it.itemName.string == "Purchase Confirmation") Regex("[\\d,]+")
                else Regex("(?<=/)\\d[\\d,]+")

            val match = it.findLore(regex) ?: continue
            val priceString = match[0] ?: continue
            Galapagos.logger.info(match.toString())
            val cleanedPriceString = priceString.value.replace(",", "")
            price = cleanedPriceString.toInt()

            category = if (it.itemName.string == "Purchase Confirmation") CoinChangeCategory.ISLAND_EXCHANGE else CoinChangeCategory.ITEM

            break
        }
    }

    fun handleCoinGain(packet: ClientboundContainerSetContentPacket) {
        for (it in packet.items) {
            if (!it.`is`(Items.ECHO_SHARD)) continue
            if (it.itemName.string != "Coins") continue

            val regex = Regex("(?<=: )\\d[\\d,]+")
            val match = it.findLore(regex) ?: continue
            val priceString = match[0] ?: continue
            val cleanedPriceString = priceString.value.replace(",", "")
            val amountGained = cleanedPriceString.toInt()

            Galapagos.logger.info("$amountGained")

            val change = CoinChange(
                amount = amountGained,
                timestamp = Clock.System.now().epochSeconds,
                category = category,
                data = data,
                dataCount = dataCount
            )

            Galapagos.save.coinChanges.add(change)
            resetData()

            break
        }
    }

    fun fetchShiftClickAmount(item: ItemStack): Int {
        val regex =
            if (item.itemName.string.contains("Reward Crate")) Regex("(?<=Shift-Click to Open All )\\d+")
            else Regex("(?<=Shift-Left-Click to Buy )\\d+")

        val match = item.findLore(regex) ?: return 1
        val countString = match[0] ?: return 1
        val cleanedCountString = countString.value.replace(",", "")
        return cleanedCountString.toInt()
    }

    fun slotClick(screen: ContainerScreen, type: ContainerInput, button: Int) {
        val slot = (screen as HoveredSlotAccessor).`galapagos$hoveredSlot`() ?: return

        Galapagos.logger.info("${slot.index}, ${slot.item.itemName.string}, $type")

        if (slot.item.itemName.string == "Coins" && screen.title.string.contains("INFINIBAG") && button == 0) {
            if (!enabledProperty.get()) return
            clickedCoinHistory = true
            playMccSound("ui.click_normal")
            playMccSound("ui.pickup_coins")
            Minecraft.getInstance().connection!!.send(ServerboundContainerClosePacket(Minecraft.getInstance().player!!.containerMenu.containerId))
            return
        }

        if (slot.index in 46..48 && screen.title.string.contains("SCAVENGING WILL PERMANENTLY")) {
            Galapagos.logger.info("this is a scavenge!")
            category = CoinChangeCategory.SCAVENGE
            return
        }

        if (slot.index in 64..66 && screen.title.string.contains("PLAYER TRADE")) {
            val outgoingSlots = listOf(28, 29, 30, 37, 38, 39, 46, 47, 48)
            val incomingSlots = listOf(32, 33, 34, 41, 42, 43, 50, 51, 52)
            val regex = Regex("Amount: (?<coins>[\\d,]+)")

            price = 0

            (outgoingSlots + incomingSlots).forEach {
                val item = screen.menu.slots[it].item
                if (item.itemName.string != "Coins") return@forEach
                val coinString = item.findLore(regex)?.get("coins")?.value ?: return@forEach
                val coins = coinString.replace(",", "").toIntOrNull() ?: return@forEach

                if (outgoingSlots.contains(it)) price -= coins else price += coins
            }

            return
        }

        if (price == 0 && data.isEmpty()) {
            if (slot.item.itemName.string.contains("Reward Crate")) {
                category = CoinChangeCategory.REWARD_CRATE
                data = slot.item.itemName.string.dropLast(13) // data only takes in rarity, so remove the " Reward Crate" suffix
            }

            if (screen.title.string.contains("MAILBOX")) {
                if (slot.item.itemName.string.contains("Listing Coin Delivery")) {
                    category = CoinChangeCategory.ISLAND_EXCHANGE

                    val regex = Regex("Item Sold: \\[(?<name>.+)]")
                    val itemName = slot.item.findLore(regex)?.get("name")?.value ?: "Unknown"
                    data = itemName
                } else {
                    category = CoinChangeCategory.MAILBOX
                    data = slot.item.itemName.string
                }
            }

            dataCount = if (type == ContainerInput.QUICK_MOVE) fetchShiftClickAmount(slot.item) else 1

            return
        }

        // appears when clicking buy button
        if (slot.index in 46..48) {
            if (dataCount > 1) {
                price *= dataCount
            }

            val change = CoinChange(
                amount = -price,
                timestamp = Clock.System.now().epochSeconds,
                category = category,
                data = data,
                dataCount = dataCount
            )

            Galapagos.save.coinChanges.add(change)
            resetData()
        }
    }

    fun tooltipAdd(stack: ItemStack, components: MutableList<Component>) {
        if (!enabledProperty.get()) return
        val screen = Minecraft.getInstance().screen ?: return
        if (!screen.title.string.contains("INFINIBAG")) return
        if (stack.itemName.string != "Coins") return

        var index = components.indexOfFirst { it.string.contains("minecraft:") } // if you have f3+h on :P
        if (index == -1) { index = components.size - 1 } // if you dont !

        components.add(index, Component.empty())

        components.add(index + 1, Component.empty()
            .append(Glyphs.getGlyphComponent("_fonts/icon/click_action_left.png"))
            .append(Component.literal(" > ").withColor(ChatFormatting.DARK_GRAY.color!!))
            .append(Component.literal("Click to ").withColor(0xecd584))
            .append(Component.literal("View History").withColor(0xfee761)))
    }

    fun containerClose() {
        resetData()

        if (!clickedCoinHistory) return
        clickedCoinHistory = false
        openCoinHistory = true
    }

    @Serializable
    enum class CoinChangeCategory(val label: String, val hide: Boolean = false) {
        REWARD_CRATE("Reward Crate"),
        ISLAND_EXCHANGE("Island Exchange"),
        MAILBOX("Mailbox"),
        TRADING("Trading"),
        ITEM("Items"),
        STYLE_PERK("Style Perks", true), // these are hidden because i havent implemented them yet :steamhappy: ill do it later
        BID("Auction Bid", true),
        SCAVENGE("Scavenging"),
        UNKNOWN("Unknown")
    }

    @Serializable
    data class CoinChange(
        var amount: Int = 0,
        var timestamp: Long = 0,
        var category: CoinChangeCategory = CoinChangeCategory.UNKNOWN,
        var data: String = "?",
        var dataCount: Int = 0
    )

    fun CoinChange.getIcon(): String {
        return when(this.category) {
            CoinChangeCategory.REWARD_CRATE -> "island_items/infinibag/openable/questing_crate_${this.data.lowercase()}.png"
            CoinChangeCategory.ISLAND_EXCHANGE -> "island_interface/navigator/island_exchange.png"
            CoinChangeCategory.MAILBOX -> "island_interface/misc/mailbox.png"
            CoinChangeCategory.TRADING -> "island_interface/social/send_trade_request.png" // this would have to do something special if i want it to be the player head..
            CoinChangeCategory.ITEM -> when(this.data) {
                "Magical Material Crate" -> "island_items/infinibag/openable/material_crate_magical.png"
                "Mechanical Material Crate" -> "island_items/infinibag/openable/material_crate_mechanical.png"
                "Natural Material Crate" -> "island_items/infinibag/openable/material_crate_nature.png"
                "Oceanic Material Crate" -> "island_items/infinibag/openable/material_crate_oceanic.png"

                "Basic Cosmetic Key" -> "island_items/infinibag/key/basic.png"
                "Ultimate Cosmetic Key" -> "island_items/infinibag/key/ultimate.png"

                "Cleansing Powder" -> "island_items/infinibag/material/cleansing_powder.png"

                else -> "island_interface/generic/question_mark.png"
            }
            CoinChangeCategory.STYLE_PERK -> {
                val perk = PlayerData.StylePerk.valueOf(this.data)
                perk.sprite
            }
            CoinChangeCategory.BID -> "island_interface/wardrobe/collector.png"
            CoinChangeCategory.SCAVENGE -> "island_interface/navigator/scavenging.png"
            CoinChangeCategory.UNKNOWN -> "island_interface/generic/question_mark.png"
        }
    }

    fun CoinChange.getSource(): String {
        return when(this.category) {
            CoinChangeCategory.REWARD_CRATE -> "${this.data} Reward Crate${if (this.dataCount > 1) " x${this.dataCount}" else ""}"
            CoinChangeCategory.ISLAND_EXCHANGE -> "${if (this.amount > 0) "Sold" else "Bought"} ${this.data}"
            CoinChangeCategory.MAILBOX -> this.data
            CoinChangeCategory.TRADING -> "Trade with ${this.data}"
            CoinChangeCategory.ITEM -> "${this.data}${if (this.dataCount > 1) " x${this.dataCount}" else ""}"
            CoinChangeCategory.STYLE_PERK -> {
                val perk = PlayerData.StylePerk.valueOf(this.data)
                "Upgraded ${perk.label} to level ${this.dataCount}"
            }
            CoinChangeCategory.BID -> "Bid on ${this.data}"
            CoinChangeCategory.SCAVENGE -> "Scavenging"
            CoinChangeCategory.UNKNOWN -> "Unknown"
        }
    }
}