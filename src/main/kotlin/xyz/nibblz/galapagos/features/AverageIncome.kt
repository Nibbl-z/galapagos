package xyz.nibblz.galapagos.features

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.util.ARGB
import net.minecraft.world.item.ItemStack
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.data.ARCANE_ANOMALY_CHANCES
import xyz.nibblz.galapagos.data.LUCKY_UPGRADE_CHANCES
import xyz.nibblz.galapagos.data.Rarity
import xyz.nibblz.galapagos.data.StylePerk
import xyz.nibblz.galapagos.data.WEEKLY_VAULT_CHANCES
import xyz.nibblz.galapagos.events.ContainerCloseEvent
import xyz.nibblz.galapagos.events.ContainerOpenEvent
import xyz.nibblz.galapagos.events.ContainerRenderEvent
import xyz.nibblz.galapagos.events.SlotClickEvent
import xyz.nibblz.galapagos.features.WeeklyVaultInfo.claims
import xyz.nibblz.galapagos.features.WeeklyVaultInfo.maxClaims
import xyz.nibblz.galapagos.mixin.accessor.HoveredSlotAccessor
import xyz.nibblz.galapagos.util.Glyphs
import xyz.nibblz.galapagos.util.findLore
import kotlin.math.min
import kotlin.reflect.KMutableProperty0

object AverageIncome : Feature {
    override val id: String = "average_income"
    override val name: String = "Average Income"
    override val description: List<Component> = listOf()
    override val enabledProperty: KMutableProperty0<Boolean> = Config.values::averageIncomeEnabled
    override val image: Config.ConfigImage = Config.ConfigImage("assembler_info.png", 842, 364)

    val CRATE_AVERAGE_COINS: HashMap<Rarity, Int> = hashMapOf(
        Rarity.COMMON to 1000,
        Rarity.UNCOMMON to 2000,
        Rarity.RARE to 5000,
        Rarity.EPIC to 10000,
        Rarity.LEGENDARY to 30000,
        Rarity.MYTHIC to 60000
    )

    val DAILY_LOGIN_CHANCES: HashMap<IntRange, HashMap<Rarity, Double>> = hashMapOf(
        0..0 to hashMapOf(
            Rarity.COMMON to 1.0
        ),
        1..2 to hashMapOf(
            Rarity.COMMON to 0.8,
            Rarity.UNCOMMON to 0.2
        ),
        3..4 to hashMapOf(
            Rarity.COMMON to 0.7,
            Rarity.UNCOMMON to 0.25,
            Rarity.RARE to 0.05
        ),
        5..6 to hashMapOf(
            Rarity.COMMON to 0.6,
            Rarity.UNCOMMON to 0.3,
            Rarity.RARE to 0.1,
        ),
        7..9 to hashMapOf(
            Rarity.COMMON to 0.45,
            Rarity.UNCOMMON to 0.35,
            Rarity.RARE to 0.15,
            Rarity.EPIC to 0.05
        ),
        10..14 to hashMapOf(
            Rarity.UNCOMMON to 0.65,
            Rarity.RARE to 0.25,
            Rarity.EPIC to 0.1
        ),
        // Playing mcci for 2,147,483,647 days breaks Galapagos average daily coins!
        // (and probably many other things, such as nuclear fallout, death of The Sun, the heat death of the universe, etc etc)
        15..Int.MAX_VALUE to hashMapOf(
            Rarity.UNCOMMON to 0.5,
            Rarity.RARE to 0.3,
            Rarity.EPIC to 0.15,
            Rarity.LEGENDARY to 0.05
        )
    )

    var AVERAGE_COINS_PER_ANOMALY = 0.0

    fun averageDailyQuestIncome(): Double {
        return averageIncome(
            LUCKY_UPGRADE_CHANCES[Galapagos.save.stylePerks[StylePerk.LUCKY_QUESTS]!!],
            1,
            Galapagos.save.stylePerks[StylePerk.BOOSTED_QUESTS]!! * 0.05,
            Galapagos.save.stylePerks[StylePerk.ARCANE_QUESTS]!! * 0.005,
            true
        )
    }

