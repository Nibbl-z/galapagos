package xyz.nibblz.galapagos.features

import dev.isxander.yacl3.api.OptionDescription
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.resources.Identifier
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.util.Glyphs
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.data.BlueprintLootPreview
import xyz.nibblz.galapagos.data.ConstantIslandData
import xyz.nibblz.galapagos.data.CosmeticTag
import xyz.nibblz.galapagos.data.LootPreviewCosmetic
import xyz.nibblz.galapagos.data.arcaneCoresPerRollTooltip
import xyz.nibblz.galapagos.data.bonusCoresPerScavenge
import xyz.nibblz.galapagos.data.getRep
import xyz.nibblz.galapagos.data.mythicCoresPerRollTooltip
import xyz.nibblz.galapagos.data.newCosmeticTooltip
import xyz.nibblz.galapagos.data.newRepTooltip
import xyz.nibblz.galapagos.data.render
import xyz.nibblz.galapagos.data.repPerDonation
import xyz.nibblz.galapagos.data.trophiesPerRollTooltip
import xyz.nibblz.galapagos.data.update
import xyz.nibblz.galapagos.events.ContainerCloseEvent
import xyz.nibblz.galapagos.events.ContainerOpenEvent
import xyz.nibblz.galapagos.events.ContainerRenderEvent
import xyz.nibblz.galapagos.events.SlotClickEvent
import xyz.nibblz.galapagos.events.SlotRenderEvent
import xyz.nibblz.galapagos.mixin.accessor.HoveredSlotAccessor
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty0

object CrateChances: Feature {
    override val id: String = "crate_chances"
    override val name: String = "Crate Chances"
    override val description: List<Component> = listOf(
        Component.literal("Displays more statistics in the Crate Emporium menu for each crate, including:"),
        Component.literal("- New cosmetic chance"),
        Component.literal("- New royal reputation chance"),
        Component.literal("- Average trophies per roll"),
        Component.literal("- Average mythic cores per roll"),
        Component.literal("- Average arcane cores per roll"),
        Component.empty(),
        Component.literal("The crate with the highest chance for a new cosmetic or new royal reputation will be highlighted with a yellow star and a purple star respectively. The best chance is determined for both standard and exclusive crates seperately, showing both of these stars for both types of crate.")
    )
    override val enabledProperty: KMutableProperty0<Boolean> = Config.values::crateChancesEnabled
    override val image: Config.ConfigImage = Config.ConfigImage("crate_chances.png", 636, 639)

    var currentCrate: String? = null
    var data: HashMap<String, BlueprintLootPreview> = hashMapOf()

    var exclusiveCrates: MutableList<String> = mutableListOf()
    var bestRepChance: Pair<String, Double> = Pair("", 0.0)
    var bestCosmeticChance: Pair<String, Double> = Pair("", 0.0)
    var bestExclusiveRepChance: Pair<String, Double> = Pair("", 0.0)
    var bestExclusiveCosmeticChance: Pair<String, Double> = Pair("", 0.0)

    override fun init() {
        ContainerRenderEvent.EVENT.register { screen, graphics, x, y, w, h -> containerRender(screen, graphics, x, y, w, h) }
        ContainerCloseEvent.EVENT.register { containerClose() }
        ContainerOpenEvent.EVENT.register { packet -> containerOpen(packet) }
        SlotClickEvent.EVENT.register { screen, input, _, _ -> slotClick(screen, input) }
        ItemTooltipCallback.EVENT.register { stack, context, flag, components -> tooltipAdd(stack, context, flag, components) }
        SlotRenderEvent.EVENT.register { extractor, slot -> slotRender(extractor, slot) }
    }

