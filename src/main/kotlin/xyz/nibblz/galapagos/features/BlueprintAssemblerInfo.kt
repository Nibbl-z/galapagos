package xyz.nibblz.galapagos.features

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.util.ARGB
import net.minecraft.world.inventory.ContainerInput
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.Glyphs
import xyz.nibblz.galapagos.data.CosmeticCore
import xyz.nibblz.galapagos.data.bonusCoresPerScavenge
import xyz.nibblz.galapagos.data.coreConversions
import xyz.nibblz.galapagos.data.coresPerScavenge
import xyz.nibblz.galapagos.data.repPerDonation
import xyz.nibblz.galapagos.events.ContainerCloseEvent
import xyz.nibblz.galapagos.events.ContainerOpenEvent
import xyz.nibblz.galapagos.events.ContainerRenderEvent
import xyz.nibblz.galapagos.events.SlotClickEvent
import xyz.nibblz.galapagos.mixin.accessor.HoveredSlotAccessor
import java.util.EnumMap
import kotlin.math.round

object BlueprintAssemblerInfo : Feature {
    override val id: String = "blueprint_assembler_info"
    override val name: String = "Blueprint Assembler Info"

    override fun init() {
        ContainerOpenEvent.EVENT.register { packet -> containerOpen(packet) }
        ContainerRenderEvent.EVENT.register { screen, graphics, x, y, w, h -> containerRender(screen, graphics, x, y, w, h) }
        SlotClickEvent.EVENT.register { screen, input, _, _ -> slotClick(screen, input) }
        ContainerCloseEvent.EVENT.register { containerClose() }
    }

    var directCores = EnumMap<CosmeticCore, Double>(CosmeticCore::class.java)
    var convertedCores = EnumMap<CosmeticCore, Double>(CosmeticCore::class.java)
    var repTrophies = 0
    var newCosmeticTrophies = 0

    var displayData = false

    fun containerOpen(packet: ClientboundContainerSetContentPacket) {
        val screen = Minecraft.getInstance().screen ?: return

        if (!screen.title.string.contains("BLUEPRINT ASSEMBLER") && !screen.title.string.contains("INFINIBAG")) displayData = false

        if (screen.title.string.contains("BLUEPRINT ASSEMBLER")) {
            updateData()
        }
    }

    fun containerClose() {
        displayData = false
    }

    fun slotClick(screen: ContainerScreen, input: ContainerInput) {
        val slot = (screen as HoveredSlotAccessor).`galapagos$hoveredSlot`() ?: return

        if (screen.title.string.contains("BLUEPRINT ASSEMBLER") && slot.item.itemName.string == "Select a Blueprint") {
            displayData = true
        }
    }

    fun updateData() {
        directCores = CosmeticCore.entries.associateWithTo(EnumMap(CosmeticCore::class.java)) { 0.0 }
        convertedCores = CosmeticCore.entries.associateWithTo(EnumMap(CosmeticCore::class.java)) { 0.0 }
        repTrophies = 0
        newCosmeticTrophies = 0

        Galapagos.save.infinibag.forEach { (_, item) ->
            if (!item.name.contains("Blueprint: ")) return@forEach
            val cosmetic = Galapagos.save.cosmetics[item.name.dropLast(6).drop(11)] ?: return@forEach

            var owned = cosmetic.isOwned
            var donations = cosmetic.donations

            repeat(item.count) {
                if (!cosmetic.isOwned && !owned) {
                    newCosmeticTrophies += cosmetic.rarity.trophies
                    owned = true
                    return@repeat
                }

                if (owned && cosmetic.donations != cosmetic.tag.maxDonations) {
                    repTrophies += cosmetic.repPerDonation()
                    donations++
                }

                if (!owned) return@repeat
                directCores[cosmetic.tag.core] = directCores[cosmetic.tag.core]!! + cosmetic.coresPerScavenge()
                directCores[cosmetic.tag.bonusCore] = directCores[cosmetic.tag.bonusCore]!! + cosmetic.bonusCoresPerScavenge()

                convertedCores[cosmetic.tag.core] = convertedCores[cosmetic.tag.core]!! + cosmetic.coresPerScavenge()
                convertedCores[cosmetic.tag.bonusCore] = convertedCores[cosmetic.tag.bonusCore]!! + cosmetic.bonusCoresPerScavenge()

                convertedCores.forEach coreConversion@{ (core, _) ->
                    val coreConversion = coreConversions[cosmetic.tag.core to core]
                    Galapagos.logger.info("from ${cosmetic.tag.core} to $core , you will receieve $coreConversion")
                    if (coreConversion != null) convertedCores[core] = (convertedCores[core] ?: 0.0) + coreConversion * cosmetic.coresPerScavenge()

                    val bonusCoreConversion = coreConversions[cosmetic.tag.bonusCore to core]
                    Galapagos.logger.info("from ${cosmetic.tag.bonusCore} to $core , you will receieve $bonusCoreConversion")
                    if (bonusCoreConversion != null) convertedCores[core] = (convertedCores[core] ?: 0.0) + bonusCoreConversion * cosmetic.bonusCoresPerScavenge()
                }
            }
        }
    }

    fun containerRender(screen: ContainerScreen, graphics: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int) {
        if (!screen.title.string.contains("BLUEPRINT ASSEMBLER") && !displayData) return

        var yOffset = if (screen.title.string.contains("INFINIBAG")) 130 else 30

        graphics.text(Minecraft.getInstance().font, Component.literal("New Cosmetic Trophies: $newCosmeticTrophies ").append(
            Glyphs.getGlyphComponent("_fonts/icon/trophy/purple.png")), x + w + 2, y + yOffset, ARGB.opaque(0x66fc56), true)

        yOffset += 15

        graphics.text(Minecraft.getInstance().font, Component.literal("Royal Rep: $repTrophies ").append(
            Glyphs.getGlyphComponent("_fonts/icon/royal_reputation.png")), x + w + 2, y + yOffset, ARGB.opaque(0x9143f0), true)

        yOffset += 15

        CosmeticCore.entries.forEach {
            graphics.text(Minecraft.getInstance().font, Component.literal("${it.label}s: ${round(directCores[it]!! * 1000) / 1000.0} (${round(convertedCores[it]!! * 1000) / 1000.0} converted) ")
                .append(it.getComponent()), x + w + 2, y + yOffset, ARGB.opaque(it.color), true)

            yOffset += 15
        }
    }
}