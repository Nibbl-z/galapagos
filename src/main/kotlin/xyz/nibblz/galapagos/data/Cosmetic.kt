package xyz.nibblz.galapagos.data

import kotlinx.serialization.Serializable
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import xyz.nibblz.galapagos.Galapagos

@Serializable
enum class CosmeticTag(val maxDonations: Int, val core: CosmeticCore, val bonusCore: CosmeticCore) {
    STANDARD(10, CosmeticCore.STANDARD, CosmeticCore.MYTHIC),
    EXCLUSIVE(5, CosmeticCore.EXCLUSIVE, CosmeticCore.ARCANE),
    ARCANE(5, CosmeticCore.EXCLUSIVE, CosmeticCore.ARCANE)
}

@Serializable
enum class Collection(val label: String) {
    ELEMENTAL("Elemental"),
    STANDARD_GAME("Standard Game"),
    EXCLUSIVE_GAME("Exclusive Game"),
    EXCLUSIVE_SEASON("Exclusive Season"),
    EXCLUSIVE_VARIETY("Exclusive Variety"),
    GATE("Gate"),
    FISHING("Fishing")
}

enum class CosmeticCore(val label: String, val color: Int, val glyph: String) {
    STANDARD("Standard Core", Rarity.UNCOMMON.color, "\uE005"),
    EXCLUSIVE("Exclusive Core", 0xfbff82, "\uE006"),
    MYTHIC("Mythic Core", Rarity.MYTHIC.color, "\uE003"),
    ARCANE("Arcane Core", ChatFormatting.LIGHT_PURPLE.color!!, "\uE004");

    fun getComponent(): MutableComponent {
        return Component.literal(this.glyph).withColor(0xffffff).withStyle(Style.EMPTY.withFont(Galapagos.font))
    }
}

// input core to output core = how many output cores per input core
val coreConversions: HashMap<Pair<CosmeticCore, CosmeticCore>, Double> = hashMapOf(
    (CosmeticCore.STANDARD to CosmeticCore.EXCLUSIVE) to 0.1,
    (CosmeticCore.EXCLUSIVE to CosmeticCore.STANDARD) to 10.0,

    (CosmeticCore.MYTHIC to CosmeticCore.STANDARD) to 25.0,
    (CosmeticCore.MYTHIC to CosmeticCore.EXCLUSIVE) to 2.5,
    (CosmeticCore.MYTHIC to CosmeticCore.ARCANE) to 0.05,

    (CosmeticCore.ARCANE to CosmeticCore.MYTHIC) to 10.0,
    (CosmeticCore.ARCANE to CosmeticCore.STANDARD) to 250.0,
    (CosmeticCore.ARCANE to CosmeticCore.EXCLUSIVE) to 25.0
)

@Serializable
data class Cosmetic(
    val name: String,
    val collection: Collection,
    val tag: CosmeticTag,
    var isOwned: Boolean,
    var donations: Int,
    val rarity: Rarity,
    val isColorable: Boolean,
    var isColored: Boolean
)

fun Cosmetic.getRep(): Int {
    return this.donations * this.repPerDonation()
}

fun Cosmetic.repPerDonation(): Int {
    return when(this.tag) {
        CosmeticTag.STANDARD -> this.rarity.trophies / 10
        CosmeticTag.EXCLUSIVE -> this.rarity.trophies / 5
        CosmeticTag.ARCANE -> 30
    }
}

fun Cosmetic.coresPerScavenge(): Int {
    return when(this.tag) {
        CosmeticTag.STANDARD -> when(this.rarity) {
            Rarity.COMMON -> 1
            Rarity.UNCOMMON -> 3
            Rarity.RARE -> 7
            Rarity.EPIC -> 15
            Rarity.LEGENDARY -> 35
            Rarity.MYTHIC -> 100
        }
        CosmeticTag.EXCLUSIVE -> when(this.rarity) {
            Rarity.RARE -> 2
            Rarity.EPIC -> 5
            Rarity.LEGENDARY -> 10
            Rarity.MYTHIC -> 30
            else -> 0
        }
        CosmeticTag.ARCANE -> 50
    }
}

fun Cosmetic.bonusCoresPerScavenge(): Double {
    return when(this.tag) {
        CosmeticTag.STANDARD -> when(this.rarity) {
            Rarity.COMMON -> 0.03
            Rarity.UNCOMMON -> 0.1
            Rarity.RARE -> 0.25
            Rarity.EPIC -> 0.5
            Rarity.LEGENDARY -> 1.0
            Rarity.MYTHIC -> 2.0
        } * if (this.donations == 10) 2.0 else 1.0
        CosmeticTag.EXCLUSIVE -> when(this.rarity) {
            Rarity.RARE -> 0.06
            Rarity.EPIC -> 0.15
            Rarity.LEGENDARY -> 0.30
            Rarity.MYTHIC -> 1.0
            else -> 0.0
        } * if (this.donations == 5) 2.0 else 1.0
        CosmeticTag.ARCANE -> 1.0 * if (this.donations == 5) 2.0 else 1.0
    }
}