    fun containerOpen(packet: ClientboundContainerSetContentPacket) {
        val screen = Minecraft.getInstance().screen ?: return
        if (!screen.title.string.contains("CRATE EMPORIUM", false)) return

        bestRepChance = Pair("", 0.0)
        bestCosmeticChance = Pair("", 0.0)

        ConstantIslandData.data.crateEmporium.forEach { (crateName, crateItems) ->
            val lootPreviewCosmetics: MutableList<LootPreviewCosmetic> = mutableListOf()
            var isExclusive = false

            crateItems.forEach crateForEach@{
                val savedCosmetic = Galapagos.save.cosmetics[it.name] ?: return@crateForEach
                if (!isExclusive && savedCosmetic.tag == CosmeticTag.EXCLUSIVE) isExclusive = true

                val lootPreviewCosmetic = LootPreviewCosmetic(
                    chance = it.chance,
                    isOwned = savedCosmetic.isOwned,
                    trophies = savedCosmetic.rarity.trophies,
                    rep = savedCosmetic.getRep(),
                    perDonation = savedCosmetic.repPerDonation(),
                    mythicCores = savedCosmetic.bonusCoresPerScavenge() * if (savedCosmetic.tag == CosmeticTag.STANDARD) 1.0 else 10.0,
                    arcaneCores = savedCosmetic.bonusCoresPerScavenge() * if (savedCosmetic.tag == CosmeticTag.STANDARD) 0.05 else 1.0,
                )

                lootPreviewCosmetics.add(lootPreviewCosmetic)
            }

            val crateData = BlueprintLootPreview()
            crateData.update(lootPreviewCosmetics)

            data[crateName] = crateData

            if (crateName == "Mythic Cosmetic Crate") return@forEach

            if (isExclusive) {
                exclusiveCrates.add(crateName)

                if (bestExclusiveCosmeticChance.second < crateData.newCosmeticChance)
                    bestExclusiveCosmeticChance = Pair(crateName, crateData.newCosmeticChance)
                if (crateData.newCosmeticChance == 0.0 && bestExclusiveRepChance.second < crateData.newRepChance)
                    bestExclusiveRepChance = Pair(crateName, crateData.newRepChance)
            } else {
                if (bestCosmeticChance.second < crateData.newCosmeticChance)
                    bestCosmeticChance = Pair(crateName, crateData.newCosmeticChance)
                if (crateData.newCosmeticChance == 0.0 && bestRepChance.second < crateData.newRepChance)
                    bestRepChance = Pair(crateName, crateData.newRepChance)
            }

        }
    }

    fun containerClose() {
        currentCrate = null
    }

    fun slotClick(screen: ContainerScreen, input: ContainerInput) {
        if (!screen.title.string.contains("CRATE EMPORIUM", false)) return
        val slot = (screen as HoveredSlotAccessor).`galapagos$hoveredSlot`() ?: return

        currentCrate = slot.item.itemName.string
    }

    fun tooltipAdd(stack: ItemStack, context: Item.TooltipContext, flag: TooltipFlag, components: MutableList<Component>) {
        if (!enabledProperty.get()) return
        val screen = Minecraft.getInstance().screen ?: return
        if (!screen.title.string.contains("CRATE EMPORIUM", false)) return
        if (!stack.itemName.string.contains("Crate")) return
        val itemName = stack.itemName.string

        var index = 7

        if (Config.values::showNewCosmeticChance.get()) {
            components.add(index, data[itemName]?.newCosmeticTooltip() ?: Component.empty())
            index++
        }

        if (Config.values::showNewRepChance.get()) {
            components.add(index, data[itemName]?.newRepTooltip() ?: Component.empty())
            index++
        }

        if (Config.values::showTrophiesPerRoll.get()) {
            components.add(index, data[itemName]?.trophiesPerRollTooltip() ?: Component.empty())
            index++
        }

        if (Config.values::showMythicCoresPerRoll.get()) {
            components.add(index, data[itemName]?.mythicCoresPerRollTooltip() ?: Component.empty())
            index++
        }

        if (Config.values::showArcaneCoresPerRoll.get()) {
            components.add(index, data[itemName]?.arcaneCoresPerRollTooltip() ?: Component.empty())
            index++
        }

        val message = when(itemName) {
            bestCosmeticChance.first -> if (Config.values::highlightBestCosmeticChance.get()) Triple(
                "Best Standard Cosmetic Chance",
                "_fonts/icon/star.png",
                ChatFormatting.YELLOW.color!!
            ) else null
            bestRepChance.first -> if (Config.values::highlightBestRepChance.get()) Triple(
                "Best Standard Rep Chance",
                "_fonts/icon/emojis/star_purple.png",
                0x9143f0
            ) else null
            bestExclusiveCosmeticChance.first -> if (Config.values::highlightBestCosmeticChance.get()) Triple(
                "Best Exclusive Cosmetic Chance",
                "_fonts/icon/star.png",
                ChatFormatting.YELLOW.color!!
            ) else null
            bestExclusiveRepChance.first -> if (Config.values::highlightBestRepChance.get()) Triple(
                "Best Exclusive Rep Chance",
                "_fonts/icon/emojis/star_purple.png",
                0x9143f0
            ) else null
            else -> null
        } ?: return

        components.add(index, Component.empty())
        components.add(index + 1, Component.empty()
            .append(Glyphs.getGlyphComponent(message.second))
            .append(Component.literal(" ${message.first}").withColor(message.third)))
    }

