package xyz.nibblz.islandeconomist.features

import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.world.item.Items
import xyz.nibblz.islandeconomist.IslandEconomist
import xyz.nibblz.islandeconomist.findLore

object CoinTracking : Feature {
    override val id = "coin_tracking"
    override val name = "Coin Tracking"

    override fun init() {}

    fun handleContainerContent(packet: ClientboundContainerSetContentPacket) {
        val screen = Minecraft.getInstance().screen
        if (screen?.title?.string?.contains("USING COINS?") == false) return

        var price = 0

        for (it in packet.items) {
            if (!it.`is`(Items.ECHO_SHARD)) continue
            val regex = Regex("(?<=/)\\d[\\d,]*")
            val match = it.findLore(regex) ?: continue
            val priceString = match[0] ?: continue

            val cleanedPriceString = priceString.value.replace(",", "")
            price = cleanedPriceString.toInt()
            break
        }

        IslandEconomist.logger.info(price.toString())
    }
}