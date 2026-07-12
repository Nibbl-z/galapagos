package xyz.nibblz.galapagos.features

import dev.isxander.yacl3.api.OptionDescription
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.util.Glyphs
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.data.Rarity
import xyz.nibblz.galapagos.events.ContainerOpenEvent
import xyz.nibblz.galapagos.events.ContainerSetSlotEvent
import xyz.nibblz.galapagos.events.SlotClickEvent
import xyz.nibblz.galapagos.util.findLore
import xyz.nibblz.galapagos.mixin.accessor.HoveredSlotAccessor
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KMutableProperty0

object ExchangeUnitPrice : Feature {
    override val id: String = "exchange_unit_price"
    override val name: String = "Island Exchange Unit Price"
    override val description: List<Component> = listOf(
        Component.literal("Shows the unit price of listings on the Island Exchange, as well as the equivalent of cosmetic/weapon skin listings in Style Souls and Weapon Wisps.")
    )
    override val enabledProperty: KMutableProperty0<Boolean> = Config.values::exchangeUnitPriceEnabled
    override val image: Config.ConfigImage = Config.ConfigImage("exchange_unit.png", 425, 906)

    override fun init() {
        ItemTooltipCallback.EVENT.register { stack, context, flag, components -> tooltipAdd(stack, context, flag, components) }
        ContainerOpenEvent.EVENT.register { packet -> containerOpen(packet) }
        ContainerSetSlotEvent.EVENT.register { packet -> containerSetSlot(packet) }
        SlotClickEvent.EVENT.register { screen, input, _, _ -> slotClick(screen, input) }
    }

    val perUnitPrices: MutableMap<ItemStack, Int> = mutableMapOf()
    val soulEquivalent: MutableMap<ItemStack, Int> = mutableMapOf()
    val wispEquivalent: MutableMap<ItemStack, Int> = mutableMapOf()

    fun containerOpen(packet: ClientboundContainerSetContentPacket) {
        val screen = Minecraft.getInstance().screen ?: return
        if (!screen.title.string.contains("ISLAND EXCHANGE", false)) return

        perUnitPrices.clear()

        packet.items.forEach {
            //if (it.count == 1) return@forEach
            getData(it)
        }
    }

    fun getData(item: ItemStack) {
        val regex = Regex("Listed Price: .(?<price>[\\d,]+)")
        val priceString = item.findLore(regex)?.get("price")?.value ?: return
        val cleanedString = priceString.replace(",", "")
        val price = cleanedString.toInt()

        perUnitPrices[item] = (price / item.count)

        var rarity: Rarity = Rarity.COMMON

        Rarity.entries.forEach {
            if (item.findLore(it.tooltipGlyph())) {
                rarity = it
            }
        }

        if (item.findLore("Chroma Set:")) { // all weapon skins have this lore so its easier to detect that
            var stars = 0
            if (item.findLore("Elimination Effect:")) stars = 1
            if (item.findLore("Eliminations:")) stars = 2
            if (item.findLore("Weapon Evolution")) stars = 3

            wispEquivalent[item] = when(rarity) {
                Rarity.RARE -> 1
                Rarity.EPIC -> when(stars) {
                    1 -> 3
                    else -> 2
                }
                Rarity.LEGENDARY -> when(stars) {
                    1 -> 7
                    2 -> 10
                    else -> 5
                }
                Rarity.MYTHIC -> when(stars) {
                    1 -> 20
                    2 -> 30
                    3 -> 40
                    else -> 15
                }
                else -> 1
            }
        } else {
            if (item.findLore("wardrobe") && !item.findLore(Glyphs.getGlyph("_fonts/icon/tooltips/collector.png"))) {
                soulEquivalent[item] = when(rarity) {
                    Rarity.RARE -> 1
                    Rarity.EPIC -> 2
                    Rarity.LEGENDARY -> 5
                    Rarity.MYTHIC -> 15
                    else -> 1
                }
            }
        }

        if (item.itemName.string.contains("Elimination Effect") or item.itemName.string.contains("Chroma Set")) {
            wispEquivalent[item] = 2
        }
    }

