package xyz.nibblz.galapagos.features

import com.noxcrew.sheeplib.DialogContainer
import com.noxcrew.sheeplib.dialog.Dialog
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.data.*
import xyz.nibblz.galapagos.dialogs.CraftingInstructionsDialog
import xyz.nibblz.galapagos.events.*
import xyz.nibblz.galapagos.mixin.accessor.HoveredSlotAccessor
import xyz.nibblz.galapagos.util.*
import kotlin.math.ceil
import kotlin.reflect.KMutableProperty0

object CraftingInstructions : Feature {
    override val id: String = "crafting_instructions"
    override val name: String = "Crafting Instructions"
    override val description: List<Component> = listOf(
        Component.literal("Shows a list of instructions to craft a blueprint by Shift+Right clicking on it."),
        Component.literal("This will open a window including step-by-step instructions on what to spend your gloop on and what to craft, in order to use the minimum gloop required to craft the cosmetic."),
        Component.literal("The window also shows how much time all the crafting of materials will take.")
    )
    override val enabledProperty: KMutableProperty0<Boolean> = Config.values::craftingInstructionsEnabled
    override val image: Config.ConfigImage = Config.ConfigImage("crafting_instructions.png", 539, 490)

    var tempInfinibag: HashMap<String, Item> = hashMapOf()

    data class BlueprintInfo(
        val rarity: Rarity,
        val type: CosmeticTag,
        val name: String
    )

    enum class InstructionType {
        CRAFT,
        PURCHASE,
        PURCHASE_IE,
        ANVIL
    }

    data class Instruction(
        val material: Material,
        val type: InstructionType,
        val count: Int
    )

    val openBlueprints: HashMap<String, CraftingInstructionsDialog> = hashMapOf()
    val craftableBlueprints: MutableList<String> = mutableListOf()

    fun Instruction.getComponent(): Component {
        return when(type) {
            InstructionType.CRAFT -> {
                val craftTime = (craftingDuration[material] ?: 0) * count
                val efficientFusion = 1.0 - (Galapagos.save.stylePerks[PlayerData.StylePerk.EFFICIENT_FUSION]!! * 0.05)

                var component = Component.literal("Craft ${count}x ")
                    .append(material.getStyledComponent())

                if (Config.values::craftingInstructionsShowCraftTime.get()) {
                    component = component
                        .append(Component.literal(if (craftTime != 0) " [${formatTimeString((craftTime * efficientFusion).toInt())}]" else "")
                        .withColor(ChatFormatting.GRAY.color!!))
                }

                component
            }
            InstructionType.PURCHASE -> {
                val purchases = purchasesForRawMaterial(material, count)

                var component = Component.literal("Buy ${count}x ")
                    .append(material.getStyledComponent())
                    .append(Component.literal(" [${purchases}x purchase${if (purchases == 1) "" else "s"}").withColor(ChatFormatting.GRAY.color!!))

                if (Config.values::craftingInstructionsShowGloop.get()) {
                    component = component
                        .append(Component.literal(", ${gloopForRawMaterial(material, count)} ").withColor(ChatFormatting.GRAY.color!!))
                        .append(mccTextureComponent("island_items/infinibag/material/gloop"))
                }

                component
                    .append(Component.literal("]").withColor(ChatFormatting.GRAY.color!!))
            }
            InstructionType.PURCHASE_IE -> Component.literal("Purchase ${count}x ")
                .append(material.getStyledComponent())
                .append(Component.literal(" from Island Exchange").withColor(0xFFFFFF))

            InstructionType.ANVIL -> Component.literal("Scavenge ${count}x Arcane Cosmetic Token${if (count == 1) "" else "s"}")
        }
    }

