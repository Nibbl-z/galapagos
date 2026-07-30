package xyz.nibblz.galapagos.data

import kotlinx.serialization.Serializable

@Serializable
enum class StylePerk(val label: String, val slotID: Int, val arcanes: List<String>, val sprite: String) {
    LUCKY_METER("Lucky Meter", 11, listOf(), "island_interface/style_perks/lucky_claims.png"),
    GLITCHED_CLAIMS("Glitched Claims", 12, listOf("Abomination Mask", "Wizard Hat (Ember Mage)"), "island_interface/style_perks/glitched_claims.png"),
    EXPANDED_METER("Expanded Meter", 13, listOf("Wizard Cloak (Ember Mage)"), "island_interface/style_perks/expanded_meter.png"),
    EXPANDED_VAULT("Expanded Vault", 14, listOf("Abomination Robe"), "island_interface/style_perks/expanded_vault.png"),
    ARCANE_CLAIMS("Arcane Claims", 15, listOf(), "island_interface/style_perks/arcane_claims.png"),
    LUCKY_QUESTS("Lucky Quests", 20, listOf(), "island_interface/style_perks/lucky_quests.png"),
    BOOSTED_QUESTS("Boosted Quests", 21, listOf("Peacock Crown", "Tidal Lord Crown"), "island_interface/style_perks/boosted_quests.png"),
    EXPANDED_DAILIES("Expanded Dailies", 22, listOf("Tidal Lord Cloak"), "island_interface/style_perks/expanded_dailies.png"),
    EXPANDED_WEEKLIES("Expanded Weeklies", 23, listOf("Peacock Tail"), "island_interface/style_perks/expanded_weeklies.png"),
    ARCANE_QUESTS("Arcane Quests", 24, listOf(), "island_interface/style_perks/arcane_quests.png"),
    EFFICIENT_FUSION("Efficient Fusion", 29, listOf("Abomination Staff", "Wizard Staff (Ember Mage)"), "island_interface/style_perks/efficient_fusion.png"),
    EFFICIENT_ASSEMBLY("Efficient Assembly", 30, listOf("Peacock Staff", "Tidal Lord Staff"), "island_interface/style_perks/efficient_assembler.png"),
    EXPANDED_FORGE("Expanded Forge", 31, listOf(), "island_interface/style_perks/expanded_forge.png"),
    EXPANDED_ASSEMBLER("Expanded Assembler", 32, listOf(), "island_interface/style_perks/expanded_assembler.png"),
    ARCANE_ANOMALY("Arcane Anomaly", 33, listOf(), "island_items/infinibag/openable/arcane_anomaly.png") // I love this perk! this perk is cool.this is my favorite  perk. :3  ilove.arcane   anomaly! anomalyyy:3
}

val LUCKY_UPGRADE_CHANCES: List<HashMap<Rarity, Double>> = listOf(
    // Level 0
    hashMapOf(
        Rarity.COMMON to 0.60,
        Rarity.UNCOMMON to 0.25,
        Rarity.RARE to 0.10,
        Rarity.EPIC to 0.05
    ),
    // Level 1
    hashMapOf(
        Rarity.COMMON to 0.53,
        Rarity.UNCOMMON to 0.25,
        Rarity.RARE to 0.15,
        Rarity.EPIC to 0.07
    ),
    // Level 2
    hashMapOf(
        Rarity.COMMON to 0.41,
        Rarity.UNCOMMON to 0.30,
        Rarity.RARE to 0.25,
        Rarity.EPIC to 0.11
    ),
    // Level 3
    hashMapOf(
        Rarity.COMMON to 0.34,
        Rarity.UNCOMMON to 0.30,
        Rarity.RARE to 0.25,
        Rarity.EPIC to 0.11
    ),
    // Level 4
    hashMapOf(
        Rarity.COMMON to 0.22,
        Rarity.UNCOMMON to 0.35,
        Rarity.RARE to 0.30,
        Rarity.EPIC to 0.13
    ),
    // Level 5
    hashMapOf(
        Rarity.UNCOMMON to 0.48,
        Rarity.RARE to 0.35,
        Rarity.EPIC to 0.15,
        Rarity.LEGENDARY to 0.02
    ),
    // Level 6
    hashMapOf(
        Rarity.UNCOMMON to 0.39,
        Rarity.RARE to 0.40,
        Rarity.EPIC to 0.17,
        Rarity.LEGENDARY to 0.04
    ),
    // Level 7
    hashMapOf(
        Rarity.UNCOMMON to 0.25,
        Rarity.RARE to 0.50,
        Rarity.EPIC to 0.19,
        Rarity.LEGENDARY to 0.06
    ),
    // Level 8
    hashMapOf(
        Rarity.RARE to 0.70,
        Rarity.EPIC to 0.21,
        Rarity.LEGENDARY to 0.08,
        Rarity.MYTHIC to 0.01
    ),
    // Level 9
    hashMapOf(
        Rarity.RARE to 0.65,
        Rarity.EPIC to 0.23,
        Rarity.LEGENDARY to 0.10,
        Rarity.MYTHIC to 0.02
    ),
    // Level 10
    hashMapOf(
        Rarity.RARE to 0.60,
        Rarity.EPIC to 0.25,
        Rarity.LEGENDARY to 0.12,
        Rarity.MYTHIC to 0.03
    ),
)

