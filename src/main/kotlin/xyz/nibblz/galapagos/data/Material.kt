package xyz.nibblz.galapagos.data

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import xyz.nibblz.galapagos.util.mccTextureComponent

enum class MaterialType {
    NONE,
    CLUSTER,
    POWER_SHARD,
    SINGULARITY,
    STYLE_SOUL,
    STYLE_SHARD,
}

enum class Material(val label: String, val type: MaterialType, val rarity: Rarity, val spriteName: String, val marketPrice: Int? = null, val marketCount: Int? = null) {
    FOGGY_CRYSTAL("Foggy Crystal", MaterialType.NONE, Rarity.COMMON, "magical_common", 4, 10),
    IRON_BOLT("Iron Bolt", MaterialType.NONE, Rarity.COMMON, "mechanical_common", 4, 10),
    PALE_BLOOM("Pale Bloom", MaterialType.NONE, Rarity.COMMON, "nature_common", 4, 10),
    BLAND_WATER("Bland Water", MaterialType.NONE, Rarity.COMMON, "oceanic_common", 4, 10),

    JADE_EYE("Jade Eye", MaterialType.NONE, Rarity.UNCOMMON, "magical_uncommon", 6, 5),
    COPPER_CHUNK("Copper Chunk", MaterialType.NONE, Rarity.UNCOMMON, "mechanical_uncommon", 6, 5),
    VERDANT_MOSS("Verdant Moss", MaterialType.NONE, Rarity.UNCOMMON, "nature_uncommon", 6, 5),
    SEAWEED_GOO("Seaweed Goo", MaterialType.NONE, Rarity.UNCOMMON, "oceanic_uncommon", 6, 5),

    FRIGID_SAPPHIRE("Frigid Sapphire", MaterialType.NONE, Rarity.RARE, "magical_rare", 9, 3),
    COBALT_ROD("Cobalt Rod", MaterialType.NONE, Rarity.RARE, "mechanical_rare", 9, 3),
    SKY_POPPY("Sky Poppy", MaterialType.NONE, Rarity.RARE, "nature_rare", 9, 3),
    DEEP_BRINE("Deep Brine", MaterialType.NONE, Rarity.RARE, "oceanic_rare", 9, 3),

    AMETHYST_TABLET("Amethyst Tablet", MaterialType.NONE, Rarity.EPIC, "magical_epic", 12, 2),
    TITANIUM_PLATE("Titanium Plate", MaterialType.NONE, Rarity.EPIC, "mechanical_epic",  12, 2),
    NIGHTSHADE_LILY("Nightshade Lily", MaterialType.NONE, Rarity.EPIC, "nature_epic", 12, 2),
    VIRULENT_VIAL("Virulent Vial", MaterialType.NONE, Rarity.EPIC, "oceanic_epic", 12, 2),

    CRYSTALLIZED_SUNSET("Crystallized Sunset", MaterialType.NONE, Rarity.LEGENDARY, "magical_legendary", 12, 1),
    SOLARFLAME_BAR("Solarflame Bar", MaterialType.NONE, Rarity.LEGENDARY, "mechanical_legendary", 12, 1),
    SPARKLING_SUNFLOWER("Sparkling Sunflower", MaterialType.NONE, Rarity.LEGENDARY, "nature_legendary", 12, 1),
    BOTTLED_SUNRISE("Bottled Sunrise", MaterialType.NONE, Rarity.LEGENDARY, "oceanic_legendary",12,  1),

    COMMON_POWER_SHARD("Common Power Shard", MaterialType.POWER_SHARD, Rarity.COMMON, "power_shard_common", 3, 1),
    UNCOMMON_POWER_SHARD("Uncommon Power Shard", MaterialType.POWER_SHARD, Rarity.UNCOMMON, "power_shard_uncommon", 6, 1),
    RARE_POWER_SHARD("Rare Power Shard", MaterialType.POWER_SHARD, Rarity.RARE, "power_shard_rare", 12, 1),
    EPIC_POWER_SHARD("Epic Power Shard", MaterialType.POWER_SHARD, Rarity.EPIC, "power_shard_epic", 24, 1),
    LEGENDARY_POWER_SHARD("Legendary Power Shard", MaterialType.POWER_SHARD, Rarity.LEGENDARY, "power_shard_legendary", 48, 1),
    MYTHIC_POWER_SHARD("Mythic Power Shard", MaterialType.POWER_SHARD, Rarity.MYTHIC, "power_shard_mythic"),

