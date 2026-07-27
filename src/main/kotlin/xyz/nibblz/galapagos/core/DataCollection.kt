package xyz.nibblz.galapagos.core

import kotlinx.serialization.json.Json
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.events.ContainerOpenEvent
import xyz.nibblz.galapagos.util.Glyphs
import xyz.nibblz.galapagos.util.findLore

// BIG GALAPAGOS IS HARVESTING YOUR DATA AND SENDING IT STRAIGHT TO THE SCAVENGER KING!!!
// im kidding
// for the fearful ones looking at the source code, this is a development tool for anything i write
// that automatically Collects Data from the game itself
// so i dont need to suffer as much when making certain things

object DataCollection : CoreFeature {
    override fun init() {
        ContainerOpenEvent.EVENT.register { packet -> containerSetSlot(packet) }
    }

    val badgeSprites: HashMap<String, String> = hashMapOf()
    val fishSprites: HashMap<String, String> = hashMapOf()

    fun containerSetSlot(packet: ClientboundContainerSetContentPacket) {
        fetchBadgeSprites(packet)
        fetchFishSprites(packet)
    }

    fun fetchBadgeSprites(packet: ClientboundContainerSetContentPacket) {
        val screen = Minecraft.getInstance().screen ?: return
        if (!screen.title.string.contains("GAME PROGRESSION")) return

        packet.items.forEach {
            if (!it.findLore(Glyphs.getGlyph("_fonts/icon/tooltips/badge.png"))) return@forEach
            badgeSprites[it.itemName.string] = it.get(DataComponents.ITEM_MODEL)?.path ?: "erm"
        }

        Galapagos.logger.info(Json.encodeToString(badgeSprites))
    }

    fun fetchFishSprites(packet: ClientboundContainerSetContentPacket) {
        val screen = Minecraft.getInstance().screen ?: return
        if (!screen.title.string.contains("A.N.G.L.R. PANEL")) return

        packet.items.forEach {
            if (!it.findLore(Glyphs.getGlyph("_fonts/icon/tooltips/fish.png"))) return@forEach
            fishSprites[it.itemName.string] = it.get(DataComponents.ITEM_MODEL)?.path ?: "erm"
        }

        Galapagos.logger.info(Json.encodeToString(fishSprites))
    }
}