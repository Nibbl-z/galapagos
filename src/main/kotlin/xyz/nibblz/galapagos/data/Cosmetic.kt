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
enum class CosmeticCollection(val label: String, val sprite: String, val bonus: Int) {
    ELEMENTAL("Elemental", "island_interface/crate_machine/pool/nature", 200),
    STANDARD_GAME("Standard Game", "island_items/game_pass/tgttos/1", 100),
    EXCLUSIVE_GAME("Exclusive Game", "island_items/game_pass/hitw/2", 200),
    EXCLUSIVE_SEASON("Exclusive Season", "island_items/infinibag/seasonal/token_s1", 300),
    EXCLUSIVE_VARIETY("Exclusive Variety", "island_items/infinibag/seasonal/token_variety", 100),
    GATE("Gate", "island_interface/navigator/arcane_gate", 100),
    FISHING("Fishing", "island_interface/fishing/perk_icon/speedy_rod", 100),
    PARTICLE("Particle", "island_interface/wardrobe/aura/icon", 50),
    BASIC_VENDOR("Basic Vendor", "island_interface/navigtor/pose", 50),
    ADVANCED_VENDOR("Advanced Vendor", "island_interface/wardrobe/pose_one", 100),
    TRIUMPH_VENDOR("Triumph Vendor", "island_interface/wardrobe/pose_two", 200),
    SPECIAL("Special", "island_interface/wardrobe/flag_golden", 0)
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
    val collection: CosmeticCollection,
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