    COMMON_MATERIAL_CLUSTER("Common Material Cluster", MaterialType.CLUSTER, Rarity.COMMON, "cluster_common"),
    UNCOMMON_MATERIAL_CLUSTER("Uncommon Material Cluster", MaterialType.CLUSTER, Rarity.UNCOMMON, "cluster_uncommon"),
    RARE_MATERIAL_CLUSTER("Rare Material Cluster", MaterialType.CLUSTER, Rarity.RARE, "cluster_rare"),
    EPIC_MATERIAL_CLUSTER("Epic Material Cluster", MaterialType.CLUSTER, Rarity.EPIC, "cluster_epic"),
    LEGENDARY_MATERIAL_CLUSTER("Legendary Material Cluster", MaterialType.CLUSTER, Rarity.LEGENDARY, "cluster_legendary"),

    MATERIAL_SINGULARITY("Material Singularity", MaterialType.SINGULARITY, Rarity.MYTHIC, "material_singularity", 900, 1),
    STYLE_SOUL("Style Soul", MaterialType.STYLE_SOUL, Rarity.MYTHIC, "style_soul"),

    RUBY_STYLE_SHARD("Ruby Style Shard", MaterialType.STYLE_SHARD, Rarity.MYTHIC, "style_shard_ruby"),
    AMBER_STYLE_SHARD("Amber Style Shard", MaterialType.STYLE_SHARD, Rarity.MYTHIC, "style_shard_amber"),
    CITRINE_STYLE_SHARD("Citrine Style Shard", MaterialType.STYLE_SHARD, Rarity.MYTHIC, "style_shard_citrine"),
    JADE_STYLE_SHARD("Jade Style Shard", MaterialType.STYLE_SHARD, Rarity.MYTHIC, "style_shard_jade"),
    AQUAMARINE_STYLE_SHARD("Aquamarine Style Shard", MaterialType.STYLE_SHARD, Rarity.MYTHIC, "style_shard_aquamarine"),
    SAPPHIRE_STYLE_SHARD("Sapphire Style Shard", MaterialType.STYLE_SHARD, Rarity.MYTHIC, "style_shard_sapphire"),
    AMETHYST_STYLE_SHARD("Amethyst Style Shard", MaterialType.STYLE_SHARD, Rarity.MYTHIC, "style_shard_amethyst"),
    GARNET_STYLE_SHARD("Garnet Style Shard", MaterialType.STYLE_SHARD, Rarity.MYTHIC, "style_shard_garnet"),
    OPAL_STYLE_SHARD("Opal Style Shard", MaterialType.STYLE_SHARD, Rarity.MYTHIC, "style_shard_opal");

    fun getSpriteLocation(): String {
        return "island_items/infinibag/${if (this == Material.MATERIAL_SINGULARITY) "component" else "material"}/${this.spriteName}"
    }

    fun getStyledComponent(): MutableComponent {
        return mccTextureComponent(this.getSpriteLocation())
            .append(Component.literal(" ${this.label}").withColor(this.rarity.color))
    }
}

fun materialFromName(label: String): Material? {
    return Material.entries.find { it.label == label }
}

fun shardFromRarity(rarity: Rarity): Material {
    return Material.entries.find { it.type == MaterialType.POWER_SHARD && it.rarity == rarity }!!
}

val craftingDuration: Map<Material, Int> = mapOf(
    Material.COMMON_MATERIAL_CLUSTER to 10*60,
    Material.UNCOMMON_MATERIAL_CLUSTER to 30*60,
    Material.RARE_MATERIAL_CLUSTER to 60*60,
    Material.EPIC_MATERIAL_CLUSTER to 180*60,
    Material.LEGENDARY_MATERIAL_CLUSTER to 360*60,
    Material.MATERIAL_SINGULARITY to 24*60*60,

    Material.UNCOMMON_POWER_SHARD to 30*60,
    Material.RARE_POWER_SHARD to 60*60,
    Material.EPIC_POWER_SHARD to 180*60,
    Material.LEGENDARY_POWER_SHARD to 360*60,
    Material.MYTHIC_POWER_SHARD to 720*60
)

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