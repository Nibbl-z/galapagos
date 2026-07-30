package xyz.nibblz.galapagos.features

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.data.ARCANE_ANOMALY_CHANCES
import xyz.nibblz.galapagos.data.LUCKY_UPGRADE_CHANCES
import xyz.nibblz.galapagos.data.Rarity
import xyz.nibblz.galapagos.data.StylePerk
import xyz.nibblz.galapagos.data.WEEKLY_VAULT_CHANCES
import xyz.nibblz.galapagos.features.WeeklyVaultInfo.claims
import xyz.nibblz.galapagos.features.WeeklyVaultInfo.maxClaims
import xyz.nibblz.galapagos.util.Glyphs
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

    var AVERAGE_COINS_PER_ANOMALY = 0.0

    fun averageDailyQuestIncome(): Double {
        return averageIncome(
            LUCKY_UPGRADE_CHANCES[Galapagos.save.stylePerks[StylePerk.LUCKY_QUESTS]!!],
            1,
            Galapagos.save.stylePerks[StylePerk.BOOSTED_QUESTS]!! * 0.05,
            Galapagos.save.stylePerks[StylePerk.ARCANE_QUESTS]!! * 0.005
        )
    }

    fun averageWeeklyQuestIncome(): Double {
        return averageIncome(
            LUCKY_UPGRADE_CHANCES[Galapagos.save.stylePerks[StylePerk.LUCKY_QUESTS]!!],
            5,
            Galapagos.save.stylePerks[StylePerk.BOOSTED_QUESTS]!! * 0.05,
            Galapagos.save.stylePerks[StylePerk.ARCANE_QUESTS]!! * 0.005
        )
    }

    fun averageDailyMeterIncome(): Double {
        return averageIncome(
            LUCKY_UPGRADE_CHANCES[Galapagos.save.stylePerks[StylePerk.LUCKY_METER]!!],
            if (Galapagos.save.mccPlus) 2 else 1,
            Galapagos.save.stylePerks[StylePerk.GLITCHED_CLAIMS]!! * 0.05,
            Galapagos.save.stylePerks[StylePerk.ARCANE_CLAIMS]!! * 0.005
        )
    }

    fun averageWeeklyVaultIncome(claims: Int): Double {
        val chances = WEEKLY_VAULT_CHANCES.entries.find { (range, _) -> claims in range }?.value
            ?: throw IllegalStateException("Claim $claims does not fall in the valid range of weekly vault claims")

        return averageIncome(
            chances,
            if (Galapagos.save.mccPlus) 2 else 1,
            Galapagos.save.stylePerks[StylePerk.GLITCHED_CLAIMS]!! * 0.05,
            Galapagos.save.stylePerks[StylePerk.ARCANE_CLAIMS]!! * 0.005
        )
    }

    fun averageIncome(chances: HashMap<Rarity, Double>, mult: Int, boostedChance: Double, arcaneChance: Double): Double {
        var totalAverage = 0.0

        chances.forEach { (rarity, chance) ->
            totalAverage += CRATE_AVERAGE_COINS[rarity]!! * chance
        }

        totalAverage *= mult

        var boostedAverage = totalAverage

        boostedAverage += (totalAverage * (boostedChance - (boostedChance * arcaneChance)))
        boostedAverage += (totalAverage * 9 * (boostedChance * arcaneChance))
        boostedAverage += (AVERAGE_COINS_PER_ANOMALY * Galapagos.save.stylePerks[StylePerk.ARCANE_ANOMALY]!! * 0.00005)

        return boostedAverage
    }

    override fun init() {
        ARCANE_ANOMALY_CHANCES.forEach { (rarity, chance) ->
            AVERAGE_COINS_PER_ANOMALY += CRATE_AVERAGE_COINS[rarity]!! * chance
        }
        AVERAGE_COINS_PER_ANOMALY *= 50
        ItemTooltipCallback.EVENT.register { stack, _, _, components -> tooltipAdd(stack, components) }
    }

    fun tooltipAdd(item: ItemStack, components: MutableList<Component>) {
        if (item.itemName.string == "Remaining Daily Quests") handleQuestIncomeTooltip(components, false)
        if (item.itemName.string == "Remaining Weekly Quests") handleQuestIncomeTooltip(components, true)
        if (item.itemName.string == "Daily Meter") handleDailyMeterIncomeTooltip(components)
        if (item.itemName.string == "Weekly Vault") handleWeeklyVaultIncomeTooltip(components)
        if (item.itemName.string == "Island Rewards" && item.get(DataComponents.ITEM_MODEL)?.path?.contains("blank") == true) handleOverallIncomeTooltip(components)
    }

    fun handleQuestIncomeTooltip(components: MutableList<Component>, isWeekly: Boolean) {
        val averageIncome = if (isWeekly) averageWeeklyQuestIncome() else averageDailyQuestIncome()

        val index = components.indexOfFirst { it.string.contains("Remaining ${if (isWeekly) "Weekly" else "Daily"} Quests:") }
        if (index == -1) return

        components.add(index + 1,
            Component.literal("Average Coins/${if (isWeekly) "Weekly" else "Daily"} Quest: ").withColor(ChatFormatting.YELLOW.color!!)
                .append(Component.literal("%,d".format(averageIncome.toInt())).withColor(0xFFFFFF))
                .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png"))
        )

        val remaining = Regex("Remaining ${if (isWeekly) "Weekly" else "Daily"} Quests: (?<quests>\\d+)").find(components[index].string)
            ?.groups?.get("quests")?.value?.toIntOrNull() ?: return

        components[index] = Component.literal("Remaining ${if (isWeekly) "Weekly" else "Daily"} Quests: ").withColor(ChatFormatting.YELLOW.color!!)
            .append(Component.literal("$remaining, ~" + "%,d".format(averageIncome.toInt() * remaining)).withColor(0xFFFFFF))
            .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png"))
    }

    fun handleDailyMeterIncomeTooltip(components: MutableList<Component>) {
        val averageIncome = averageDailyMeterIncome()

        val index = components.indexOfFirst { it.string.contains("Daily Claims:") }
        if (index == -1) return

        components.add(index + 1,
            Component.literal("Average Coins/Claim: ").withColor(ChatFormatting.YELLOW.color!!)
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
            Component.literal("Average Coins: ").withColor(ChatFormatting.YELLOW.color!!)
                .append(Component.literal("%,d".format(averageIncomePerClaim.toInt() * claims)).withColor(0xFFFFFF))
                .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png"))
        )

        if (claims == maxClaims) return
        val averageIncomePerMaxClaim = averageWeeklyVaultIncome(maxClaims)

        components.add(index + 2,
            Component.literal("Average Coins (Maxed): ").withColor(ChatFormatting.YELLOW.color!!)
                .append(Component.literal("%,d".format(averageIncomePerMaxClaim.toInt() * maxClaims)).withColor(0xFFFFFF))
                .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png"))
        )
    }

    fun handleOverallIncomeTooltip(components: MutableList<Component>) {
        val dailyQuests = 2 + (Galapagos.save.rank?.bonusQuests ?: 0) + (Galapagos.save.stylePerks[StylePerk.EXPANDED_DAILIES] ?: 0)
        val weeklyQuests = 2 + (Galapagos.save.rank?.bonusQuests ?: 0) + (Galapagos.save.stylePerks[StylePerk.EXPANDED_WEEKLIES] ?: 0)
        val dailyClaims = 7 + (Galapagos.save.stylePerks[StylePerk.EXPANDED_METER] ?: 0)
        val weeklyClaims = (20 + (Galapagos.save.stylePerks[StylePerk.EXPANDED_VAULT] ?: 0) * 5)

        // todo: with the coins per day (maybe this can be a setting), add an option to also include coins from doing a quest scroll alongside?
        val coinsPerDay = dailyQuests * averageDailyQuestIncome() + dailyClaims * averageDailyMeterIncome()
        val coinsPerWeek = coinsPerDay * 7 + weeklyQuests * averageWeeklyQuestIncome() + weeklyClaims * averageWeeklyVaultIncome(weeklyClaims)

        // todo 2: in settings, add options to choose how of everything you think you'll do on average

        val index = components.indexOfFirst { it.string.contains("earn Reward Crates") }
        if (index == -1) return

        components.add(index + 1, Component.empty())
        components.add(index + 2, Component.literal("Average Coins/Day: ").withColor(ChatFormatting.YELLOW.color!!)
            .append(Component.literal("%,d".format(coinsPerDay.toInt())).withColor(0xFFFFFF))
            .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png")))
        components.add(index + 3, Component.literal("Average Coins/Week: ").withColor(ChatFormatting.YELLOW.color!!)
            .append(Component.literal("%,d".format(coinsPerWeek.toInt())).withColor(0xFFFFFF))
            .append(Glyphs.getGlyphComponent("_fonts/icon/coin_small.png")))
    }
}