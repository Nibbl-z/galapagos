package xyz.nibblz.galapagos.features

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.data.*
import xyz.nibblz.galapagos.events.ContainerCloseEvent
import xyz.nibblz.galapagos.events.ContainerOpenEvent
import xyz.nibblz.galapagos.events.ContainerRenderEvent
import xyz.nibblz.galapagos.events.SlotClickEvent
import xyz.nibblz.galapagos.mixin.accessor.HoveredSlotAccessor
import xyz.nibblz.galapagos.util.Glyphs
import java.util.*
import kotlin.math.round
import kotlin.reflect.KMutableProperty0

object BlueprintAssemblerInfo : Feature {
    override val id: String = "blueprint_assembler_info"
    override val name: String = "Blueprint Assembler Info"
    override val description: List<Component> = listOf(
        Component.literal("Displays more statistics inside of the Blueprint Assembler menu, including:"),
        Component.literal("- New style trophies"),
        Component.literal("- New royal reputation"),
        Component.literal("- Average cosmetic cores from scavenging, including ")
            .append(Component.literal("Standard Cores, ").withColor(CosmeticCore.STANDARD.color))
            .append(Component.literal("Exclusive Cores, ").withColor(CosmeticCore.EXCLUSIVE.color))
            .append(Component.literal("Mythic Cores, ").withColor(CosmeticCore.MYTHIC.color))
            .append(Component.literal("and ").withColor(0xFFFFFF))
            .append(Component.literal("Arcane Cores.").withColor(CosmeticCore.ARCANE.color)),
        Component.empty(),
        Component.literal("The amount of cosmetic cores earned if all cores are converted towards that specific type of core is also shown."),
        Component.literal("For example, if your current owned blueprints will yield 10 standard cores and 1 exclusive core from scavenging, exclusive cores will be labelled as \"1 exclusive core, 2 converted\"")

    )
    override val enabledProperty: KMutableProperty0<Boolean> = Config.values::assemblerInfoEnabled
    override val image: Config.ConfigImage = Config.ConfigImage("assembler_info.png", 842, 364)


    override fun init() {
        ContainerOpenEvent.EVENT.register { containerOpen() }
        ContainerRenderEvent.EVENT.register { screen, graphics, x, y, w, _ -> containerRender(screen, graphics, x, y, w) }
        SlotClickEvent.EVENT.register { screen, _, _, _ -> slotClick(screen) }
        ContainerCloseEvent.EVENT.register { containerClose() }
    }

    val cosmeticCoreSettings: HashMap<CosmeticCore, KMutableProperty0<Config.AssemblerCoreInfoType>>
        get() {
            return hashMapOf(
                CosmeticCore.STANDARD to Config.values::assemblerInfoStandardCores,
                CosmeticCore.EXCLUSIVE to Config.values::assemblerInfoExclusiveCores,
                CosmeticCore.MYTHIC to Config.values::assemblerInfoMythicCores,
                CosmeticCore.ARCANE to Config.values::assemblerInfoArcaneCores
            )
        }

    var directCores = EnumMap<CosmeticCore, Double>(CosmeticCore::class.java)
    var convertedCores = EnumMap<CosmeticCore, Double>(CosmeticCore::class.java)
    var repTrophies = 0
    var newCosmeticTrophies = 0

    var displayData = false

    fun containerOpen() {
        val screen = Minecraft.getInstance().screen ?: return

        if (!screen.title.string.contains("BLUEPRINT ASSEMBLER") && !screen.title.string.contains("INFINIBAG")) displayData = false

        if (screen.title.string.contains("BLUEPRINT ASSEMBLER")) {
            cosmeticCoreSettings.forEach {
                Galapagos.logger.info("${it.key} ${it.value.get()}")
            }

            updateData()
        }
    }

    fun containerClose() {
        displayData = false
    }

    fun slotClick(screen: ContainerScreen) {
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
                    if (coreConversion != null) convertedCores[core] = (convertedCores[core] ?: 0.0) + coreConversion * cosmetic.coresPerScavenge()

                    val bonusCoreConversion = coreConversions[cosmetic.tag.bonusCore to core]
                    if (bonusCoreConversion != null) convertedCores[core] = (convertedCores[core] ?: 0.0) + bonusCoreConversion * cosmetic.bonusCoresPerScavenge()
                }
            }
        }
    }

    fun containerRender(screen: ContainerScreen, graphics: GuiGraphicsExtractor, x: Int, y: Int, w: Int) {
        if (!enabledProperty.get()) return
        if (!screen.title.string.contains("BLUEPRINT ASSEMBLER") && !displayData) return

        var yOffset = if (screen.title.string.contains("INFINIBAG")) 130 else 30

        if (Config.values::assemblerInfoShowNewTrophies.get()) {
            graphics.text(Minecraft.getInstance().font, Component.literal("New Cosmetic Trophies: $newCosmeticTrophies ").append(
                Glyphs.getGlyphComponent("_fonts/icon/trophy/purple.png")), x + w + 2, y + yOffset, ARGB.opaque(0x66fc56), true)

            yOffset += 15
        }

        if (Config.values::assemblerInfoShowNewRep.get()) {
            graphics.text(Minecraft.getInstance().font, Component.literal("Royal Rep: $repTrophies ").append(
                Glyphs.getGlyphComponent("_fonts/icon/royal_reputation.png")), x + w + 2, y + yOffset, ARGB.opaque(0x9143f0), true)

            yOffset += 15
        }

        CosmeticCore.entries.forEach {
            if (cosmeticCoreSettings[it]!!.get() == Config.AssemblerCoreInfoType.DISABLED) return@forEach

            graphics.text(Minecraft.getInstance().font,
                Component.literal("${it.label}s: ${round((directCores[it] ?: 0.0) * 1000) / 1000.0}${if (cosmeticCoreSettings[it]!!.get() == Config.AssemblerCoreInfoType.CONVERSION) " (${round((convertedCores[it] ?: 0.0) * 1000) / 1000.0} converted) " else " "}")
                .append(it.getComponent()), x + w + 2, y + yOffset, ARGB.opaque(it.color), true)

            yOffset += 15
        }
    }
}