    fun averageWeeklyQuestIncome(): Double {
        return averageIncome(
            LUCKY_UPGRADE_CHANCES[Galapagos.save.stylePerks[StylePerk.LUCKY_QUESTS]!!],
            5,
            Galapagos.save.stylePerks[StylePerk.BOOSTED_QUESTS]!! * 0.05,
            Galapagos.save.stylePerks[StylePerk.ARCANE_QUESTS]!! * 0.005,
            true
        )
    }

    fun averageDailyMeterIncome(): Double {
        return averageIncome(
            LUCKY_UPGRADE_CHANCES[Galapagos.save.stylePerks[StylePerk.LUCKY_METER]!!],
            if (Galapagos.save.mccPlus) 2 else 1,
            Galapagos.save.stylePerks[StylePerk.GLITCHED_CLAIMS]!! * 0.05,
            Galapagos.save.stylePerks[StylePerk.ARCANE_CLAIMS]!! * 0.005,
            true
        )
    }

    fun averageWeeklyVaultIncome(claims: Int): Double {
        val chances = WEEKLY_VAULT_CHANCES.entries.find { (range, _) -> claims in range }?.value
            ?: throw IllegalStateException("Claim $claims does not fall in the valid range of weekly vault claims")

        return averageIncome(
            chances,
            if (Galapagos.save.mccPlus) 2 else 1,
            Galapagos.save.stylePerks[StylePerk.GLITCHED_CLAIMS]!! * 0.05,
            Galapagos.save.stylePerks[StylePerk.ARCANE_CLAIMS]!! * 0.005,
            true
        )
    }

    fun averageIncome(chances: HashMap<Rarity, Double>, mult: Int, boostedChance: Double, arcaneChance: Double, includeAnomaly: Boolean = false): Double {
        var totalAverage = 0.0

        chances.forEach { (rarity, chance) ->
            totalAverage += CRATE_AVERAGE_COINS[rarity]!! * chance
        }

        totalAverage *= mult

        var boostedAverage = totalAverage

        boostedAverage += (totalAverage * (boostedChance - (boostedChance * arcaneChance)))
        boostedAverage += (totalAverage * 9 * (boostedChance * arcaneChance))
        if (includeAnomaly) boostedAverage += (AVERAGE_COINS_PER_ANOMALY * Galapagos.save.stylePerks[StylePerk.ARCANE_ANOMALY]!! * 0.00005)

        return boostedAverage
    }

    fun averageCoinsFromAllScrolls(): Int {
        val scrollCounts: HashMap<Rarity, Int> = hashMapOf()

        Rarity.entries.forEach {
            val scroll = Galapagos.save.infinibag["${it.label} Quest Scroll"]

            if (scroll == null) scrollCounts[it] = 0 else scrollCounts[it] = scroll.count
        }

        return scrollCounts.entries.sumOf { (rarity, count) -> CRATE_AVERAGE_COINS[rarity]!! * count }
    }

    fun averageCoinsFromScrolls(scrolls: Int): Int {
        val scrollCounts: HashMap<Rarity, Int> = hashMapOf()

        Rarity.entries.forEach {
            val scroll = Galapagos.save.infinibag["${it.label} Quest Scroll"]

            if (scroll == null) scrollCounts[it] = 0 else scrollCounts[it] = scroll.count
        }

        var coins = 0

        repeat(scrolls) {
            Rarity.entries.reversed().forEach {
                if (scrollCounts[it]!! > 0) {
                    coins += CRATE_AVERAGE_COINS[it]!!
                    scrollCounts[it] = scrollCounts[it]!! - 1
                    return@repeat
                }
            }
        }

        return coins
    }

    fun averageDailyChestIncome(days: Int): Double {
        val chances = DAILY_LOGIN_CHANCES.entries.find { (range, _) -> days in range }?.value
            ?: throw IllegalStateException("Day $days does not fall in the valid range days... Wtf???")

        return averageIncome(chances, 1, 0.0, 0.0)
    }