    override fun init() {
        SlotClickEvent.EVENT.register { screen, input, ci, button -> slotClick(screen, input, ci, button) }
        ItemTooltipCallback.EVENT.register { stack, _, _, components -> tooltipAdd(stack, components) }
        InfinibagUpdateEvent.EVENT.register { infinibagUpdate() }
        ContainerSetSlotEvent.EVENT.register { packet -> setSlot(packet) }
        ContainerOpenEvent.EVENT.register { packet -> openContainer(packet) }
        ScoreboardTitleUpdateEvent.EVENT.register { title -> scoreboardTitleUpdate(title) }
    }

    fun infinibagUpdate() {
        val toRemove = mutableListOf<String>()

        openBlueprints.forEach { (name, dialog) ->
            dialog.calculateInstructions()
            if (dialog.state == Dialog.State.CLOSED) {
                toRemove.add(name)
            }
        }

        toRemove.forEach {
            openBlueprints.remove(it)
        }
    }

    fun hasValidInstructions(item: ItemStack): Boolean {
        val screen = Minecraft.getInstance().screen ?: return false
        if (screen !is ContainerScreen) return false

        return hasValidInstructions(item.itemName.string, item.getTooltipLines(
            net.minecraft.world.item.Item.TooltipContext.EMPTY,
            Minecraft.getInstance().player,
            TooltipFlag.Default.NORMAL
        ), screen)
    }

    fun hasValidInstructions(item: ItemStack, lore: List<Component>): Boolean {
        val screen = Minecraft.getInstance().screen ?: return false
        if (screen !is ContainerScreen) return false

        return hasValidInstructions(item.itemName.string, lore, screen)
    }

    fun hasValidInstructions(name: String, lore: List<Component>, screen: ContainerScreen): Boolean {
        if (screen.title.string.contains("STYLE PERKS")) {
            //if (!PlayerData.StylePerk.entries.any { it.label == name }) return false
            if (findLoreFromList(lore, "Reach Style Level")) return false
        } else {
            if (!name.contains("Blueprint:") && !findLoreFromList(lore, "Trophies: ") && !findLoreFromList(lore, "Style Shard")) return false
            if (findLoreFromList(lore, "You already own this item.")) return false
            if (craftableBlueprints.contains(name)) return false
            if (findLoreFromList(lore, Glyphs.getGlyph("_fonts/icon/tooltips/material.png"))) return false
        }

        if (
            !findLoreFromList(lore, "Crafting Materials:")
            && !findLoreFromList(lore, "Upgrade Cost:")
            && !findLoreFromList(lore, "Cost:")
        ) return false

        return true
    }

    fun slotClick(screen: ContainerScreen, type: ContainerInput, ci: CallbackInfo, button: Int) {
        if (!enabledProperty.get()) return
        val slot = (screen as HoveredSlotAccessor).`galapagos$hoveredSlot`() ?: return
        if (type != ContainerInput.QUICK_MOVE) return
        if (button != 1) return
        if (!hasValidInstructions(slot.item)) return

        val requirements = fetchCraftingMaterials(slot.item)
        if (requirements.isEmpty()) return

        ci.cancel()
        playMccSound("ui.click_normal")

        if (openBlueprints[slot.item.itemName.string] != null) {
            openBlueprints[slot.item.itemName.string]!!.close()
            return
        }

        val tag = if (slot.item.findLore(Glyphs.getGlyph("_fonts/icon/tooltips/collector.png"))) {
            CosmeticTag.ARCANE
        } else {
            slot.item.getCosmeticTag()
        }

        val perkName = Regex("(?<perk>.+) ").find(slot.item.itemName.string)?.groups["perk"]?.value
        val stylePerk = PlayerData.StylePerk.entries.find { it.label == perkName }

        val dialog = CraftingInstructionsDialog(10, 10, BlueprintInfo(
            slot.item.getItemRarity() ?: Rarity.COMMON,
            tag,
            slot.item.itemName.string,
        ), stylePerk)

        dialog.requirements = requirements.sortedByDescending {
            val material = materialFromName(it.first) ?: throw IllegalStateException("Crafting recipe calls for ${it.first}, which is not a valid material")
            material.rarity.ordinal
        }

        dialog.calculateInstructions()

        openBlueprints[slot.item.itemName.string] = dialog
        DialogContainer += dialog
    }

