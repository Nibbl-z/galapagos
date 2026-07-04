package xyz.nibblz.galapagos.data

enum class MaterialType {
    NONE,
    CLUSTER,
    POWER_SHARD,
    SINGULARITY,
    STYLE_SOUL
}

enum class Material(val label: String, val type: MaterialType, val rarity: Rarity, val marketPrice: Int? = null, val marketCount: Int? = null) {
    FOGGY_CRYSTAL("Foggy Crystal", MaterialType.NONE, Rarity.COMMON, 4, 10),
    IRON_BOLT("Iron Bolt", MaterialType.NONE, Rarity.COMMON, 4, 10),
    PALE_BLOOM("Pale Bloom", MaterialType.NONE, Rarity.COMMON, 4, 10),
    BLAND_WATER("Bland Water", MaterialType.NONE, Rarity.COMMON, 4, 10),

    JADE_EYE("Jade Eye", MaterialType.NONE, Rarity.UNCOMMON, 6, 5),
    COPPER_CHUNK("Copper Chunk", MaterialType.NONE, Rarity.UNCOMMON, 6, 5),
    VERDANT_MOSS("Verdant Moss", MaterialType.NONE, Rarity.UNCOMMON, 6, 5),
    SEAWEED_GOO("Seaweed Goo", MaterialType.NONE, Rarity.UNCOMMON, 6, 5),

    FRIGID_SAPPHIRE("Frigid Sapphire", MaterialType.NONE, Rarity.RARE, 9, 3),
    COBALT_ROD("Cobalt Rod", MaterialType.NONE, Rarity.RARE, 9, 3),
    SKY_POPPY("Sky Poppy", MaterialType.NONE, Rarity.RARE, 9, 3),
    DEEP_BRINE("Deep Brine", MaterialType.NONE, Rarity.RARE, 9, 3),

    AMETHYST_TABLET("Amethyst Tablet", MaterialType.NONE, Rarity.EPIC, 12, 2),
    TITANIUM_PLATE("Titanium Plate", MaterialType.NONE, Rarity.EPIC, 12, 2),
    NIGHTSHADE_LILY("Nightshade Lily", MaterialType.NONE, Rarity.EPIC, 12, 2),
    VIRULENT_VIAL("Virulent Vial", MaterialType.NONE, Rarity.EPIC, 12, 2),

    CRYSTALLIZED_SUNSET("Crystallized Sunset", MaterialType.NONE, Rarity.LEGENDARY, 12, 1),
    SOLARFLAME_BAR("Solarflame Bar", MaterialType.NONE, Rarity.LEGENDARY, 12, 1),
    SPARKLING_SUNFLOWER("Sparkling Sunflower", MaterialType.NONE, Rarity.LEGENDARY, 12, 1),
    BOTTLED_SUNRISE("Bottled Sunrise", MaterialType.NONE, Rarity.LEGENDARY, 12, 1),

    COMMON_POWER_SHARD("Common Power Shard", MaterialType.POWER_SHARD, Rarity.COMMON, 3, 1),
    UNCOMMON_POWER_SHARD("Uncommon Power Shard", MaterialType.POWER_SHARD, Rarity.UNCOMMON, 6, 1),
    RARE_POWER_SHARD("Rare Power Shard", MaterialType.POWER_SHARD, Rarity.RARE, 12, 1),
    EPIC_POWER_SHARD("Epic Power Shard", MaterialType.POWER_SHARD, Rarity.EPIC, 24, 1),
    LEGENDARY_POWER_SHARD("Legendary Power Shard", MaterialType.POWER_SHARD, Rarity.LEGENDARY, 48, 1),
    MYTHIC_POWER_SHARD("Mythic Power Shard", MaterialType.POWER_SHARD, Rarity.MYTHIC),

    COMMON_MATERIAL_CLUSTER("Common Material Cluster", MaterialType.CLUSTER, Rarity.COMMON),
    UNCOMMON_MATERIAL_CLUSTER("Uncommon Material Cluster", MaterialType.CLUSTER, Rarity.COMMON),
    RARE_MATERIAL_CLUSTER("Rare Material Cluster", MaterialType.CLUSTER, Rarity.COMMON),
    EPIC_MATERIAL_CLUSTER("Epic Material Cluster", MaterialType.CLUSTER, Rarity.COMMON),
    LEGENDARY_MATERIAL_CLUSTER("Legendary Material Cluster", MaterialType.CLUSTER, Rarity.COMMON),

    MATERIAL_SINGULARITY("Material Singularity", MaterialType.SINGULARITY, Rarity.MYTHIC, 900, 1),
    STYLE_SOUL("Style Soul", MaterialType.STYLE_SOUL, Rarity.MYTHIC);
}

fun materialFromName(label: String): Material? {
    return Material.entries.find { it.label == label }
}

fun shardFromRarity(rarity: Rarity): Material {
    return Material.entries.find { it.type == MaterialType.POWER_SHARD && it.rarity == rarity }!!
}

val recipes: Map<Material, List<Pair<Material, Int>>> = mapOf(
    // shard recipes
    Material.UNCOMMON_POWER_SHARD to listOf(
        Material.COMMON_POWER_SHARD to 2
    ),
    Material.RARE_POWER_SHARD to listOf(
        Material.UNCOMMON_POWER_SHARD to 2
    ),
    Material.EPIC_POWER_SHARD to listOf(
        Material.RARE_POWER_SHARD to 2
    ),
    Material.LEGENDARY_POWER_SHARD to listOf(
        Material.EPIC_POWER_SHARD to 2
    ),
    Material.MYTHIC_POWER_SHARD to listOf(
        Material.LEGENDARY_POWER_SHARD to 2
    ),

    // cluster recipes
    Material.COMMON_MATERIAL_CLUSTER to listOf(
        Material.FOGGY_CRYSTAL to 1,
        Material.IRON_BOLT to 1,
        Material.PALE_BLOOM to 1,
        Material.BLAND_WATER to 1
    ),

    Material.UNCOMMON_MATERIAL_CLUSTER to listOf(
        Material.JADE_EYE to 1,
        Material.COPPER_CHUNK to 1,
        Material.VERDANT_MOSS to 1,
        Material.SEAWEED_GOO to 1
    ),

    Material.RARE_MATERIAL_CLUSTER to listOf(
        Material.FRIGID_SAPPHIRE to 1,
        Material.COBALT_ROD to 1,
        Material.SKY_POPPY to 1,
        Material.DEEP_BRINE to 1
    ),

    Material.EPIC_MATERIAL_CLUSTER to listOf(
        Material.AMETHYST_TABLET to 1,
        Material.TITANIUM_PLATE to 1,
        Material.NIGHTSHADE_LILY to 1,
        Material.VIRULENT_VIAL to 1
    ),

    Material.LEGENDARY_MATERIAL_CLUSTER to listOf(
        Material.CRYSTALLIZED_SUNSET to 1,
        Material.SOLARFLAME_BAR to 1,
        Material.SPARKLING_SUNFLOWER to 1,
        Material.BOTTLED_SUNRISE to 1
    ),

    Material.MATERIAL_SINGULARITY to listOf(
        Material.COMMON_MATERIAL_CLUSTER to 32,
        Material.UNCOMMON_MATERIAL_CLUSTER to 24,
        Material.RARE_MATERIAL_CLUSTER to 12,
        Material.EPIC_MATERIAL_CLUSTER to 6,
        Material.LEGENDARY_MATERIAL_CLUSTER to 3
    )
)