    var loginStreak = 0
    var openedScrollMenu = false

    override fun init() {
        ARCANE_ANOMALY_CHANCES.forEach { (rarity, chance) ->
            AVERAGE_COINS_PER_ANOMALY += CRATE_AVERAGE_COINS[rarity]!! * chance
        }
        AVERAGE_COINS_PER_ANOMALY *= 50
        ItemTooltipCallback.EVENT.register { stack, _, _, components -> tooltipAdd(stack, components) }
        SlotClickEvent.EVENT.register { screen, _, _, _ -> slotClick(screen) }
        ContainerRenderEvent.EVENT.register { screen, graphics, x, y, w, _ -> containerRender(screen, graphics, x, y, w) }
        ContainerCloseEvent.EVENT.register { containerClose() }
        ContainerOpenEvent.EVENT.register { packet -> containerOpen(packet) }
    }

    fun tooltipAdd(item: ItemStack, components: MutableList<Component>) {
        if (item.itemName.string == "Remaining Daily Quests") handleQuestIncomeTooltip(components, false)
        if (item.itemName.string == "Remaining Weekly Quests") handleQuestIncomeTooltip(components, true)
        if (item.itemName.string == "Daily Meter") handleDailyMeterIncomeTooltip(components)
        if (item.itemName.string == "Weekly Vault") handleWeeklyVaultIncomeTooltip(components)
        if (item.itemName.string == "Island Rewards" && item.get(DataComponents.ITEM_MODEL)?.path?.contains("blank") == true) handleOverallIncomeTooltip(components)
        if (item.itemName.string == "Click to Add a Quest Scroll") handleQuestScrollTooltip(components)
        else if (item.itemName.string.contains(" Quest Scroll")) handleStartedQuestScrollTooltip(item, components)
        if (item.itemName.string == "Daily Login Chest") handleDailyChestTooltip(components)
    }

    fun slotClick(screen: ContainerScreen) {
        val slot = (screen as HoveredSlotAccessor).`galapagos$hoveredSlot`() ?: return

        if (!screen.title.string.contains("ISLAND REWARDS")) return
        if (slot.item.itemName.string != "Click to Add a Quest Scroll") return

        openedScrollMenu = true
    }

    fun containerClose() {
        openedScrollMenu = false
    }

    fun containerOpen(packet: ClientboundContainerSetContentPacket) {
        val screen = Minecraft.getInstance().screen ?: return
        if (!screen.title.string.contains("INFINIBAG")) openedScrollMenu = false

        if (screen.title.string.contains("ISLAND REWARDS")) {
            loginStreak = packet.items[10].findLore(Regex("Current Login Streak: (?<streak>\\d+) Days"))
                ?.get("streak")?.value?.toIntOrNull() ?: 0
        }
    }