// ok this isnt really a style perk but wtv

val WEEKLY_VAULT_CHANCES: HashMap<IntRange, HashMap<Rarity, Double>> = hashMapOf(
    0..4 to hashMapOf(
        Rarity.COMMON to 0.40,
        Rarity.UNCOMMON to 0.30,
        Rarity.RARE to 0.20,
        Rarity.EPIC to 0.10
    ),
    5..9 to hashMapOf(
        Rarity.COMMON to 0.30,
        Rarity.UNCOMMON to 0.35,
        Rarity.RARE to 0.23,
        Rarity.EPIC to 0.12
    ),
    10..14 to hashMapOf(
        Rarity.COMMON to 0.20,
        Rarity.UNCOMMON to 0.40,
        Rarity.RARE to 0.26,
        Rarity.EPIC to 0.14
    ),
    15..19 to hashMapOf(
        Rarity.UNCOMMON to 0.54,
        Rarity.RARE to 0.30,
        Rarity.EPIC to 0.16
    ),
    20..24 to hashMapOf(
        Rarity.UNCOMMON to 0.46,
        Rarity.RARE to 0.35,
        Rarity.EPIC to 0.18,
        Rarity.LEGENDARY to 0.01
    ),
    25..29 to hashMapOf(
        Rarity.UNCOMMON to 0.38,
        Rarity.RARE to 0.40,
        Rarity.EPIC to 0.20,
        Rarity.LEGENDARY to 0.02
    ),
    30..34 to hashMapOf(
        Rarity.UNCOMMON to 0.34,
        Rarity.RARE to 0.40,
        Rarity.EPIC to 0.22,
        Rarity.LEGENDARY to 0.04
    ),
    35..39 to hashMapOf(
        Rarity.UNCOMMON to 0.20,
        Rarity.RARE to 0.50,
        Rarity.EPIC to 0.24,
        Rarity.LEGENDARY to 0.06
    ),
    40..44 to hashMapOf(
        Rarity.RARE to 0.65,
        Rarity.EPIC to 0.26,
        Rarity.LEGENDARY to 0.08,
        Rarity.MYTHIC to 0.01
    ),
    45..49 to hashMapOf(
        Rarity.RARE to 0.60,
        Rarity.EPIC to 0.28,
        Rarity.LEGENDARY to 0.10,
        Rarity.MYTHIC to 0.02
    ),
    50..54 to hashMapOf(
        Rarity.RARE to 0.55,
        Rarity.EPIC to 0.30,
        Rarity.LEGENDARY to 0.12,
        Rarity.MYTHIC to 0.03
    ),
    55..59 to hashMapOf(
        Rarity.RARE to 0.48,
        Rarity.EPIC to 0.32,
        Rarity.LEGENDARY to 0.16,
        Rarity.MYTHIC to 0.04
    ),
    60..60 to hashMapOf(
        Rarity.RARE to 0.40,
        Rarity.EPIC to 0.35,
        Rarity.LEGENDARY to 0.20,
        Rarity.MYTHIC to 0.05
    ),
)

// this, too, also isn't really a style perk, however ARCANE_ANOMALY resides here so its ok
// excludes the item chances.. for now.. :P
val ARCANE_ANOMALY_CHANCES: HashMap<Rarity, Double> = hashMapOf(
    Rarity.COMMON to 0.135,
    Rarity.UNCOMMON to 0.135,
    Rarity.RARE to 0.09,
    Rarity.EPIC to 0.09,
    Rarity.LEGENDARY to 0.035,
    Rarity.MYTHIC to 0.025
)