    fun setSlot(packet: ClientboundContainerSetSlotPacket) {
        checkBlueprint(packet.item)
    }

    fun openContainer(packet: ClientboundContainerSetContentPacket) {
        packet.items.forEach { checkBlueprint(it) }
    }

    fun scoreboardTitleUpdate(title: String) {
        if (title.contains("MAIN ISLAND")) return

        openBlueprints.forEach { (_, dialog) ->
            dialog.close()
        }

        openBlueprints.clear()
    }

    fun checkBlueprint(item: ItemStack) {
        val requirements = fetchCraftingMaterials(item)
        if (requirements.isEmpty()) return

        var requirementsMet = 0

        requirements.forEach {
            val material = it.first
            val count = it.second
            val savedCount = Galapagos.save.infinibag[material]?.count ?: 0

            if (count <= savedCount) requirementsMet++
        }

        if (requirementsMet == requirements.size) {
            craftableBlueprints.add(item.itemName.string)
        } else {
            craftableBlueprints.removeIf { it == item.itemName.string }
        }
    }

    fun tooltipAdd(stack: ItemStack, components: MutableList<Component>) {
        if (!enabledProperty.get()) return
        if (!hasValidInstructions(stack, components)) return

        var index = components.indexOfFirst { it.string.contains("Click to") && !it.string.contains("Right") && !it.string.contains("Middle") }
        if (index != -1) index++
        // fallback!!
        if (index == -1) { index = components.indexOfFirst { it.string.contains("minecraft:") } } // if you have f3+h on :P
        if (index == -1) { index = components.size - 1 } // if you dont !

        components.add(index, Component.empty()
            .append(Glyphs.getGlyphComponent("_fonts/icon/click_action_shift.png"))
            .append(Component.literal("+").withColor(0xecd584))
            .append(Glyphs.getGlyphComponent("_fonts/icon/click_action_right.png"))
            .append(Component.literal(" > ").withColor(ChatFormatting.DARK_GRAY.color!!))
            .append(Component.literal("Shift-Right-Click to ").withColor(0xecd584))
            .append(Component.literal("${if (openBlueprints[stack.itemName.string] == null) "Open" else "Close"} Instructions").withColor(0xfee761)))
    }

    fun fetchCraftingMaterials(item: ItemStack): List<Pair<String, Int>> {
        val regex = Regex("\\[(?<name>.+?)] \\[(?<count>[\\d,]+)/(?<price>[\\d,]+)]") // shows up on blueprints
        var matches = item.findLores(regex)

        if (matches.isEmpty()) {
            val altRegex = Regex("(?<count>\\d+)/(?<price>\\d+) \\[(?<name>.+)]") // shows up on secret styles/fancypants
            matches = item.findLores(altRegex)
        }

        if (matches.isEmpty()) {
            val altRegex = Regex("\\[(?<name>.+?)] (?<count>\\d+)/(?<price>\\d+)") // shows up on style perks dear god noxcrew be consistent :sob:
            matches = item.findLores(altRegex)
            if (matches.isEmpty()) return listOf()
        }

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

        return purchasesForRawMaterial(material, count)!! * material.marketPrice
    }