    fun handleQuestIncomeTooltip(components: MutableList<Component>, isWeekly: Boolean) {
        val averageIncome = if (isWeekly) averageWeeklyQuestIncome() else averageDailyQuestIncome()

        var index = components.indexOfFirst { it.string.contains("Remaining ${if (isWeekly) "Weekly" else "Daily"} Quests:") }
        if (index == -1) return

        val originalIndex = index

        index++

        components.add(index,
            Component.literal("Average Coins/${if (isWeekly) "Weekly" else "Daily"} Quest: ").withColor(0xfee761)
                .append(Component.literal("%,d".format(averageIncome.toInt())).withColor(0xFFFFFF))
                .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png"))
        )

        if (!isWeekly && Config.values::averageIncomeIncludeQuestScrolls.get()) {
            index++
            components.add(index,
                Component.literal("Average Coins/Daily Quest + Scroll: ").withColor(0xfee761)
                    .append(Component.literal("%,d".format(averageIncome.toInt() + averageCoinsFromScrolls(1))).withColor(0xFFFFFF))
                    .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png"))
            )
        }

        val remaining = Regex("Remaining ${if (isWeekly) "Weekly" else "Daily"} Quests: (?<quests>\\d+)").find(components[originalIndex].string)
            ?.groups?.get("quests")?.value?.toIntOrNull() ?: return

        index++
        components.add(index, Component.literal("Average Coins Remaining: ").withColor(0xfee761)
            .append(Component.literal("%,d".format(averageIncome.toInt() * remaining)).withColor(0xFFFFFF))
            .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png")))

        if (!isWeekly && Config.values::averageIncomeIncludeQuestScrolls.get()) {
            index++
            components.add(index, Component.literal("Average Coins Remaining + Scrolls: ").withColor(0xfee761)
                .append(Component.literal("%,d".format(averageIncome.toInt() * remaining + averageCoinsFromScrolls(remaining))).withColor(0xFFFFFF))
                .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png")))
        }
    }

    fun handleDailyMeterIncomeTooltip(components: MutableList<Component>) {
        val averageIncome = averageDailyMeterIncome()

        val index = components.indexOfFirst { it.string.contains("Daily Claims:") }
        if (index == -1) return

        components.add(index + 1,
            Component.literal("Average Coins/Claim: ").withColor(0xfee761)
                .append(Component.literal("%,d".format(averageIncome.toInt())).withColor(0xFFFFFF))
                .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png"))
        )

        val claimsMatch = Regex("Daily Claims: (?<completed>\\d+)/(?<total>\\d+)").find(components[index].string)?.groups ?: return
        val completedClaims = claimsMatch["completed"]?.value?.toIntOrNull() ?: return
        val totalClaims = claimsMatch["total"]?.value?.toIntOrNull() ?: return

        components[index] = components[index].copy()
            .append(Component.literal(" [").withColor(ChatFormatting.DARK_GRAY.color!!))
            .append(Component.literal("~" + "%,d".format(averageIncome.toInt() * (totalClaims - completedClaims))).withColor(0xFFFFFF))
            .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png"))
            .append(Component.literal(" remaining").withColor(ChatFormatting.GRAY.color!!))
            .append(Component.literal("]").withColor(ChatFormatting.DARK_GRAY.color!!))
    }

    fun handleWeeklyVaultIncomeTooltip(components: MutableList<Component>) {
        val index = components.indexOfFirst { it.string.contains("Stored Rewards:") }
        if (index == -1) return

        val storedRewardsMatch = Regex("Stored Rewards: (?<claims>\\d+)/(?<max>\\d+)").find(components[index].string)?.groups ?: return
        claims = storedRewardsMatch["claims"]?.value?.toIntOrNull() ?: return
        maxClaims = storedRewardsMatch["max"]?.value?.toIntOrNull() ?: return

        val averageIncomePerClaim = averageWeeklyVaultIncome(claims)
        components.add(index + 1,
            Component.literal("Average Coins: ").withColor(0xfee761)
                .append(Component.literal("%,d".format(averageIncomePerClaim.toInt() * claims)).withColor(0xFFFFFF))
                .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png"))
        )

        if (claims == maxClaims) return
        val averageIncomePerMaxClaim = averageWeeklyVaultIncome(maxClaims)

        components.add(index + 2,
            Component.literal("Average Coins (Maxed): ").withColor(0xfee761)
                .append(Component.literal("%,d".format(averageIncomePerMaxClaim.toInt() * maxClaims)).withColor(0xFFFFFF))
                .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png"))
        )
    }

    fun handleOverallIncomeTooltip(components: MutableList<Component>) {
        val dailyQuests = min(2 + (Galapagos.save.rank?.bonusQuests ?: 0) + (Galapagos.save.stylePerks[StylePerk.EXPANDED_DAILIES] ?: 0), Config.values::averageIncomeDailies.get())
        val weeklyQuests = min(2 + (Galapagos.save.rank?.bonusQuests ?: 0) + (Galapagos.save.stylePerks[StylePerk.EXPANDED_WEEKLIES] ?: 0), Config.values::averageIncomeWeeklies.get())
        val dailyClaims = min(7 + (Galapagos.save.stylePerks[StylePerk.EXPANDED_METER] ?: 0), Config.values::averageIncomeMeters.get())
        val weeklyClaims = min((20 + (Galapagos.save.stylePerks[StylePerk.EXPANDED_VAULT] ?: 0) * 5), Config.values::averageIncomeVaultClaims.get())

        val coinsPerDay = dailyQuests * averageDailyQuestIncome() + dailyClaims * averageDailyMeterIncome() + averageDailyChestIncome(loginStreak)
        val coinsPerWeek = coinsPerDay * 7 + weeklyQuests * averageWeeklyQuestIncome() + weeklyClaims * averageWeeklyVaultIncome(weeklyClaims)

        val coinsPerDayWithScrolls = coinsPerDay + averageCoinsFromScrolls(dailyQuests)
        val coinsPerWeekWithScrolls = coinsPerWeek + averageCoinsFromScrolls(dailyQuests * 7)

        var index = components.indexOfFirst { it.string.contains("earn Reward Crates") }
        if (index == -1) return

        index++
        components.add(index, Component.empty())

        index++
        components.add(index, Component.literal("Average Coins/Day: ").withColor(0xfee761)
            .append(Component.literal("%,d".format(coinsPerDay.toInt())).withColor(0xFFFFFF))
            .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png")))

        if (Config.values::averageIncomeIncludeQuestScrolls.get()) {
            index++
            components.add(index, Component.literal("Average Coins/Day + Scrolls: ").withColor(0xfee761)
                .append(Component.literal("%,d".format(coinsPerDayWithScrolls.toInt())).withColor(0xFFFFFF))
                .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png")))
        }

        index++
        components.add(index, Component.literal("Average Coins/Week: ").withColor(0xfee761)
            .append(Component.literal("%,d".format(coinsPerWeek.toInt())).withColor(0xFFFFFF))
            .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png")))

        if (Config.values::averageIncomeIncludeQuestScrolls.get()) {
            index++
            components.add(index, Component.literal("Average Coins/Week + Scrolls: ").withColor(0xfee761)
                .append(Component.literal("%,d".format(coinsPerWeekWithScrolls.toInt())).withColor(0xFFFFFF))
                .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png")))
        }
    }

    fun handleQuestScrollTooltip(components: MutableList<Component>) {
        components.add(1, Component.empty())
        components.add(2, Component.literal("Average Coins from All Scrolls: ").withColor(0xfee761)
            .append(Component.literal("%,d".format(averageCoinsFromAllScrolls())).withColor(0xFFFFFF))
            .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png"))
        )
    }

    fun handleStartedQuestScrollTooltip(item: ItemStack, components: MutableList<Component>) {
        val index = components.indexOfFirst { it.string == "Rewards:" }
        if (index == -1) return

        val rarity = Rarity.entries.find { item.itemName.string.contains(it.label) } ?: return

        components.add(index + 2, Component.literal("Average Coins: ").withColor(0xfee761)
            .append(Component.literal("%,d".format(CRATE_AVERAGE_COINS[rarity]!!)).withColor(0xFFFFFF))
            .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png"))
        )
    }

    fun handleDailyChestTooltip(components: MutableList<Component>) {
        val index = components.indexOfFirst { it.string.contains("Current Login Streak:") }
        if (index == -1) return

        val income = averageDailyChestIncome(loginStreak).toInt()

        components.add(index + 2, Component.literal("Average Coins: ").withColor(0xfee761)
            .append(Component.literal("%,d".format(income)).withColor(0xFFFFFF))
            .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png"))
        )
    }

    fun containerRender(screen: ContainerScreen, graphics: GuiGraphicsExtractor, x: Int, y: Int, w: Int) {
        if (!openedScrollMenu) return
        if (!screen.title.string.contains("INFINIBAG")) return

        val coins = averageCoinsFromAllScrolls()

        graphics.text(Minecraft.getInstance().font,
            Component.literal("Average Coins from All Scrolls: " + "%,d".format(coins))
                .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png")),
            x + w + 2, y + 130, ARGB.opaque(0xfee761), true
        )
    }
}