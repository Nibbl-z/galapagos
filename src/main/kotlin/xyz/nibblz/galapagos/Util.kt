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

fun ItemStack.getCosmetic(): Cosmetic? {
    val chanceRegex = Regex("Chance: (?<chance>[\\d,.]+)%")
    val chanceString = this.findLore(chanceRegex)?.get("chance")?.value ?: return null
    val chance = chanceString.toDouble()

    val isOwned = this.findLore("Royal Donations:")
    var trophies = 10

    Rarity.entries.forEach {
        if (this.findLore(it.tooltipGlyph())) {
            trophies = it.trophies
        }
    }

    val isExclusive = this.findLore(Glyphs.getGlyph("_fonts/icon/tooltips/exclusive.png"))

    val donations = if (isOwned) {
        val repRegex = Regex("Royal Donations: (?<rep>\\d+)")
        val repString = this.findLore(repRegex)?.get("rep")?.value ?: return null
        repString.toInt()
    } else 0
    val trophiesPerDonation = trophies / (if (isExclusive) 5 else 10)

    val rep = donations * trophiesPerDonation

    return Cosmetic(
        chance = chance,
        isOwned = isOwned,
        trophies = trophies,
        rep = rep
    )
}

fun BlueprintLootPreview.update(cosmetics: List<Cosmetic>) {
    this.newCosmeticChance = 0.0
    this.currentTrophies = 0
    this.totalTrophies = 0
    this.newRepChance = 0.0
    this.currentRep = 0
    this.totalRep = 0

    var totalNewCosmeticChance = 0.0
    var totalNewRepChance = 0.0

    cosmetics.forEach {
        if (!it.isOwned) {
            this.newCosmeticChance += it.chance
        } else {
            this.currentTrophies += it.trophies
        }

        if (it.trophies != it.rep) {
            this.newRepChance += it.chance
        }

        this.totalTrophies += it.trophies
        this.currentRep += it.rep
        this.totalRep += it.trophies

        totalNewCosmeticChance += it.chance
        totalNewRepChance += it.chance
    }

    // this is needed because the chances for some crates are rounded and don't add up to 100% (eg. the mythic crate)
    this.newCosmeticChance = (this.newCosmeticChance / totalNewCosmeticChance) * 100.0
    this.newRepChance = (this.newRepChance / totalNewRepChance) * 100.0
}

fun BlueprintLootPreview.render(graphics: GuiGraphicsExtractor, x: Int, y: Int, w: Int) {
    graphics.text(Minecraft.getInstance().font, Component.literal("New Cosmetic: ${"%.2f".format(this.newCosmeticChance)}%"), x + w + 2, y + 30, ARGB.opaque(0x66fc56), true)
    graphics.text(Minecraft.getInstance().font, Component.literal("${this.currentTrophies}/${this.totalTrophies} ").append(
        Glyphs.getGlyphComponent("_fonts/icon/trophy/purple.png")), x + w + 2, y + 45, ARGB.opaque(0x66fc56), true)
    graphics.text(Minecraft.getInstance().font, Component.literal("New Rep: ${"%.2f".format(this.newRepChance)}%"), x + w + 2, y + 60, ARGB.opaque(0x9143f0), true)
    graphics.text(Minecraft.getInstance().font, Component.literal("${this.currentRep}/${this.totalRep} ").append(
        Glyphs.getGlyphComponent("_fonts/icon/royal_reputation.png")), x + w + 2, y + 75, ARGB.opaque(0x9143f0), true)
}
