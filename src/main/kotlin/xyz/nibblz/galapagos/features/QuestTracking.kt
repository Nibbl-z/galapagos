package xyz.nibblz.galapagos.features

import kotlinx.serialization.Serializable
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.core.component.DataComponents
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
import xyz.nibblz.galapagos.screens.VaultHistory
import xyz.nibblz.galapagos.util.Glyphs
import xyz.nibblz.galapagos.util.findLore
import xyz.nibblz.galapagos.util.getItemCount
import xyz.nibblz.galapagos.util.playMcciSound
import kotlin.reflect.KMutableProperty0
import kotlin.time.Clock

object QuestTracking : Feature {
    override val id: String = "quest_tracking"
    override val name: String = "Quest Tracking"
    override val description: List<Component> = listOf(
        Component.literal("Logs all completed quests and daily meter claims, including their rarity as well as if the quest is boosted/glitched/arcane. Weekly vaults are also tracked, showing how many of each crate was earned, as well as how many claims were stored in the vault."),
        Component.empty(),
        Component.literal("To view past quests, click on the info icon at the top-right of the journal. To view past weekly vaults, click on the weekly vault."),
        Component.empty(),
        Component.literal("Note: Disabling this feature will NOT disable quest tracking, but will disable the quest/vault history menu.")
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
            if (openQuestHistory) {
                openQuestHistory = false
                // im getting sick of it plain old disease
                Minecraft.getInstance().setScreen(QuestHistory())
            }

            if (openVaultHistory) {
                openVaultHistory = false
                // its not my life but i will live it how i please
                Minecraft.getInstance().setScreen(VaultHistory())
            }
        }
    }

    enum class QuestingRewardSource(val label: String, val mult: Int) {
        DAILY_QUEST("Daily Quest", 1),
        WEEKLY_QUEST("Weekly Quest", 5),
        QUEST_SCROLL("Quest Scroll", 1),
        DAILY_METER("Daily Meter", 1),
    }

    enum class QuestingRewardBonus(val label: String, val mult: Int) {
        NONE("", 1),
        BOOSTED("Boosted", 2),
        GLITCHED("Glitched", 2),
        ARCANE("Arcane", 10)
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
            }
        }

        fun getLabel(): String {
            return when(source) {
                QuestingRewardSource.DAILY_QUEST,
                QuestingRewardSource.WEEKLY_QUEST,
                QuestingRewardSource.DAILY_METER -> "${rarity.label}${if (bonus != QuestingRewardBonus.NONE) " ${bonus.label}" else ""} ${source.label}"
                QuestingRewardSource.QUEST_SCROLL -> "${rarity.label} ${source.label}"
            }
        }

        fun getExpectedChatMessage(): String {
            val count = when(source) {
                QuestingRewardSource.DAILY_QUEST,
                QuestingRewardSource.WEEKLY_QUEST,
                QuestingRewardSource.QUEST_SCROLL,
                QuestingRewardSource.DAILY_METER -> source.mult * bonus.mult * if (hasMccPlus) 2 else 1
            }

            return "[${rarity.label} Reward Crate]${if (count > 1) " x${count}" else ""}"
        }
    }

    @Serializable
    data class WeeklyVault(
        var timestamp: Long = 0,
        var rewards: HashMap<Rarity, Int> = hashMapOf(),
        var anomalies: Int = 0, // yea because there's a chance for more than 1 anomaly. I $$$$ING WISH!!!!!!! I LOve anomaly anomaly yum
        // yum yum i loveanomaliyies iarcane anomly arcanee:3anomallyyyyy mmmm arcane anaomalyyyyy
        // I'm fine.
        var claims: Int = 0,
        var maxClaims: Int = 20
    )

    var checkDailyMeter = false
    var checkWeeklyVault = false
    var clickedQuest: QuestingReward? = null
    var hasMccPlus = false

    var clickedQuestHistory = false
    var openQuestHistory = false
    var clickedVaultHistory = false
    var openVaultHistory = false

    fun containerOpen(packet: ClientboundContainerSetContentPacket) {
        val screen = Minecraft.getInstance().screen ?: return
        if (!screen.title.string.contains("ISLAND REWARDS")) return

        val favorites = packet.items[43]
        if (favorites.findLore("Click to Select Favorites")) {
            hasMccPlus = true
        }

        if (checkWeeklyVault && screen.title.string.contains("SUMMARY")) {
            checkWeeklyVault = false
            // TODO: MAKE SURE THIS WORKS! IT IS COMPLETELY UNTESTED!
            // and it SURE AS HELL BETTER WORK THE FIRST TIME because then i'll have to wait a week, aka 0.05 modrinth review periods

            val vault = WeeklyVault(
                timestamp = Clock.System.now().epochSeconds,
                claims = WeeklyVaultInfo.claims,
                maxClaims = WeeklyVaultInfo.maxClaims
            )

            packet.items.forEach {
                if (it.itemName.string == "Arcane Anomaly") {
                    vault.anomalies++
                } else {
                    val rarity = Rarity.entries.find { rarity -> it.itemName.string.contains(rarity.label) } ?: Rarity.COMMON
                    vault.rewards[rarity] = it.getItemCount()
                }
            }

            Galapagos.save.weeklyVaultHistory.add(vault)
        }
    }

    fun containerClose() {
        if (clickedQuestHistory) {
            clickedQuestHistory = false
            openQuestHistory = true
        }

        if (clickedVaultHistory) {
            clickedVaultHistory = false
            openVaultHistory = true
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
        if (item.itemName.string.contains("Weekly Quest")) return QuestingRewardSource.WEEKLY_QUEST
        if (item.itemName.string.contains("Quest Scroll")) return QuestingRewardSource.QUEST_SCROLL
        if (item.itemName.string.contains("Daily Meter")) return QuestingRewardSource.DAILY_METER

        return QuestingRewardSource.DAILY_QUEST // fallback, i guess
    }

    fun slotClick(screen: ContainerScreen, button: Int) {
        val slot = (screen as HoveredSlotAccessor).`galapagos$hoveredSlot`() ?: return

        if (screen.title.string.contains("JOURNAL") || screen.title.string.contains("MAILBOX")) {
            if (slot.item.itemName.string.contains("Island Rewards") && button == 0 && slot.index == 8) {
                if (!enabledProperty.get()) return
                clickedQuestHistory = true
                playMcciSound("ui.click_normal")
                playMcciSound("ui.quest_complete")
                Minecraft.getInstance().connection!!.send(ServerboundContainerClosePacket(Minecraft.getInstance().player!!.containerMenu.containerId))
                return
            }
        }

        if (!screen.title.string.contains("ISLAND REWARDS")) return

        val source = getRewardSource(slot.item)

        if (slot.item.itemName.string == "Weekly Vault" && !slot.item.findLore("Click to Claim")) {
            if (!enabledProperty.get()) return
            clickedVaultHistory = true
            playMcciSound("ui.click_normal")
            Minecraft.getInstance().connection!!.send(ServerboundContainerClosePacket(Minecraft.getInstance().player!!.containerMenu.containerId))
            return
        }

        if (slot.item.findLore("Click to Claim")) {
            if (slot.item.itemName.string.contains("Quest")) {
                var rarity: Rarity = Rarity.COMMON
                val bonus = getQuestBonus(slot.item)

                Rarity.entries.forEach {
                    if (slot.item.findLore(it.tooltipGlyph())) {
                        rarity = it
                    }
                }

                clickedQuest = QuestingReward(
                    rarity = rarity,
                    source = source,
                    bonus = bonus,
                    timestamp = Clock.System.now().epochSeconds
                )
            } else if (slot.item.itemName.string.contains("Daily Meter")) {
                checkDailyMeter = true
            } else if (slot.item.itemName.string.contains("Weekly Vault")) {
                checkWeeklyVault = true
            }
        }
    }

    fun tooltipAdd(stack: ItemStack, components: MutableList<Component>) {
        if (!enabledProperty.get()) return
        val screen = Minecraft.getInstance().screen ?: return
        if (!screen.title.string.contains("JOURNAL") && !screen.title.string.contains("MAILBOX")) return

        if (stack.itemName.string == "Island Rewards" && stack.get(DataComponents.ITEM_MODEL)?.path?.contains("blank") == false) {
            var index = components.indexOfFirst { it.string.contains("minecraft:") } // if you have f3+h on :P
            if (index == -1) { index = components.size - 1 } // if you dont !

            components.add(index, Component.empty())

            components.add(index + 1, Component.empty()
                .append(Glyphs.getGlyphComponent("_fonts/icon/click_action_left.png"))
                .append(Component.literal(" > ").withColor(ChatFormatting.DARK_GRAY.color!!))
                .append(Component.literal("Click to ").withColor(0xecd584))
                .append(Component.literal("View Quest History").withColor(0xfee761)))
        }

        if (stack.itemName.string == "Weekly Vault" && !components.any {it.string.contains("Click to Claim")}) {
            var index = components.indexOfFirst { it.string.contains("minecraft:") } // if you have f3+h on :P
            if (index == -1) { index = components.size - 1 } // if you dont !

            components.add(index, Component.empty())

            components.add(index + 1, Component.empty()
                .append(Glyphs.getGlyphComponent("_fonts/icon/click_action_left.png"))
                .append(Component.literal(" > ").withColor(ChatFormatting.DARK_GRAY.color!!))
                .append(Component.literal("Click to ").withColor(0xecd584))
                .append(Component.literal("View Vault History").withColor(0xfee761)))
        }
    }

    fun systemChat(packet: ClientboundSystemChatPacket) {
        if (clickedQuest != null) {
            if (!packet.content.string.contains(clickedQuest?.getExpectedChatMessage() ?: "")) return
            Galapagos.save.questHistory.add(clickedQuest!!)
            clickedQuest = null
        }

        if (checkDailyMeter) {
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

            Galapagos.logger.info("$rarity $count $bonus")

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
}