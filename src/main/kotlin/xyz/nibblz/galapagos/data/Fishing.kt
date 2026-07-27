package xyz.nibblz.galapagos.data

import kotlinx.serialization.Serializable

@Serializable
enum class FishingResearch(val label: String, val sprite: String) {
    STRONG("Strong Research", "island_interface/fishing/strong_research"),
    WISE("Wise Research", "island_interface/fishing/wise_research"),
    GLIMMERING("Glimmering Research", "island_interface/fishing/glimmering_research"),
    GREEDY("Greedy Research", "island_interface/fishing/greedy_research"),
    LUCKY("Lucky Research", "island_interface/fishing/lucky_research")
}

@Serializable
enum class FishingUpgrade(val label: String, val sprite: String) {
    STRONG_HOOK("Strong Hook", "island_interface/fishing/perk_icon/strong_hook"),
    WISE_HOOK("Wise Hook", "island_interface/fishing/perk_icon/wise_hook"),
    GLIMMERING_HOOK("Glimmering Hook", "island_interface/fishing/perk_icon/glimmering_hook"),
    GREEDY_HOOK("Greedy Hook", "island_interface/fishing/perk_icon/greedy_hook"),
    LUCKY_HOOK("Lucky Hook", "island_interface/fishing/perk_icon/lucky_hook"),

    XP_MAGNET("XP Magnet", "island_interface/fishing/perk_icon/xp_magnet"),
    FISH_MAGNET("Fish Magnet", "island_interface/fishing/perk_icon/fish_magnet"),
    PEARL_MAGNET("Pearl Magnet", "island_interface/fishing/perk_icon/pearl_magnet"),
    TREASURE_MAGNET("Treasure Magnet", "island_interface/fishing/perk_icon/treasure_magnet"),
    SPIRIT_MAGNET("Spirit Magnet", "island_interface/fishing/perk_icon/spirit_magnet"),

    BOOSTED_ROD("Boosted Rod", "island_interface/fishing/perk_icon/boosted_rod"),
    SPEEDY_ROD("Speedy Rod", "island_interface/fishing/perk_icon/speedy_rod"),
    GRACEFUL_ROD("Graceful Rod", "island_interface/fishing/perk_icon/graceful_rod"),
    GLITCHED_ROD("Glitched Rod", "island_interface/fishing/perk_icon/glitched_rod"),
    STABLE_ROD("Stable Rod", "island_interface/fishing/perk_icon/stable_rod"),

    STRONG_POT("Strong Pot", "island_interface/fishing/perk_icon/strong_pot"),
    WISE_POT("Wise Pot", "island_interface/fishing/perk_icon/wise_pot"),
    GLIMMERING_POT("Glimmering Pot", "island_interface/fishing/perk_icon/glimmering_pot"),
    GREEDY_POT("Greedy Pot", "island_interface/fishing/perk_icon/greedy_pot"),
    LUCKY_POT("Lucky Pot", "island_interface/fishing/perk_icon/lucky_pot"),

    ELUSIVE_CHANCE("Elusive Chance", "island_items/fishing_item/anglr_lure_strong"),
    WAYFINDER_DATA("Wayfinder Data", "island_items/fishing_item/anglr_lure_wise"),
    PEARL_CHANCE("Pearl Chance", "island_items/fishing_item/anglr_lure_glimmering"),
    TREASURE_CHANCE("Treasure Chance", "island_items/fishing_item/anglr_lure_greedy"),
    SPIRIT_CHANCE("Spirit Chance", "island_items/fishing_item/anglr_lure_lucky"),

    HOOK_OVERCLOCK("Hook Overclock", "island_interface/fihsing/overclock/strong_hook"),
    MAGNET_OVERCLOCK("Magnet Overclock", "island_interface/fihsing/overclock/xp_magnet"),
    ROD_OVERCLOCK("Rod Overclock", "island_interface/fihsing/overclock/boosted_rod"),
    POT_OVERCLOCK("Pot Overclock", "island_interface/fihsing/overclock/strong_pot"),
    UNSTABLE_OVERCLOCK("Unstable Overclock", "island_interface/fihsing/overclock/strong_unstable")
}