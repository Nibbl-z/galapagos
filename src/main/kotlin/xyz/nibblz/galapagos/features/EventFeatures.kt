package xyz.nibblz.galapagos.features

import com.noxcrew.sheeplib.DialogContainer
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.world.item.ItemStack
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.data.Rarity
import xyz.nibblz.galapagos.dialogs.SeaMonstersOrdersDialog
import xyz.nibblz.galapagos.events.ContainerOpenEvent
import xyz.nibblz.galapagos.events.ContainerSetSlotEvent
import xyz.nibblz.galapagos.events.ScoreboardTitleUpdateEvent
import xyz.nibblz.galapagos.events.SystemChatEvent
import xyz.nibblz.galapagos.util.Vector2
import xyz.nibblz.galapagos.util.findLore
import xyz.nibblz.galapagos.util.findLores

object EventFeatures : Feature {
    override val id: String = "event_features"
    override val name: String = "Event Features"
    override val description: List<Component> = listOf(
        Component.literal("A catch-all feature for anything relating to limited-time events. Currently, this feature only includes a window that displays current fish orders, as well as the waypoint icon for each fish under the orders, during Sea Monsters events.")
    )
    override val image: Config.ConfigImage = Config.ConfigImage("event_features.png", 291, 240)

    data class SeaMonsterOrder(
        val rarity: Rarity,
        val boosted: Boolean,
        val fish: HashMap<String, Int>
    )

    var seaMonstersOrdersDialog: SeaMonstersOrdersDialog? = null
    var seaMonstersOrdersDialogPosition: Vector2 = Vector2(10, 200)
    var onSeaMonstersIsland: Boolean = false
    val activeOrders: MutableList<SeaMonsterOrder?> = mutableListOf(null, null, null)
    val fishCounts: HashMap<String, Int> = hashMapOf()
    val orderRequirementRegex = Regex("(?<current>\\d+)/(?<required>\\d+) \\[(?<fish>.+)]")
    val fishCatchRegex = Regex("You caught: \\[(?<fish>.+)]")

    fun updateOrder(index: Int, item: ItemStack) {
        if (index !in 0..2) return
        if (item.itemName.string.contains("cooldown")) {
            activeOrders[index] = null
            return
        }

        val rarity = Rarity.entries.find { item.findLore(it.tooltipGlyph()) } ?: Rarity.COMMON
        val boosted = item.get(DataComponents.ITEM_MODEL)?.path?.contains("boosted") ?: false
        val fish: HashMap<String, Int> = hashMapOf()

        item.findLores(orderRequirementRegex).forEach {
            val current = it["current"]?.value?.toIntOrNull() ?: 0
            val required = it["required"]?.value?.toIntOrNull() ?: 0
            val fishName = it["fish"]?.value ?: "?"

            fish[fishName] = required
            fishCounts[fishName] = current
        }

        activeOrders[index] = SeaMonsterOrder(rarity, boosted, fish)
    }

    fun refreshSeaMonstersDialog() {
        if (!onSeaMonstersIsland || !enabled) {
            seaMonstersOrdersDialog?.close()
            return
        }

        seaMonstersOrdersDialogPosition = Vector2(seaMonstersOrdersDialog?.x ?: 10, seaMonstersOrdersDialog?.y ?: 200)

        if (seaMonstersOrdersDialog != null) {
            seaMonstersOrdersDialog!!.close()
        }

        seaMonstersOrdersDialog = SeaMonstersOrdersDialog(seaMonstersOrdersDialogPosition.x, seaMonstersOrdersDialogPosition.y)
        DialogContainer += seaMonstersOrdersDialog!!
    }

    override fun init() {
        ContainerOpenEvent.EVENT.register { packet -> containerOpen(packet) }
        ContainerSetSlotEvent.EVENT.register { packet -> containerSetSlot(packet) }
        ScoreboardTitleUpdateEvent.EVENT.register { title -> scoreboardTitleUpdate(title) }
        SystemChatEvent.EVENT.register { packet -> systemChat(packet) }
    }

    fun containerOpen(packet: ClientboundContainerSetContentPacket) {
        val screen = Minecraft.getInstance().screen ?: return

        if (screen.title.string.contains("EVENT ORDERS")) {
            updateOrder(0, packet.items[22])
            updateOrder(1, packet.items[23])
            updateOrder(2, packet.items[24])

            refreshSeaMonstersDialog()
        }
    }

    fun scoreboardTitleUpdate(title: String) {
        onSeaMonstersIsland = title.contains("SEA MONSTERS", false)
        refreshSeaMonstersDialog()
    }

    fun containerSetSlot(packet: ClientboundContainerSetSlotPacket) {
        val screen = Minecraft.getInstance().screen ?: return

        if (screen.title.string.contains("EVENT ORDERS")) {
            if (packet.slot in 22..24) updateOrder(packet.slot - 22, packet.item)
            refreshSeaMonstersDialog()
        }
    }

    fun systemChat(packet: ClientboundSystemChatPacket) {
        if (onSeaMonstersIsland) {
            val match = fishCatchRegex.find(packet.content.string) ?: return
            val fish = match.groups["fish"]?.value ?: return
            fishCounts[fish] = (fishCounts[fish] ?: 0) + if (packet.content.string.contains("x2")) 2 else 1

            refreshSeaMonstersDialog()
        }
    }
}