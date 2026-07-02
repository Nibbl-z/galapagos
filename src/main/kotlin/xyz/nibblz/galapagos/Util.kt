package xyz.nibblz.galapagos

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import xyz.nibblz.galapagos.data.BlueprintLootPreview
import xyz.nibblz.galapagos.data.Cosmetic
import xyz.nibblz.galapagos.data.Rarity

// stealing from devcmb stealing from pe3ep part 1
// https://github.com/pe3ep/Trident/blob/master/src/main/kotlin/cc/pe3epwithyou/trident/state/MCCIState.kt
fun onIsland(): Boolean {
    val server = Minecraft.getInstance().currentServer ?: return false
    return server.ip.contains("mccisland.net", true)
}

fun ItemStack.findLore(regex: Regex): MatchGroupCollection? {
    val lore = this.getTooltipLines(
        Item.TooltipContext.EMPTY,
        Minecraft.getInstance().player,
        TooltipFlag.Default.NORMAL
    )
    // w mojang

    lore.forEach {
        val match = regex.find(it.string) ?: return@forEach
        return@findLore match.groups
    }

    return null
}

fun ItemStack.findLore(string: String): Boolean {
    val lore = this.getTooltipLines(
        Item.TooltipContext.EMPTY,
        Minecraft.getInstance().player,
        TooltipFlag.Default.NORMAL
    )

    lore.forEach {
        if (it.string.contains(string)) {
            return@findLore true
        }
    }

    return false
}

fun ItemStack.findLores(regex: Regex): List<MatchGroupCollection> {
    val lore = this.getTooltipLines(
        Item.TooltipContext.EMPTY,
        Minecraft.getInstance().player,
        TooltipFlag.Default.NORMAL
    )
    // w mojang

    val matches: MutableList<MatchGroupCollection> = mutableListOf()

    lore.forEach {
        val match = regex.find(it.string) ?: return@forEach
        matches.add(match.groups)
    }

    return matches
}

fun ItemStack.toDataItem(): PlayerData.Item {
    val name = this.itemName.string

    val regex = Regex("Amount: (?<amount>[\\d,]+)")
    val amountString = this.findLore(regex)?.get("amount")?.value ?: this.count.toString()
    val cleanedString = amountString.replace(",", "")
    val count = cleanedString.toInt()

    return PlayerData.Item(
        name = name,
        count = count,
        isCosmeticToken = name.contains("Token") && !name.contains("Blueprint:") && !name.contains("MCC+")
    )
}
