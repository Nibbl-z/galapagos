package xyz.nibblz.galapagos.features

import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.GalapagosCommand
import xyz.nibblz.galapagos.PlayerData
import xyz.nibblz.galapagos.data.Material
import xyz.nibblz.galapagos.data.Rarity
import xyz.nibblz.galapagos.data.materialFromName
import xyz.nibblz.galapagos.data.recipes
import xyz.nibblz.galapagos.data.shardFromRarity
import xyz.nibblz.galapagos.events.SlotClickEvent
import xyz.nibblz.galapagos.findLores
import xyz.nibblz.galapagos.mixin.accessor.HoveredSlotAccessor
import kotlin.math.ceil
import kotlin.text.get

object CraftingInstructions : Feature {
    override val id: String = "crafting_instructions"
    override val name: String = "Crafting Instructions"

    var tempInfinibag: HashMap<String, PlayerData.Item> = hashMapOf()

    override fun init() {
        SlotClickEvent.EVENT.register { screen, input -> slotClick(screen, input)  }
    }

    fun slotClick(screen: ContainerScreen, type: ContainerInput) {
        //if (!screen.title.string.contains("INFINIBAG", false)) return
        val slot = (screen as HoveredSlotAccessor).`galapagos$hoveredSlot`() ?: return

        val requirements = fetchCraftingMaterials(slot.item)
        if (requirements.isEmpty()) return

        calculateCraftingInstructions(requirements.sortedByDescending {
            val material = materialFromName(it.first) ?: throw IllegalStateException("Crafting recipe calls for ${it.first}, which is not a valid material")
            material.rarity.ordinal
        })
    }

    // if (!item.itemName.string.contains("Blueprint:")) return
    fun fetchCraftingMaterials(item: ItemStack): List<Pair<String, Int>> {
        val regex = Regex("\\[(?<name>.+?)] \\[(?<count>[\\d,]+)/(?<price>[\\d,]+)]")
        val matches = item.findLores(regex)
        if (matches.isEmpty()) return listOf()

        val materials: MutableList<Pair<String, Int>> = mutableListOf()

        matches.forEach {
            val name = it["name"]?.value ?: return@forEach
            val price = it["price"]?.value?.toIntOrNull() ?: return@forEach

            materials.add(name to price)
        }

        return materials
    }

    fun gloopForRawMaterial(material: Material, count: Int): Int? {
        if (material.marketPrice == null) return null
        val purchases = ceil(count / material.marketCount!!.toDouble()).toInt()

        return purchases * material.marketPrice
    }

    fun calculateRecipeCrafting(material: Material, count: Int): List<Pair<Material, Int>> {
        val recipe = recipes[material] ?: return mutableListOf()
        val missing: MutableList<Pair<Material, Int>> = mutableListOf()

        recipe.forEach {
            val saveMaterial = tempInfinibag[it.first.label]

            if (saveMaterial == null) {
                missing.add(it.first to it.second * count)
                return@forEach
            }

            if (saveMaterial.count >= it.second * count) {
                saveMaterial.count -= it.second * count
                return@forEach
            }

            val missingCount = it.second * count - saveMaterial.count
            missing.add(it.first to missingCount)

            saveMaterial.count = 0
        }

        return missing
    }

    fun calculateClusterInstructions(cluster: Material, count: Int): Pair<List<String>, Int> {
        val materials = calculateRecipeCrafting(cluster, count)
        val instructions: MutableList<String> = mutableListOf()
        var gloop = 0

        instructions.add("to craft x$count ${cluster.name}...")
        materials.forEach {
            instructions.add("spend ${gloopForRawMaterial(it.first, it.second)} gloop on ${it.first.name}")
            gloop += gloopForRawMaterial(it.first, it.second) ?: 0
        }
        instructions.add("craft x$count ${cluster.name}")

        return instructions to gloop
    }

    fun calculateSingularityInstructions(count: Int): Pair<List<String>, Int> {
        val materials = calculateRecipeCrafting(Material.MATERIAL_SINGULARITY, count)
        val instructions: MutableList<String> = mutableListOf()
        var gloop = 0

        instructions.add("to craft x$count material singularity")

        materials.forEach {
            val clusterData = calculateClusterInstructions(it.first, it.second)
            clusterData.first.forEach { instruction -> instructions.add(instruction) }
            gloop += clusterData.second
        }

        instructions.add("craft x$count material singularity")
        return instructions to gloop
    }

