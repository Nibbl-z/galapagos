package xyz.nibblz.galapagos.features

import kotlinx.serialization.Serializable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.Glyphs
import xyz.nibblz.galapagos.data.Rarity
import xyz.nibblz.galapagos.events.ContainerOpenEvent
import xyz.nibblz.galapagos.events.SlotClickEvent
import xyz.nibblz.galapagos.events.SystemChatEvent
import xyz.nibblz.galapagos.findLore
import xyz.nibblz.galapagos.mixin.accessor.HoveredSlotAccessor
import kotlin.time.Clock

object QuestTracking : Feature {
    override val id: String = "quest_tracking"
    override val name: String = "Quest Tracking"

    override fun init() {
        ContainerOpenEvent.EVENT.register { packet -> containerOpen(packet) }
        SlotClickEvent.EVENT.register { screen, input -> slotClick(screen, input) }
        SystemChatEvent.EVENT.register { packet -> systemChat(packet) }
    }

    enum class QuestingRewardSource(val label: String) {
        DAILY_QUEST("Daily Quest"),
        WEELY_QUEST("Weekly Quest"),
        QUEST_SCROLL("Quest Scroll"),
        DAILY_METER("Daily Meter"),
        WEEKLY_VAULT("Weekly Vault")
    }

    enum class QuestingRewardBonus {
        NONE,
        BOOSTED,
        GLITCHED,
        ARCANE
    }

    @Serializable
    data class QuestingReward(
        var source: QuestingRewardSource = QuestingRewardSource.DAILY_QUEST,
        var bonus: QuestingRewardBonus = QuestingRewardBonus.NONE,
        var rarity: Rarity = Rarity.COMMON,
        var timestamp: Long = 0
    )

    var checkDailyMeter = false
    var hasMccPlus = false

    fun containerOpen(packet: ClientboundContainerSetContentPacket) {
        // todo: this should probably be in like a "player data" class, to store stuff like
        // - owned/max rep cosmetics
        // - mcc+ status
        // - ...idk what else
        val screen = Minecraft.getInstance().screen ?: return
        if (!screen.title.string.contains("ISLAND REWARDS")) return

        val favorites = packet.items[43]
        if (favorites.findLore("Click to Select Favorites")) {
            hasMccPlus = true
        }
    }

    fun getQuestBonus(item: ItemStack): QuestingRewardBonus {
        if (item.itemName.string.contains("Daily Quest")) {
            if (item.findLore("2x")) return QuestingRewardBonus.BOOSTED
            if (item.findLore("10x")) return QuestingRewardBonus.ARCANE
        }

        if (item.itemName.string.contains("Weekly Quest")) {
            if (item.findLore("10x")) return QuestingRewardBonus.BOOSTED
            if (item.findLore("50x")) return QuestingRewardBonus.ARCANE
        }

        return QuestingRewardBonus.NONE
    }

    fun getRewardSource(item: ItemStack): QuestingRewardSource {
        if (item.itemName.string.contains("Daily Quest")) return QuestingRewardSource.DAILY_QUEST
        if (item.itemName.string.contains("Weekly Quest")) return QuestingRewardSource.WEELY_QUEST
        if (item.itemName.string.contains("Quest Scroll")) return QuestingRewardSource.QUEST_SCROLL
        if (item.itemName.string.contains("Daily Meter")) return QuestingRewardSource.DAILY_METER
        if (item.itemName.string.contains("Weekly Vault")) return QuestingRewardSource.WEEKLY_VAULT

        return QuestingRewardSource.DAILY_QUEST // fallback, i guess
    }

    fun slotClick(screen: ContainerScreen, type: ContainerInput) {
        if (!screen.title.string.contains("ISLAND REWARDS")) return

        val slot = (screen as HoveredSlotAccessor).`galapagos$hoveredSlot`() ?: return

        val source = getRewardSource(slot.item)

        if (slot.item.findLore("Click to Claim")) {
            if (slot.item.itemName.string.contains("Quest")) {
                var rarity: Rarity = Rarity.COMMON
                val bonus = getQuestBonus(slot.item)

                Rarity.entries.forEach {
                    if (slot.item.findLore(it.tooltipGlyph())) {
                        rarity = it
                    }
                }

                val reward = QuestingReward(
                    rarity = rarity,
                    source = source,
                    bonus = bonus,
                    timestamp = Clock.System.now().epochSeconds
                )

                Galapagos.logger.info("$rarity, $bonus, $source")

                Galapagos.save.questHistory.add(reward)
            } else if (slot.item.itemName.string.contains("Daily Meter")) {
                checkDailyMeter = true
            }
        }
    }

    fun systemChat(packet: ClientboundSystemChatPacket) {
        if (!checkDailyMeter) return
        if (!packet.content.string.contains("Reward Crate", false)) return

        var rarity: Rarity = Rarity.COMMON

        Rarity.entries.forEach {
            if (packet.content.string.contains(it.name, true)) {
                rarity = it
            }
        }

        val regex = Regex("\\d+")
        val match = regex.find(packet.content.string) ?: return

        val count = match.groups[0]?.value?.toInt()
        val bonus = when(count) {
            1 -> QuestingRewardBonus.NONE
            2 -> if (hasMccPlus) QuestingRewardBonus.NONE else QuestingRewardBonus.GLITCHED
            4 -> QuestingRewardBonus.GLITCHED
            10, 20 -> QuestingRewardBonus.ARCANE
            else -> QuestingRewardBonus.NONE
        }

        val source = QuestingRewardSource.DAILY_METER

        Galapagos.logger.info("$rarity, $bonus, $source")
        val reward = QuestingReward(
            rarity = rarity,
            source = source,
            bonus = bonus,
            timestamp = Clock.System.now().epochSeconds
        )

        Galapagos.save.questHistory.add(reward)
        checkDailyMeter = false
    }
}