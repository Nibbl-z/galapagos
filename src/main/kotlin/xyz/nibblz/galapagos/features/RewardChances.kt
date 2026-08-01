package xyz.nibblz.galapagos.features

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.data.Rarity
import xyz.nibblz.galapagos.data.StylePerk
import xyz.nibblz.galapagos.util.mcciTextureComponent
import kotlin.reflect.KMutableProperty0

object RewardChances : Feature {
    override val id: String = "reward_chances"
    override val name: String = "Reward Chances"
    override val description: List<Component> = listOf(
        Component.literal("Displays the chances for x1, x2 (boosted/glitched), and x10 (arcane) rewards seperately in the reward chance for each rarity in the tooltips of Daily Quests, Weekly Quests, Daily Meter, and Weekly Vault."),
        Component.empty(),
        Component.literal("Your chance for an Arcane Anomaly will also appear if you have the perk upgraded.")
    )
    override val enabledProperty: KMutableProperty0<Boolean> = Config.values::rewardChancesEnabled
    override val image: Config.ConfigImage = Config.ConfigImage("reward_chances.png", 618, 472)

    val rarityChanceRegex = Regex("] - (?<chance>\\d+)%")

    override fun init() {
        ItemTooltipCallback.EVENT.register { _, _, _, components -> tooltipAdd(components) }
    }

    fun tooltipAdd(components: MutableList<Component>) {
        if (!enabledProperty.get()) return

        if (components.any {it.string.contains("Reward Chances:")}) editRewardChancesTooltip(
            components,
            (Galapagos.save.stylePerks[StylePerk.GLITCHED_CLAIMS] ?: 0) * 0.05,
            (Galapagos.save.stylePerks[StylePerk.ARCANE_CLAIMS] ?: 0) * 0.005,
            true
        )

        if (components.any {it.string.contains("Quest Rarity Chances:")}) editRewardChancesTooltip(
            components,
            (Galapagos.save.stylePerks[StylePerk.BOOSTED_QUESTS] ?: 0) * 0.05,
            (Galapagos.save.stylePerks[StylePerk.ARCANE_QUESTS] ?: 0) * 0.005,
            false
        )
    }

    fun editRewardChancesTooltip(components: MutableList<Component>, boosted: Double, arcane: Double, isClaim: Boolean) {
        var lastRarityIndex = -1

        components.forEachIndexed { index, component ->
            var chance = rarityChanceRegex.find(component.string)?.groups?.get("chance")?.value?.toIntOrNull()?.toDouble() ?: return@forEachIndexed
            chance /= 100.0

            val rarity = Rarity.entries.find { component.string.contains(it.label) } ?: return

            if (lastRarityIndex < index) lastRarityIndex = index

            val boostedChance = chance * (boosted - arcane) * 100.0
            val arcaneChance = chance * arcane * 100.0
            val unboostedChance = chance * 100.0 - boostedChance - arcaneChance

            var newComponent = Component.empty()
                .append(Component.literal(" • ").withColor(ChatFormatting.DARK_GRAY.color!!))
                .append(Component.literal("[${rarity.label}]").withColor(rarity.color))
                .append(Component.literal(" - ").withColor(ChatFormatting.DARK_GRAY.color!!))
                .append(Component.literal("${Galapagos.decimalFormat.format(unboostedChance)}%"))

            if (boostedChance > 0) {
                newComponent = newComponent
                    .append(Component.literal(", "))
                    .append(mcciTextureComponent(
                        if (isClaim) StylePerk.GLITCHED_CLAIMS.sprite.dropLast(4)
                        else StylePerk.BOOSTED_QUESTS.sprite.dropLast(4))
                    )
                    .append(Component.literal(" ${Galapagos.decimalFormat.format(boostedChance)}%"))
            }

            if (arcaneChance > 0) {
                newComponent = newComponent
                    .append(Component.literal(", "))
                    .append(mcciTextureComponent(
                        if (isClaim) StylePerk.ARCANE_CLAIMS.sprite.dropLast(4)
                        else StylePerk.ARCANE_QUESTS.sprite.dropLast(4))
                    )
                    .append(Component.literal(" ${Galapagos.decimalFormat.format(arcaneChance)}%"))
            }

            components[index] = newComponent
        }

        if ((Galapagos.save.stylePerks[StylePerk.ARCANE_ANOMALY] ?: 0) > 0 && lastRarityIndex != -1) {
            components.add(lastRarityIndex + 1, Component.empty()
                .append(Component.literal(" • ").withColor(ChatFormatting.DARK_GRAY.color!!))
                .append(Component.literal("[Arcane Anomaly]").withColor(Rarity.MYTHIC.color))
                .append(Component.literal(" - ").withColor(ChatFormatting.DARK_GRAY.color!!))
                .append(Component.literal("${Galapagos.save.stylePerks[StylePerk.ARCANE_ANOMALY]!! * 0.005}%"))
            )
        }
    }
}