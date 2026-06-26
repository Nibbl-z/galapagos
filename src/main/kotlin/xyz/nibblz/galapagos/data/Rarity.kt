package xyz.nibblz.galapagos.data

import xyz.nibblz.galapagos.Glyphs

enum class Rarity(val label: String) {
    COMMON("Common"),
    UNCOMMON("Uncommon"),
    RARE("Rare"),
    EPIC("Epic"),
    LEGENDARY("Legendary"),
    MYTHIC("Mythic");

    fun tooltipGlyph(): String {
        return Glyphs.getGlyph("_fonts/icon/tooltips/${this.name.lowercase()}.png")
    }
}