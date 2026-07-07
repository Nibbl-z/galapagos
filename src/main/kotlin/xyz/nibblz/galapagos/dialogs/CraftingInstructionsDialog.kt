package xyz.nibblz.galapagos.dialogs

import com.noxcrew.sheeplib.LayoutConstants
import com.noxcrew.sheeplib.dialog.Dialog
import com.noxcrew.sheeplib.dialog.title.TextTitleWidget
import com.noxcrew.sheeplib.layout.GridLayout
import com.noxcrew.sheeplib.layout.grid
import com.noxcrew.sheeplib.layout.linear
import com.noxcrew.sheeplib.theme.DefaultTheme
import com.noxcrew.sheeplib.theme.Theme
import com.noxcrew.sheeplib.theme.Themed
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.objects.AtlasSprite
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.Glyphs
import xyz.nibblz.galapagos.PlayerData
import xyz.nibblz.galapagos.data.CosmeticTag
import xyz.nibblz.galapagos.data.Item
import xyz.nibblz.galapagos.data.Material
import xyz.nibblz.galapagos.data.craftingDuration
import xyz.nibblz.galapagos.data.materialFromName
import xyz.nibblz.galapagos.features.CraftingInstructions
import xyz.nibblz.galapagos.features.CraftingInstructions.Instruction
import xyz.nibblz.galapagos.features.CraftingInstructions.InstructionType
import xyz.nibblz.galapagos.features.CraftingInstructions.calculateClusterInstructions
import xyz.nibblz.galapagos.features.CraftingInstructions.calculatePowerShardInstructions
import xyz.nibblz.galapagos.features.CraftingInstructions.calculateSingularityInstructions
import xyz.nibblz.galapagos.features.CraftingInstructions.getComponent
import xyz.nibblz.galapagos.features.CraftingInstructions.gloopForRawMaterial
import xyz.nibblz.galapagos.features.CraftingInstructions.tempInfinibag
import xyz.nibblz.galapagos.formatTimeString
import xyz.nibblz.galapagos.mccTextureComponent

class CraftingInstructionsDialog(x: Int, y: Int, val blueprint: CraftingInstructions.BlueprintInfo) : Dialog(x, y), Themed by GalapagosTheme {
    val instructions: HashMap<Material, List<Instruction>> = hashMapOf()
    val materialStatus: HashMap<Material, Pair<Int, Int>> = hashMapOf()
    var requirements: List<Pair<String, Int>> = listOf()
    var gloop: Int = 0
    var time: Int = 0

    fun calculateInstructions() {
        tempInfinibag.clear()
        Galapagos.save.infinibag.forEach { (name, item) ->
            tempInfinibag[name] = item.clone()
        }

        Galapagos.save.fusionForge.forEach {
            if (tempInfinibag[it.name] == null) {
                tempInfinibag[it.name] = Item(name = it.name, count = it.count, isCosmeticToken = false)
            } else {
                tempInfinibag[it.name]!!.count += it.count
            }
        }

        instructions.clear()
        materialStatus.clear()
        gloop = 0
        time = 0

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

            materialStatus[material] = (saveMaterial?.count ?: 0) to count

            // call me yanderedev the way i be if else if else if else if else if ing
            if (material.label.contains("Power Shard")) {
                val shardData = calculatePowerShardInstructions(material, required)

                instructions[material] = shardData.first
                gloop += shardData.second
            } else if (material.label.contains("Cluster")) {
                val clusterData = calculateClusterInstructions(material, required)
                instructions[material] = clusterData.first
                gloop += clusterData.second
            } else if (material.label.contains("Material Singularity")) {
                val singularityData = calculateSingularityInstructions(required)

                instructions[material] = singularityData.first
                gloop += singularityData.second
            } else if (material.label.contains("Style Soul") || material.label.contains("Style Shard")) {
                instructions[material] = listOf(Instruction(
                    type = InstructionType.PURCHASE_IE,
                    material = material,
                    count = required
                ))
            } else { // Standard material
                instructions[material] = listOf(Instruction(
                    type = InstructionType.PURCHASE,
                    material = material,
                    count = required
                ))

                gloop += gloopForRawMaterial(material, required) ?: 0

                saveMaterial?.count -= required
            }
        }

        if (instructions.isEmpty()) {
            super.close()
            return
        }

        instructions.forEach { (_, instructionList) ->
            instructionList.forEach craftCheck@ {
                if (it.type != InstructionType.CRAFT) return@craftCheck

                time += (craftingDuration[it.material] ?: 0) * it.count
            }
        }

        super.init()
    }

    override fun layout() = linear(LinearLayout.Orientation.VERTICAL) {
        val font = Minecraft.getInstance().font
        val efficientFusion = 1.0 - (Galapagos.save.stylePerks[PlayerData.StylePerk.EFFICIENT_FUSION]!! * 0.05)

        instructions.forEach { (material, instructions) ->
            +StringWidget(
                mccTextureComponent(material.getSpriteLocation())
                    .append(Component.literal(" ${material.label}:").withColor(material.rarity.color))
                    .append(Component.literal(" [${materialStatus[material]!!.first}/${materialStatus[material]!!.second}]").withColor(
                        ChatFormatting.GRAY.color!!)),
                font)
            instructions.forEach {
                +StringWidget((Component.literal("• ")).append(it.getComponent()), font)
            }
        }

        +StringWidget(Component.literal(""), font)
        +StringWidget(Component.literal("Total Gloop: $gloop ").append(mccTextureComponent("island_items/infinibag/material/gloop")), font)
        if (time > 0) {
            +StringWidget(Component.literal("Total Craft Time: ${formatTimeString((time * efficientFusion).toInt())} ").append(
                Glyphs.getGlyphComponent("_fonts/icon/time.png")), font)
        }

    }

    override fun onClose() {
        CraftingInstructions.openBlueprints.remove(blueprint.name)
    }

    override val title = TextTitleWidget(this,
        mccTextureComponent("island_items/infinibag/blueprint/cosmetic_${if (blueprint.type == CosmeticTag.STANDARD) "" else "${blueprint.type.name.lowercase()}_"}${blueprint.rarity.name.lowercase()}")
            .append(Component.literal(" ${blueprint.name}").withColor(blueprint.rarity.color))
    )
}