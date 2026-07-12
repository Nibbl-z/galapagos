package xyz.nibblz.galapagos.features

import kotlinx.serialization.Serializable
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.world.item.ItemStack
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.data.Rarity
import xyz.nibblz.galapagos.events.ContainerCloseEvent
import xyz.nibblz.galapagos.events.ContainerOpenEvent
import xyz.nibblz.galapagos.events.SlotClickEvent
import xyz.nibblz.galapagos.events.SystemChatEvent
import xyz.nibblz.galapagos.mixin.accessor.HoveredSlotAccessor
import xyz.nibblz.galapagos.screens.QuestHistory
import xyz.nibblz.galapagos.util.Glyphs
import xyz.nibblz.galapagos.util.findLore
import xyz.nibblz.galapagos.util.playMccSound
import kotlin.reflect.KMutableProperty0
import kotlin.time.Clock

object QuestTracking : Feature {
    override val id: String = "quest_tracking"
    override val name: String = "Quest Tracking"
    override val description: List<Component> = listOf(
        Component.literal("Logs all completed quests and daily meter claims, including their rarity as well as if the quest is boosted/glitched/arcane."),
        Component.literal("To view past quests, right click on the Island Rewards tab at the bottom of the journal."),
        Component.empty(),
        Component.literal("Note: Disabling this feature will NOT disable quest tracking, but will disable the quest history menu.")
    )
    override val enabledProperty: KMutableProperty0<Boolean> = Config.values::questTrackingEnabled
    override val image: Config.ConfigImage = Config.ConfigImage("quest_tracking.png", 1097, 465)

    override fun init() {
        ContainerOpenEvent.EVENT.register { packet -> containerOpen(packet) }
        ContainerCloseEvent.EVENT.register { containerClose() }
        SlotClickEvent.EVENT.register { screen, _, _, button -> slotClick(screen, button) }
        SystemChatEvent.EVENT.register { packet -> systemChat(packet) }
        ItemTooltipCallback.EVENT.register { stack, _, _, components -> tooltipAdd(stack, components) }
        ClientTickEvents.END_CLIENT_TICK.register {
            if (!openQuestHistory) return@register
            openQuestHistory = false
            // im getting sick of it for the new disease
            Minecraft.getInstance().setScreen(QuestHistory())
        }
    }

    enum class QuestingRewardSource(val label: String) {
        DAILY_QUEST("Daily Quest"),
        WEEKLY_QUEST("Weekly Quest"),
        QUEST_SCROLL("Quest Scroll"),
        DAILY_METER("Daily Meter"),
        WEEKLY_VAULT("Weekly Vault")
    }

    enum class QuestingRewardBonus(val label: String) {
        NONE(""),
        BOOSTED("Boosted"),
        GLITCHED("Glitched"),
        ARCANE("Arcane")
    }

    @Serializable
    data class QuestingReward(
        var source: QuestingRewardSource = QuestingRewardSource.DAILY_QUEST,
        var bonus: QuestingRewardBonus = QuestingRewardBonus.NONE,
        var rarity: Rarity = Rarity.COMMON,
        var timestamp: Long = 0
    ) {
        fun getIcon(): String {
            return when(source) {
                QuestingRewardSource.DAILY_QUEST, QuestingRewardSource.WEEKLY_QUEST -> when(bonus) {
                    QuestingRewardBonus.NONE -> "island_interface/quest_log/daily/${rarity.name.lowercase()}.png"
                    QuestingRewardBonus.BOOSTED -> "island_interface/quest_log/boosted/${rarity.name.lowercase()}.png"
                    QuestingRewardBonus.ARCANE -> "island_interface/quest_log/arcane/${rarity.name.lowercase()}.png"
                    else -> "island_interface/quest_log/daily/${rarity.name.lowercase()}"
                }
                QuestingRewardSource.QUEST_SCROLL -> "island_items/infinibag/quest_scroll/${rarity.name.lowercase()}.png"
                QuestingRewardSource.DAILY_METER -> "island_interface/quest_log/daily/daily_meter_4.png"
                QuestingRewardSource.WEEKLY_VAULT -> "island_interface/quest_log/meters/daily_vault_full.png" // mhm, the "daily vault"
            }
        }

        fun getLabel(): String {
            return when(source) {
                QuestingRewardSource.DAILY_QUEST,
                QuestingRewardSource.WEEKLY_QUEST,
                QuestingRewardSource.DAILY_METER -> "${rarity.label}${if (bonus != QuestingRewardBonus.NONE) " ${bonus.label}" else ""} ${source.label}"
                QuestingRewardSource.QUEST_SCROLL -> "${rarity.label} ${source.label}"
                QuestingRewardSource.WEEKLY_VAULT -> "Weekly Vault"
            }
        }
    }

    var checkDailyMeter = false
    var hasMccPlus = false
    var clickedQuestHistory = false
    var openQuestHistory = false

    fun containerOpen(packet: ClientboundContainerSetContentPacket) {
        val screen = Minecraft.getInstance().screen ?: return
        if (!screen.title.string.contains("ISLAND REWARDS")) return

        val favorites = packet.items[43]
        if (favorites.findLore("Click to Select Favorites")) {
            hasMccPlus = true
        }
    }

    fun containerClose() {
        if (!clickedQuestHistory) return
        clickedQuestHistory = false
        openQuestHistory = true
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
        if (item.itemName.string.contains("Weekly Quest")) return QuestingRewardSource.WEEKLY_QUEST
        if (item.itemName.string.contains("Quest Scroll")) return QuestingRewardSource.QUEST_SCROLL
        if (item.itemName.string.contains("Daily Meter")) return QuestingRewardSource.DAILY_METER
        if (item.itemName.string.contains("Weekly Vault")) return QuestingRewardSource.WEEKLY_VAULT

        return QuestingRewardSource.DAILY_QUEST // fallback, i guess
    }

    fun slotClick(screen: ContainerScreen, button: Int) {
        val slot = (screen as HoveredSlotAccessor).`galapagos$hoveredSlot`() ?: return

        if (screen.title.string.contains("JOURNAL") || screen.title.string.contains("MAILBOX")) {
            if (slot.item.itemName.string.contains("Island Rewards") && button == 1) {
                if (!enabledProperty.get()) return
                clickedQuestHistory = true
                playMccSound("ui.click_normal")
                playMccSound("ui.quest_complete")
                Minecraft.getInstance().connection!!.send(ServerboundContainerClosePacket(Minecraft.getInstance().player!!.containerMenu.containerId))
                return
            }
        }

        if (!screen.title.string.contains("ISLAND REWARDS")) return

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

    fun tooltipAdd(stack: ItemStack, components: MutableList<Component>) {
        if (!enabledProperty.get()) return
        val screen = Minecraft.getInstance().screen ?: return
        if (!screen.title.string.contains("JOURNAL") && !screen.title.string.contains("MAILBOX")) return
        if (stack.itemName.string != "Island Rewards") return

        var index = components.indexOfFirst { it.string.contains("minecraft:") } // if you have f3+h on :P
        if (index == -1) { index = components.size - 1 } // if you dont !

        components.add(index, Component.empty())

        components.add(index + 1, Component.empty()
            .append(Glyphs.getGlyphComponent("_fonts/icon/click_action_right.png"))
            .append(Component.literal(" > ").withColor(ChatFormatting.DARK_GRAY.color!!))
            .append(Component.literal("Right-Click to ").withColor(0xecd584))
            .append(Component.literal("View Quest History").withColor(0xfee761)))
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