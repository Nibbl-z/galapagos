package xyz.nibblz.galapagos.features

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.Glyphs
import xyz.nibblz.galapagos.PlayerData
import xyz.nibblz.galapagos.PlayerData.getRep
import xyz.nibblz.galapagos.data.BlueprintLootPreview
import xyz.nibblz.galapagos.data.Cosmetic
import xyz.nibblz.galapagos.data.Rarity
import xyz.nibblz.galapagos.data.render
import xyz.nibblz.galapagos.data.update
import xyz.nibblz.galapagos.events.ContainerCloseEvent
import xyz.nibblz.galapagos.events.ContainerOpenEvent
import xyz.nibblz.galapagos.events.ContainerRenderEvent
import xyz.nibblz.galapagos.events.SlotClickEvent
import xyz.nibblz.galapagos.mixin.accessor.HoveredSlotAccessor
import kotlin.math.round

object CosmeticMachineChances : Feature {
    override val id: String = "cosmetic_machine_chances"
    override val name: String = "Cosmetic Machine Chances"

    val BASIC_CHANCES: Map<Rarity, Double> = mapOf(
        Rarity.COMMON to 0.45,
        Rarity.UNCOMMON to 0.30,
        Rarity.RARE to 0.15,
        Rarity.EPIC to 0.08,
        Rarity.LEGENDARY to 0.02
    )
    const val BASIC_EXCLUSIVE_CHANCE = 0.1

    val ULTIMATE_CHANCES: Map<Rarity, Double> = mapOf(
        Rarity.RARE to 0.50,
        Rarity.EPIC to 0.35,
        Rarity.LEGENDARY to 0.12,
        Rarity.MYTHIC to 0.03
    )
    const val ULTIMATE_EXCLUSIVE_CHANCE = 0.3
    const val ULTIMATE_ARCANE_CHANCE = 0.01

    fun getChance(isUltimate: Boolean, rarity: Rarity, tag: PlayerData.CosmeticTag): Double? {
        return if (isUltimate) {
            ULTIMATE_CHANCES[rarity]?.times(when(tag) {
                PlayerData.CosmeticTag.STANDARD -> 1.0 - ULTIMATE_EXCLUSIVE_CHANCE - if (rarity == Rarity.MYTHIC) ULTIMATE_ARCANE_CHANCE else 0.0
                PlayerData.CosmeticTag.EXCLUSIVE -> ULTIMATE_EXCLUSIVE_CHANCE
                PlayerData.CosmeticTag.ARCANE -> ULTIMATE_ARCANE_CHANCE
            })
        } else {
            BASIC_CHANCES[rarity]?.times(if (tag == PlayerData.CosmeticTag.STANDARD) 1.0 - when(rarity) {
                Rarity.COMMON, Rarity.UNCOMMON -> 0.0
                else -> BASIC_EXCLUSIVE_CHANCE
            } else BASIC_EXCLUSIVE_CHANCE)
        }
    }

    override fun init() {
        ContainerOpenEvent.EVENT.register { packet -> containerOpen(packet) }
        ContainerCloseEvent.EVENT.register { containerClose() }
        SlotClickEvent.EVENT.register { screen, input -> slotClick(screen, input) }
        ItemTooltipCallback.EVENT.register { stack, context, flag, components -> tooltipAdd(stack, context, flag, components) }
        ContainerRenderEvent.EVENT.register { screen, graphics, x, y, w, h -> containerRender(screen, graphics, x, y, w, h) }
    }

    var inCosmeticMachine = false
    var isUltimate = false

    var basicCosmetics: MutableMap<String, Cosmetic> = mutableMapOf()
    var ultimateCosmetics: MutableMap<String, Cosmetic> = mutableMapOf()

    var basicData = BlueprintLootPreview()
    var ultimateData = BlueprintLootPreview()

    fun slotClick(screen: ContainerScreen, type: ContainerInput) {
        val slot = (screen as HoveredSlotAccessor).`galapagos$hoveredSlot`() ?: return

        if (slot.item.itemName.string.contains("Basic Pull")) isUltimate = false
        if (slot.item.itemName.string.contains("Ultimate Pull")) isUltimate = true
    }

    fun containerOpen(packet: ClientboundContainerSetContentPacket) { updateItems() }

