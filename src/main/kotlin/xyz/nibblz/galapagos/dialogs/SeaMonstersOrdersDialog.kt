package xyz.nibblz.galapagos.dialogs

import com.noxcrew.sheeplib.dialog.Dialog
import com.noxcrew.sheeplib.dialog.title.TextTitleWidget
import com.noxcrew.sheeplib.layout.linear
import com.noxcrew.sheeplib.theme.Themed
import com.noxcrew.sheeplib.widget.TextWidgets
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.network.chat.Component
import xyz.nibblz.galapagos.data.Area
import xyz.nibblz.galapagos.data.FISH_PER_AREA
import xyz.nibblz.galapagos.features.EventFeatures
import xyz.nibblz.galapagos.util.mcciTextureComponent

class SeaMonstersOrdersDialog(x: Int, y: Int) : Dialog(x, y), Themed by GalapagosTheme {
    override fun layout() = linear(LinearLayout.Orientation.VERTICAL) {
        val font = Minecraft.getInstance().font

        EventFeatures.activeOrders.filterNotNull().forEach {
            +StringWidget(Component.empty()
                .append(mcciTextureComponent("island_interface/quest_log/${if (it.boosted) "boosted" else "daily"}/${it.rarity.name.lowercase()}"))
                .append(Component.literal(" ${it.rarity.label} Fish Order").withColor(it.rarity.color)),
            font)

            var fishComponent = Component.empty()
            var lines = 0
            it.fish.entries.sortedBy { (fish, _) ->
                val area = Area.entries.find { area -> FISH_PER_AREA[area]!!.contains(fish) }
                val rarity = FISH_PER_AREA[area]!![fish]!! // !!!11!!111!!!!!

                rarity.ordinal
            }.forEach { (fish, count) ->
                lines++

                val area = Area.entries.find { area -> FISH_PER_AREA[area]!!.contains(fish) }
                val rarity = FISH_PER_AREA[area]!![fish]!! // !!!11!!111!!!!!

                fishComponent = fishComponent
                    .append(Component.literal("• ").withColor(ChatFormatting.DARK_GRAY.color!!))
                    .append(Component.literal("${EventFeatures.fishCounts[fish]}").withColor(
                        if ((EventFeatures.fishCounts[fish] ?: 0) < count) ChatFormatting.RED.color!! else ChatFormatting.GREEN.color!!
                    ))
                    .append(Component.literal("/$count "))
                    .append(Component.literal("[$fish] ").withColor(rarity.color))
                    .append(mcciTextureComponent("island_interface/button/heart/${area!!.heart}"))

                if (lines != it.fish.size) {
                    fishComponent = fishComponent.append(Component.literal("\n"))
                }
            }

            +TextWidgets.multiLine(fishComponent)
        }

        if (EventFeatures.activeOrders.filterNotNull().isEmpty()) {
            +StringWidget(Component.empty()
                .append(mcciTextureComponent("island_interface/generic/clock_used"))
                .append(Component.literal(" No orders found!")),
            font)
            +StringWidget(Component.literal("Open Event Orders tab to update menu."), font)
        }
    }

    override val title = TextTitleWidget(this,
        Component.empty()
            .append(mcciTextureComponent("island_interface/navigator/sea_monster_event"))
            .append(Component.literal(" Orders"))
    )
}