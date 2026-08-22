package xyz.nibblz.galapagos.data

enum class Area(val label: String, val heart: String) {
    FROSTED_FOREST("Frosted Forest", "heart_blue"),
    GLACIAL_SETTLEMENT("Glacial Settlement", "heart_white"),
    CRYSTAL_CLIFFS("Crystal Cliffs", "heart_purple"),
    ARCTIC_CAVERNS("Arctic Caverns", "heart_pink"),
    BAMBOO_HILLS("Bamboo Hills", "heart_green"),
    MAGMATIC_SPRINGS("Magmatic Springs", "heart"),
    BLOOMING_SWAMP("Blooming Swamp", "heart_orange"),
    BASIC_BEACH("Basic Beach", "heart_yellow")
}

val FISH_PER_AREA: HashMap<Area, HashMap<String, Rarity>> = hashMapOf(
    Area.FROSTED_FOREST to hashMapOf(
        "Fern Flounder" to Rarity.COMMON,
        "Coral Cod" to Rarity.UNCOMMON,
        "Glass Pike" to Rarity.RARE,
        "Mosaic Guppy" to Rarity.EPIC,
        "Queenfish" to Rarity.LEGENDARY,
        "Mirrored Mahi" to Rarity.MYTHIC
    ),
    Area.GLACIAL_SETTLEMENT to hashMapOf(
        "Bluegill" to Rarity.COMMON,
        "Midnight Gourami" to Rarity.UNCOMMON,
        "Painted Discus" to Rarity.RARE,
        "Pearlescent Betta" to Rarity.EPIC,
        "Disco Discus" to Rarity.LEGENDARY,
        "Wreckfish" to Rarity.MYTHIC
    ),
    Area.CRYSTAL_CLIFFS to hashMapOf(
        "Reef Anchovy" to Rarity.COMMON,
        "Crystalline Cod" to Rarity.UNCOMMON,
        "Shardine" to Rarity.RARE,
        "Ocean Moonfish" to Rarity.EPIC,
        "Nightmare Marlin" to Rarity.LEGENDARY,
        "Floodfish" to Rarity.MYTHIC
    ),
    Area.ARCTIC_CAVERNS to hashMapOf(
        "Silver Snook" to Rarity.COMMON,
        "Ancient Snapper" to Rarity.UNCOMMON,
        "Sunken Koi" to Rarity.RARE,
        "Blossom Betta" to Rarity.EPIC,
        "Sapphire Salmon" to Rarity.LEGENDARY,
        "Cosmic Cod" to Rarity.MYTHIC
    ),
    Area.BAMBOO_HILLS to hashMapOf(
        "Neon Tetra" to Rarity.COMMON,
        "Sinharaja Salmon" to Rarity.UNCOMMON,
        "Daintree Guppy" to Rarity.RARE,
        "Viney Perch" to Rarity.EPIC,
        "Green Terror Cichlid" to Rarity.LEGENDARY,
        "Torch Tarpon" to Rarity.MYTHIC
    ),
    Area.MAGMATIC_SPRINGS to hashMapOf(
        "Coal Cod" to Rarity.COMMON,
        "Emberpike" to Rarity.UNCOMMON,
        "Volcanic Surgeonfish" to Rarity.RARE,
        "Molten Goldfish" to Rarity.EPIC,
        "Ashen Tilapia" to Rarity.LEGENDARY,
        "Blazing Betta" to Rarity.MYTHIC
    ),
    Area.BLOOMING_SWAMP to hashMapOf(
        "Fungal Salmon" to Rarity.COMMON,
        "Frilled Mackerel" to Rarity.UNCOMMON,
        "Ancient Fangtooth" to Rarity.RARE,
        "Swampy Betta" to Rarity.EPIC,
        "Glowberry Guppy" to Rarity.LEGENDARY,
        "Troutarium" to Rarity.MYTHIC
    ),
    Area.BASIC_BEACH to hashMapOf(
        "Coral Angelfish" to Rarity.COMMON,
        "Butterfly Fish" to Rarity.UNCOMMON,
        "Surgeonfish" to Rarity.RARE,
        "Braincoral Betta" to Rarity.EPIC,
        "Parrotfish" to Rarity.LEGENDARY,
        "Diamond Oarfish" to Rarity.MYTHIC
    ),
)