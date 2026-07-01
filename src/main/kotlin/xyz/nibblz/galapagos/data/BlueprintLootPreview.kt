package xyz.nibblz.galapagos.data

import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.util.ARGB
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.Glyphs

data class BlueprintLootPreview(
    var newCosmeticChance: Double = 0.0,
    var currentTrophies: Int = 0,
    var totalTrophies: Int = 0,
    var newRepChance: Double = 0.0,
    var currentRep: Int = 0,
    var totalRep: Int = 0,
    var trophiesPerRoll: Double = 0.0,
    var mythicCoresPerRoll: Double = 0.0,
    var arcaneCoresPerRoll: Double = 0.0
)

fun BlueprintLootPreview.update(cosmetics: List<Cosmetic>) {
    this.newCosmeticChance = 0.0
    this.currentTrophies = 0
    this.totalTrophies = 0
    this.newRepChance = 0.0
    this.currentRep = 0
    this.totalRep = 0
    this.trophiesPerRoll = 0.0
    this.mythicCoresPerRoll = 0.0
    this.arcaneCoresPerRoll = 0.0

    var totalChance = 0.0

    cosmetics.forEach { totalChance += it.chance }

    cosmetics.forEach {
        val fixedChance = it.chance / totalChance

        this.mythicCoresPerRoll += it.mythicCores * fixedChance
        this.arcaneCoresPerRoll += it.arcaneCores * fixedChance

        if (!it.isOwned) {
            this.newCosmeticChance += fixedChance
            this.trophiesPerRoll += it.trophies * fixedChance
        } else {
            this.currentTrophies += it.trophies
        }

        if (it.trophies != it.rep) {
            this.newRepChance += fixedChance
            if (it.isOwned) this.trophiesPerRoll += it.perDonation * fixedChance
        }

        this.totalTrophies += it.trophies
        this.currentRep += it.rep
        this.totalRep += it.trophies
    }

    this.newCosmeticChance *= 100.0
    this.newRepChance *= 100.0
}

fun BlueprintLootPreview.render(graphics: GuiGraphicsExtractor, x: Int, y: Int, w: Int) {
    graphics.text(Minecraft.getInstance().font, Component.literal("New Cosmetic: ${"%.2f".format(this.newCosmeticChance)}%"), x + w + 2, y + 30, ARGB.opaque(0x66fc56), true)
    graphics.text(Minecraft.getInstance().font, Component.literal("${this.currentTrophies}/${this.totalTrophies} ").append(
        Glyphs.getGlyphComponent("_fonts/icon/trophy/purple.png")), x + w + 2, y + 45, ARGB.opaque(0x66fc56), true)
    graphics.text(Minecraft.getInstance().font, Component.literal("New Rep: ${"%.2f".format(this.newRepChance)}%"), x + w + 2, y + 60, ARGB.opaque(0x9143f0), true)
    graphics.text(Minecraft.getInstance().font, Component.literal("${this.currentRep}/${this.totalRep} ").append(
        Glyphs.getGlyphComponent("_fonts/icon/royal_reputation.png")), x + w + 2, y + 75, ARGB.opaque(0x9143f0), true)
}

fun BlueprintLootPreview.newCosmeticTooltip(): Component {
    return Component.literal("New Cosmetic: ").withColor(ChatFormatting.GRAY.color!!)
        .append(Component.literal("${"%.2f".format(this.newCosmeticChance)}%").withColor(ChatFormatting.GREEN.color!!))
        .append(Component.literal(" [").withColor(ChatFormatting.GRAY.color!!))
        .append(Component.literal("${this.currentTrophies}/${this.totalTrophies} ").withColor(ChatFormatting.GREEN.color!!))
        .append(Glyphs.getGlyphComponent("_fonts/icon/trophy/purple.png"))
        .append(Component.literal("]").withColor(ChatFormatting.GRAY.color!!))
}

fun BlueprintLootPreview.newRepTooltip(): Component {
    return Component.literal("New Royal Rep: ").withColor(ChatFormatting.GRAY.color!!)
        .append(Component.literal("${"%.2f".format(this.newRepChance)}%").withColor(0x9143f0))
        .append(Component.literal(" [").withColor(ChatFormatting.GRAY.color!!))
        .append(Component.literal("${this.currentRep}/${this.totalRep} ").withColor(0x9143f0))
        .append(Glyphs.getGlyphComponent("_fonts/icon/royal_reputation.png"))
        .append(Component.literal("]").withColor(ChatFormatting.GRAY.color!!))
}

fun BlueprintLootPreview.trophiesPerRollTooltip(): Component {
    return Component.literal("Average Trophies/Roll: ").withColor(ChatFormatting.GRAY.color!!)
        .append(Component.literal("%.3f ".format(this.trophiesPerRoll)).withColor(ChatFormatting.GREEN.color!!))
        .append(Glyphs.getGlyphComponent("_fonts/icon/trophy/purple.png"))

}

fun BlueprintLootPreview.mythicCoresPerRollTooltip(): Component {
    return Component.literal("Average Mythic Cores/Roll: ").withColor(ChatFormatting.GRAY.color!!)
        .append(Component.literal("%.3f ".format(this.mythicCoresPerRoll)).withColor(ChatFormatting.RED.color!!))
        .append(Component.literal("\uE003").withColor(0xffffff).withStyle(Style.EMPTY.withFont(Galapagos.font)))
}

fun BlueprintLootPreview.arcaneCoresPerRollTooltip(): Component {
    return Component.literal("Average Arcane Cores/Roll: ").withColor(ChatFormatting.GRAY.color!!)
        .append(Component.literal("%.3f ".format(this.arcaneCoresPerRoll)).withColor(ChatFormatting.LIGHT_PURPLE.color!!))
        .append(Component.literal("\uE004").withColor(0xffffff).withStyle(Style.EMPTY.withFont(Galapagos.font)))
}