    fun updateItems() {
        val screen = Minecraft.getInstance().screen ?: return
        if (screen.title.string.contains("COSMETIC MACHINE")) inCosmeticMachine = true
        if (!screen.title.string.contains("LOOT PREVIEW")) return
        if (!inCosmeticMachine) return

        val cosmeticCounts: HashMap<Pair<Rarity, PlayerData.CosmeticTag>, Int> = hashMapOf()

        Galapagos.save.cosmetics.forEach { (_, it) ->
            cosmeticCounts[Pair(it.rarity, it.tag)] = (cosmeticCounts[Pair(it.rarity, it.tag)] ?: 0) + 1
        }

        Galapagos.logger.info(cosmeticCounts.toString())

        //<Again!>
        Galapagos.save.cosmetics.forEach { (_, it) ->
            if (it.rarity != Rarity.MYTHIC) {
                val basicChance = getChance(false, it.rarity, it.tag)

                if (basicChance != null) {
                    //Galapagos.logger.info("BASIC: ${it.name} - ${basicChance / cosmeticCounts[(Pair(it.rarity, it.tag))]!!}")
                    basicCosmetics[it.name] = Cosmetic(
                        chance = basicChance / cosmeticCounts[(Pair(it.rarity, it.tag))]!!,
                        isOwned = it.isOwned,
                        trophies = it.rarity.trophies,
                        rep = it.getRep()
                    )
                } else Galapagos.logger.warn("Failed to get basic pull chance for ${it.name}")
            }

            if (it.rarity != Rarity.COMMON && it.rarity != Rarity.UNCOMMON) {
                val ultimateChance = getChance(true, it.rarity, it.tag)

                if (ultimateChance != null) {
                    //Galapagos.logger.info("ULTIMATE: ${it.name} - ${ultimateChance / cosmeticCounts[(Pair(it.rarity, it.tag))]!!}")
                    ultimateCosmetics[it.name] = Cosmetic(
                        chance = ultimateChance / cosmeticCounts[(Pair(it.rarity, it.tag))]!!,
                        isOwned = it.isOwned,
                        trophies = it.rarity.trophies,
                        rep = it.getRep()
                    )
                } else Galapagos.logger.warn("Failed to get ultimate pull chance for ${it.name}")
            }
        }

//        items.forEach {
//            if (it.isEmpty) return@forEach
//            if (it.itemName.string == "Back") return@forEach
//            if (it.itemName.string == "Loot Preview") return@forEach
//            if (it.itemName.string == "Next Page") return@forEach
//            if (it.itemName.string == "Previous Page") return@forEach
//
//            val cosmetic = it.getCosmetic() ?: return@forEach
//            if (isUltimate) {
//                ultimateCosmetics[it.itemName.string] = cosmetic
//            } else {
//                basicCosmetics[it.itemName.string] = cosmetic
//            }
//        }

        basicData.update(basicCosmetics.entries.map {it.value})
        ultimateData.update(ultimateCosmetics.entries.map {it.value})
    }

    fun containerClose() {
        inCosmeticMachine = false
    }

    fun getModifiedChanceTooltip(rarity: Rarity, isUltimate: Boolean): Component {
        val chance = if (isUltimate) ULTIMATE_CHANCES[rarity]!! else BASIC_CHANCES[rarity]!!
        val exclusiveChance = if (isUltimate) ULTIMATE_EXCLUSIVE_CHANCE else BASIC_EXCLUSIVE_CHANCE
        val arcaneChance = if (isUltimate && rarity == Rarity.MYTHIC) ULTIMATE_ARCANE_CHANCE else 0.0

        val fixedChance = round((chance * (1.0 - exclusiveChance - arcaneChance) * 100.0) * 1000) / 1000.0
        val fixedExclusiveChance = round(chance * exclusiveChance * 100.0 * 1000) / 1000.0

        return Component.literal(" • ").withColor(ChatFormatting.DARK_GRAY.color!!)
                .append(Component.literal("$fixedChance%").withColor(ChatFormatting.GRAY.color!!))
                .append(Component.literal(" - ").withColor(ChatFormatting.DARK_GRAY.color!!))
                .append(Component.literal(rarity.label).withColor(rarity.color)
                .append(Component.literal(" [").withColor(ChatFormatting.DARK_GRAY.color!!))
                .append(Component.literal("$fixedExclusiveChance% ").withColor(ChatFormatting.GRAY.color!!))
                .append(Component.literal("\uE000").withColor(0xFFFFFF).withStyle(Style.EMPTY.withFont(Galapagos.font)))
                .append(Component.literal("]").withColor(ChatFormatting.DARK_GRAY.color!!)))
    }

    fun tooltipAdd(item: ItemStack, context: Item.TooltipContext, flag: TooltipFlag, list: MutableList<Component>) {
        if (!inCosmeticMachine) return

        if(item.itemName.string == "Basic Pull") {
            list[7] = getModifiedChanceTooltip(Rarity.RARE, false)
            list[8] = getModifiedChanceTooltip(Rarity.EPIC, false)
            list[9] = getModifiedChanceTooltip(Rarity.LEGENDARY, false)

            // devcmb told me to use repeat (2) but idont wanna
            list.removeAt(11)
            list.removeAt(11)
        }

        if(item.itemName.string == "Ultimate Pull") {
            list[5] = getModifiedChanceTooltip(Rarity.RARE, true)
            list[6] = getModifiedChanceTooltip(Rarity.EPIC, true)
            list[7] = getModifiedChanceTooltip(Rarity.LEGENDARY, true)
            list[8] = getModifiedChanceTooltip(Rarity.MYTHIC, true)
            list.add(9, Component.literal(" • ").withColor(ChatFormatting.DARK_GRAY.color!!)
                .append(Component.literal("0.03%").withColor(ChatFormatting.GRAY.color!!))
                .append(Component.literal(" - ").withColor(ChatFormatting.DARK_GRAY.color!!))
                .append(Glyphs.getGlyphComponent("_fonts/icon/tooltips/arcane.png")))

            list.removeAt(11)
            list.removeAt(11)
            list.removeAt(11)
        }
    }

    fun containerRender(screen: ContainerScreen, graphics: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int) {
        if (!screen.title.string.contains("LOOT PREVIEW", true)) return
        if (!inCosmeticMachine) return

        val data = if (isUltimate) ultimateData else basicData

        data.render(graphics, x, y, w)
    }
}