    fun purchasesForRawMaterial(material: Material, count: Int): Int? {
        if (material.marketPrice == null) return null
        val purchases = ceil(count / material.marketCount!!.toDouble()).toInt()

        return purchases
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

    fun calculateClusterInstructions(cluster: Material, count: Int): Pair<List<Instruction>, Int> {
        val materials = calculateRecipeCrafting(cluster, count)
        val instructions: MutableList<Instruction> = mutableListOf()
        var gloop = 0

        materials.forEach {
            instructions.add(Instruction(
                type = InstructionType.PURCHASE,
                material = it.first,
                count = it.second
            ))
            gloop += gloopForRawMaterial(it.first, it.second) ?: 0
        }
        instructions.add(Instruction(
            type = InstructionType.CRAFT,
            material = cluster,
            count = count
        ))

        return instructions to gloop
    }

    fun calculateSingularityInstructions(count: Int): Pair<List<Instruction>, Int> {
        val materials = calculateRecipeCrafting(Material.MATERIAL_SINGULARITY, count)
        val instructions: MutableList<Instruction> = mutableListOf()
        var gloop = 0

        materials.forEach {
            val clusterData = calculateClusterInstructions(it.first, it.second)
            clusterData.first.forEach { instruction -> instructions.add(instruction) }
            gloop += clusterData.second
        }

        instructions.add(Instruction(
            type = InstructionType.CRAFT,
            material = Material.MATERIAL_SINGULARITY,
            count = count
        ))
        return instructions to gloop
    }

    fun calculatePowerShardInstructions(shard: Material, count: Int): Pair<List<Instruction>, Int> { // hello and welcome to ~~recursion~~NVM hell
        val instructions: MutableList<Instruction> = mutableListOf()

        val purchases: HashMap<Material, Int> = hashMapOf()
        val crafts: HashMap<Material, Int> = hashMapOf()

        var gloop = 0

        val lowerShards: MutableMap<Material, Int> = mutableMapOf()
        var lowestShard: Pair<Material, Int>

        Rarity.entries.forEach {
            purchases[shardFromRarity(it)] = 0
            crafts[shardFromRarity(it)] = 0
        }

        repeat(count) { _ ->
            if ((tempInfinibag[shardFromRarity(Rarity.entries[shard.rarity.ordinal - 1]).label]?.count ?: 0) >= 2) {
                tempInfinibag[shardFromRarity(Rarity.entries[shard.rarity.ordinal - 1]).label]!!.count -= 2
                crafts[shard] = crafts[shard]!! + 1
                return@repeat
            }


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
                    Item(name = lowerShard.label, count = 0, isCosmeticToken = false)
                if (tempInfinibag[upperShard.label] == null) tempInfinibag[upperShard.label] =
                    Item(name = upperShard.label, count = 0, isCosmeticToken = false)

                if (lowerCount == 0) return@forEach

                if (lowerCount % 2 == 0) { // even!
                    val craftAmount = lowerCount / 2

                    tempInfinibag[lowerShard.label]!!.count -= craftAmount * 2

                    crafts[upperShard] = crafts[upperShard]!! + craftAmount
                    tempInfinibag[upperShard.label]!!.count += craftAmount

                    if (updateAbove) lowerShards[upperShard] = lowerShards[upperShard]!! + craftAmount
                    lowerShards[lowerShard] = 0
                } else { // odd!
                    val craftAmount = (lowerCount + 1) / 2

                    purchases[lowerShard] = purchases[lowerShard]!! + 1
                    gloop += lowerShard.marketPrice!!
                    tempInfinibag[lowerShard.label]!!.count++

                    crafts[upperShard] = crafts[upperShard]!! + craftAmount
                    tempInfinibag[upperShard.label]!!.count += craftAmount
                    tempInfinibag[lowerShard.label]!!.count -= craftAmount * 2

                    if (updateAbove) lowerShards[upperShard] = lowerShards[upperShard]!! + craftAmount
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

            if (crafts[shard]!! > 0) instructions.add(Instruction(
                type = InstructionType.CRAFT,
                material = shard,
                count = crafts[shard]!!
            ))

            if (purchases[shard]!! > 0) instructions.add(Instruction(
                type = InstructionType.PURCHASE,
                material = shard,
                count = purchases[shard]!!
            ))
        }

        return instructions to gloop
    }
}