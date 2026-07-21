package xyz.nibblz.galapagos.util

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import xyz.nibblz.galapagos.data.CosmeticTag
import xyz.nibblz.galapagos.data.Item
import kotlin.collections.forEach

fun ItemStack.findLore(regex: Regex): MatchGroupCollection? {
    val lore = this.getTooltipLines(
        net.minecraft.world.item.Item.TooltipContext.EMPTY,
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
        net.minecraft.world.item.Item.TooltipContext.EMPTY,
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

fun findLoreFromList(components: List<Component>, string: String): Boolean {
    components.forEach {
        if (it.string.contains(string)) {
            return true
        }
    }

    return false
}

fun ItemStack.findLores(regex: Regex): List<MatchGroupCollection> {
    val lore = this.getTooltipLines(
        net.minecraft.world.item.Item.TooltipContext.EMPTY,
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

fun ItemStack.toDataItem(): Item {
    val name = this.itemName.string

    val regex = Regex("Amount: (?<amount>[\\d,]+)")
    val amountString = this.findLore(regex)?.get("amount")?.value ?: this.count.toString()
    val cleanedString = amountString.replace(",", "")
    val count = cleanedString.toInt()

    return Item(
        name = name,
        count = count,
        isCosmeticToken = name.contains("Token") && !name.contains("Blueprint:") && !name.contains("MCC+")
    )
}

fun ItemStack.getCosmeticTag(): CosmeticTag {
    if (this.findLore(Glyphs.getGlyph("_fonts/icon/tooltips/exclusive.png"))) return CosmeticTag.EXCLUSIVE
    if (this.findLore(Glyphs.getGlyph("_fonts/icon/tooltips/arcane.png"))) return CosmeticTag.ARCANE
    return CosmeticTag.STANDARD
}