    fun containerRender(screen: ContainerScreen, graphics: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int) {
        if (!enabledProperty.get()) return
        if (!screen.title.string.contains("LOOT PREVIEW", true)) return
        if (currentCrate == null) return

        data[currentCrate]?.render(graphics, x, y, w, Config.values::showNewCosmeticChance.get(), Config.values::showNewRepChance.get())
    }

    fun slotRender(graphics: GuiGraphicsExtractor, slot: Slot) {
        if (!enabledProperty.get()) return
        val screen = Minecraft.getInstance().screen ?: return
        if (!screen.title.string.contains("CRATE EMPORIUM", false)) return
        if (!slot.item.itemName.string.contains("Crate")) return

        val itemName = slot.item.itemName.string

        val crateData = data[itemName] ?: return
        var maxSprite: String? = null

        if (crateData.newRepChance == 0.00 && Config.values::showMaxRepCrates.get())
            maxSprite = "textures/_fonts/icon/royal_reputation.png"
        else if (crateData.newCosmeticChance == 0.00 && Config.values::showMaxCosmeticCrates.get())
            maxSprite = "textures/_fonts/icon/trophy/purple.png"

        if (maxSprite != null) {
            graphics.pose().pushMatrix()
            val pose = graphics.pose()
            pose.scaleAround(0.67f, (slot.x + 6).toFloat(), (slot.y - 1).toFloat())
            // DevCmb laughed a lot when i wrote "0.67f" for this value. :smile:

            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                Identifier.fromNamespaceAndPath("mcc", maxSprite),
                slot.x + 6,
                slot.y - 1,
                0f, 0f,
                16, 16,
                16, 16
            )

            pose.scaleAround(0.85f, (slot.x + 6).toFloat(), (slot.y - 1).toFloat())
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                Identifier.fromNamespaceAndPath("mcc", "textures/_fonts/icon/accept.png"),
                slot.x + 8,
                slot.y,
                0f, 0f,
                16, 16,
                16, 16
            )

            graphics.pose().popMatrix()
        }

        var bestSprite: String? = null

        if ((bestCosmeticChance.first == itemName || bestExclusiveCosmeticChance.first == itemName) && Config.values::highlightBestCosmeticChance.get())
            bestSprite = "textures/_fonts/icon/star.png"
        else if ((bestRepChance.first == itemName || bestExclusiveRepChance.first == itemName) && Config.values::highlightBestRepChance.get())
            bestSprite = "textures/_fonts/icon/emojis/star_purple.png"

        if (bestSprite != null) {
            graphics.pose().pushMatrix()
            val pose = graphics.pose()
            pose.scaleAround(0.5f, (slot.x).toFloat(), (slot.y).toFloat())
            // DevCmb laughed a lot when i wrote "0.67f" for this value. :smile:
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                Identifier.fromNamespaceAndPath("mcc", bestSprite),
                slot.x - 4,
                slot.y + 16,
                0f, 0f,
                16, 16,
                16, 16
            )
            graphics.pose().popMatrix()
        }
    }
}