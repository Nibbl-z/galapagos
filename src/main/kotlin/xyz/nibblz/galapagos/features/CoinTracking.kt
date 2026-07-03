package xyz.nibblz.galapagos.features

import kotlinx.serialization.Serializable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.events.ContainerCloseEvent
import xyz.nibblz.galapagos.events.ContainerOpenEvent
import xyz.nibblz.galapagos.events.SlotClickEvent
import xyz.nibblz.galapagos.findLore
import xyz.nibblz.galapagos.mixin.accessor.HoveredSlotAccessor
import kotlin.time.Clock

object CoinTracking : Feature {
    override val id = "coin_tracking"
    override val name = "Coin Tracking"

    override fun init() {
        ContainerOpenEvent.EVENT.register { packet -> handleContainerContent(packet) }
        ContainerCloseEvent.EVENT.register { handleContainerClose() }
        SlotClickEvent.EVENT.register { screen, input -> slotClick(screen, input) }
    }

    var price = 0
    var shiftClickAmount = 0
    var source = "Unknown"
    var rewardCrate: String? = null

    fun handleContainerContent(packet: ClientboundContainerSetContentPacket) {
        val screen = Minecraft.getInstance().screen
        if (screen?.title?.string?.contains("USING COINS?") == true) {
            handleCoinPurchase(packet)
        }

        if (screen?.title?.string?.contains("SUMMARY") == true) {
            handleCoinGain(packet)
        }
    }

    fun handleCoinPurchase(packet: ClientboundContainerSetContentPacket) {
        for ((i, it) in packet.items.withIndex()) {
            if (i == 28) {
                source = it.itemName.string
            }

            if (!it.`is`(Items.ECHO_SHARD)) continue

            val regex =
                if (it.itemName.string == "Purchase Confirmation") Regex("[\\d,]+")
                else Regex("(?<=/)\\d[\\d,]+")

            Galapagos.logger.info("$shiftClickAmount")

            val match = it.findLore(regex) ?: continue
            val priceString = match[0] ?: continue
            Galapagos.logger.info(match.toString())
            val cleanedPriceString = priceString.value.replace(",", "")
            price = cleanedPriceString.toInt()
            break
        }

        Galapagos.logger.info("$price, $source")
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

            if (shiftClickAmount != 0) {
                rewardCrate += " x$shiftClickAmount"
            }

            val change = CoinChange(
                amount = amountGained,
                timestamp = Clock.System.now().epochSeconds,
                source = rewardCrate ?: "Unknown"
            )

            Galapagos.save.coinChanges.add(change)
            rewardCrate = null
            break
        }
    }

    fun fetchShiftClickAmount(item: ItemStack) {
        val regex =
            if (item.itemName.string.contains("Reward Crate")) Regex("(?<=Shift-Click to Open All )\\d+")
            else Regex("(?<=Shift-Left-Click to Buy )\\d+")

        val match = item.findLore(regex) ?: return
        val priceString = match[0] ?: return
        val cleanedPriceString = priceString.value.replace(",", "")
        shiftClickAmount = cleanedPriceString.toInt()
    }

    fun slotClick(screen: ContainerScreen, type: ContainerInput) {
        val item = (screen as HoveredSlotAccessor).`galapagos$hoveredSlot`() ?: return

        Galapagos.logger.info("${item.index}, ${item.item.itemName.string}, $type")

        if (price == 0 && rewardCrate == null) {
            if (item.item.itemName.string.contains("Reward Crate")) {
                rewardCrate = item.item.itemName.string
            }

            if (type == ContainerInput.QUICK_MOVE) { // quick move is a  shift click !
                fetchShiftClickAmount(item.item)
            } else {
                shiftClickAmount = 0
            }

            return
        } else {

        }

        if (item.index in 46..48) {
            Galapagos.logger.info("this item has apparently been purchased! i've spent $price coins!")

            if (shiftClickAmount != 0) {
                price *= shiftClickAmount
                source += " x$shiftClickAmount"
            }

            val change = CoinChange(
                amount = -price,
                timestamp = Clock.System.now().epochSeconds,
                source = source
            )

            Galapagos.save.coinChanges.add(change)

            price = 0
            source = "Unknown"
            shiftClickAmount = 0
        }
    }

    fun handleContainerClose() {
        price = 0
        rewardCrate = null
        source = "Unknown"
        Galapagos.logger.info("this ui has apparently been closed! (which is probably any ui)")
    }

    @Serializable
    data class CoinChange(
        var amount: Int = 0,
        var timestamp: Long = 0,
        var source: String = "Unknown"
    )
}