    fun calculatePowerShardInstructions(shard: Material, count: Int): Pair<List<String>, Int> { // hello and welcome to recursion hell
        val instructions: MutableList<String> = mutableListOf()

        val purchases: HashMap<Material, Int> = hashMapOf()
        val crafts: HashMap<Material, Int> = hashMapOf()

        var gloop = 0

        val lowerShards: MutableMap<Material, Int> = mutableMapOf()
        var lowestShard: Pair<Material, Int>

        instructions.add("to craft x$count $shard...")

        Rarity.entries.forEach {
            purchases[shardFromRarity(it)] = 0
            crafts[shardFromRarity(it)] = 0
        }

        repeat(count) { _ ->
            lowestShard = shard to (tempInfinibag[shard.label]?.count ?: 0)

            Rarity.entries.forEach {
                if (it.ordinal >= shard.rarity.ordinal) return@forEach
                val lowerShard = shardFromRarity(it)
                lowerShards[lowerShard] = tempInfinibag[lowerShard.label]?.count ?: 0

                if (lowerShards[lowerShard]!! > 0 && it.ordinal < lowestShard.first.rarity.ordinal) {
                    lowestShard = lowerShard to lowerShards[lowerShard]!!
                }
            }

            lowerShards.forEach { (lowerShard, lowerCount) ->
                var updateAbove = true
                val upperShard = shardFromRarity(Rarity.entries[lowerShard.rarity.ordinal + 1])

                if (lowerShards[upperShard] == null) updateAbove = false

                if (tempInfinibag[lowerShard.label] == null) tempInfinibag[lowerShard.label] =
                    PlayerData.Item(name = lowerShard.label, count = 0, isCosmeticToken = false)
                if (tempInfinibag[upperShard.label] == null) tempInfinibag[upperShard.label] =
                    PlayerData.Item(name = upperShard.label, count = 0, isCosmeticToken = false)

                if (lowerCount == 0) return@forEach

                if (lowerCount % 2 == 0) { // even!
                    tempInfinibag[lowerShard.label]!!.count -= 2

                    crafts[upperShard] = crafts[upperShard]!! + 1
                    tempInfinibag[upperShard.label]!!.count++

                    if (updateAbove) lowerShards[upperShard] = lowerShards[upperShard]!! + 1
                    lowerShards[lowerShard] = 0

                } else { // odd!
                    purchases[lowerShard] = purchases[lowerShard]!! + 1
                    gloop += lowerShard.marketPrice!!
                    tempInfinibag[lowerShard.label]!!.count--

                    crafts[upperShard] = crafts[upperShard]!! + 1
                    tempInfinibag[upperShard.label]!!.count++

                    if (updateAbove) lowerShards[upperShard] = lowerShards[upperShard]!! + 1
                    lowerShards[lowerShard] = 0
                }
            }

            if (lowestShard.first == shard) {
                if (shard == Material.MYTHIC_POWER_SHARD) {
                    // w hard coding :heart:
                    purchases[Material.LEGENDARY_POWER_SHARD] = purchases[Material.LEGENDARY_POWER_SHARD]!! + 2
                    gloop += Material.LEGENDARY_POWER_SHARD.marketPrice!! * 2
                    crafts[Material.MYTHIC_POWER_SHARD] = crafts[Material.MYTHIC_POWER_SHARD]!! + 1
                } else {
                    purchases[shard] = purchases[shard]!! + 1
                    gloop += shard.marketPrice!!
                }
            }
        }

        Rarity.entries.forEach {
            val shard = shardFromRarity(it)
            if (purchases[shard]!! > 0) instructions.add("purchase x${purchases[shard]} $shard")
            if (crafts[shard]!! > 0) instructions.add("craft x${crafts[shard]} $shard")
        }

        return instructions to gloop
    }

    fun calculateCraftingInstructions(requirements: List<Pair<String, Int>>) {
        tempInfinibag.clear()
        Galapagos.save.infinibag.forEach { (name, item) ->
            tempInfinibag[name] = item.clone()
        }

        val instructions: MutableList<String> = mutableListOf()
        var gloop = 0

        requirements.forEach { req ->
            val material = materialFromName(req.first) ?: return@forEach
            val count = req.second
            val saveMaterial = tempInfinibag[material.label]

            Galapagos.logger.info("checking requirement $material (${material.label}), i need $count, i have ${saveMaterial?.count ?: 0}")

            if (saveMaterial != null) {
                if (saveMaterial.count >= count) { // already have enough of the material
                    saveMaterial.count -= count
                    return@forEach
                }
            }

            val required = count - (saveMaterial?.count ?: 0)

                    // call me yanderedev the way i be if else if else if else if else if ing
            if (material.label.contains("Power Shard")) {
                val shardData = calculatePowerShardInstructions(material, required)
                shardData.first.forEach { instruction -> instructions.add(instruction) }
                gloop += shardData.second
            } else if (material.label.contains("Cluster")) {
                val clusterData = calculateClusterInstructions(material, required)
                clusterData.first.forEach { instruction -> instructions.add(instruction) }
                gloop += clusterData.second
            } else if (material.label.contains("Material Singularity")) {
                val singularityData = calculateSingularityInstructions(required)
                singularityData.first.forEach { instruction -> instructions.add(instruction) }
                gloop += singularityData.second
            } else if (material.label.contains("Style Soul")) {
                instructions.add("sell your soul for style souls on ie")
            } else { // Standard material
                instructions.add("spend ${gloopForRawMaterial(material, required)} gloop on ${material.name}")
                gloop += gloopForRawMaterial(material, required) ?: 0

                saveMaterial?.count -= required
            }
        }

        Galapagos.logger.info(gloop.toString())
        instructions.forEach {
            Galapagos.logger.info(it)
        }
    }
}