    fun containerSetSlot(packet: ClientboundContainerSetSlotPacket) {
        val screen = Minecraft.getInstance().screen ?: return
        if (!screen.title.string.contains("ISLAND EXCHANGE", false)) return

        val item = packet.item
        //if (item.count == 1) return

        getData(item)
    }

    fun clear() {
        perUnitPrices.clear()
        soulEquivalent.clear()
        wispEquivalent.clear()
    }

    fun slotClick(screen: ContainerScreen, type: ContainerInput) {
        if (!screen.title.string.contains("ISLAND EXCHANGE", false)) return
        val slot = (screen as HoveredSlotAccessor).`galapagos$hoveredSlot`() ?: return

        if (slot.item.itemName.string.contains("Back")) clear()
        if (slot.item.itemName.string.contains("Main Filter")) clear()
        if (slot.item.itemName.string.contains("Filter by Rarity")) clear()
        if (slot.item.itemName.string.contains("Filter by Type")) clear()
        if (slot.item.itemName.string.contains("Refresh Listings")) clear()
        if (slot.item.itemName.string.contains("Next Page")) clear()
        if (slot.item.itemName.string.contains("Previous Page")) clear()
    }

    fun tooltipAdd(item: ItemStack, context: Item.TooltipContext, flag: TooltipFlag, list: MutableList<Component>) {
        if (!enabledProperty.get()) return
        val screen = Minecraft.getInstance().screen ?: return
        if (!screen.title.string.contains("ISLAND EXCHANGE", false)) return

        val listedPriceIndex = list.indexOfFirst {
            it.string.contains("Listed Price")
        }
        if (listedPriceIndex == -1) return

        if (item.count != 1 && Config.values::exchangeShowUnitPrice.get()) {
            list.add(listedPriceIndex + 1,
                Component.literal("Unit Price: ").withColor(ChatFormatting.GRAY.color!!)
                    .append(Glyphs.getGlyphComponent("_fonts/icon/coin.png"))
                    .append(Component.literal("%,d".format(perUnitPrices[item])).withColor(0xffffff))
            )
        }

        if (soulEquivalent[item] != null && Config.values::exchangeShowSoulEquivalent.get()) {
            val soulUnitPrice = (perUnitPrices[item] ?: 1) / (soulEquivalent[item] ?: 1)

            list.add(listedPriceIndex + 1,
                Component.literal("Soul Equivalent: ").withColor(ChatFormatting.GRAY.color!!)
                    .append(Component.literal("\uE001").withColor(0xffffff).withStyle(Style.EMPTY.withFont(Galapagos.font)))
                    .append(Component.literal((soulEquivalent[item] !! * item.count).toString()).withColor(0xffffff))
                    .append(Component.literal(", "))
                    .append(Glyphs.getGlyphComponent("_fonts/icon/coin.png"))
                    .append(Component.literal("%,d".format(soulUnitPrice) + " per").withColor(0xffffff))
            )
        }

        if (wispEquivalent[item] != null && Config.values::exchangeShowWispEquivalent.get()) {
            val wispUnitPrice = (perUnitPrices[item] ?: 1) / (wispEquivalent[item] ?: 1)

            list.add(listedPriceIndex + 1,
                Component.literal("Wisp Equivalent: ").withColor(ChatFormatting.GRAY.color!!)
                    .append(Component.literal("\uE002").withColor(0xffffff).withStyle(Style.EMPTY.withFont(Galapagos.font)))
                    .append(Component.literal((wispEquivalent[item]!! * item.count).toString()).withColor(0xffffff))
                    .append(Component.literal(", "))
                    .append(Glyphs.getGlyphComponent("_fonts/icon/coin.png"))
                    .append(Component.literal("%,d".format(wispUnitPrice) + " per").withColor(0xffffff))
            )
        }
    }
}