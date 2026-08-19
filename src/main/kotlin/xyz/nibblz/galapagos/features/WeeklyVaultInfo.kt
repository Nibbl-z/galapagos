package xyz.nibblz.galapagos.features

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.world.item.ItemStack
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.events.ContainerOpenEvent
import xyz.nibblz.galapagos.util.findLore
import xyz.nibblz.galapagos.util.mcciProgressBar

object WeeklyVaultInfo : Feature {
    override val id: String = "weekly_vault_info"
    override val name: String = "Weekly Vault Info"
    override val description: List<Component> = listOf(
        Component.literal("Shows a progress bar on the weekly vault's tooltip showing the overall progress towards a max vault, as well as how much XP you'll need on average per day to max your vault."),
    )
    override val image: Config.ConfigImage = Config.ConfigImage("weekly_vault_info.png", 470, 341)

    val xpPerLevel: HashMap<IntRange, Int> = hashMapOf(
        1..5 to 500,
        6..10 to 1000,
        11..15 to 1500,
        16..20 to 2000,
        21..25 to 3000,
        26..30 to 4000,
        31..35 to 5000,
        36..40 to 6000,
        41..45 to 7000,
        46..50 to 8000,
        51..55 to 10000,
        56..60 to 12000
    )

    var claims = 0
    var maxClaims = 20
    var progress = 0
    var daysLeft = 7

    val storedRewardsRegex = Regex("Stored Rewards: (?<claims>\\d+)/(?<max>\\d+)")
    val progressRegex = Regex("Progress: (?<xp>[\\d,]+)")
    val claimableInRegex = Regex("Claimable in: (?<days>\\d)d")

    fun claimXP(claim: Int): Int {
        // note for the future:
        // this'll probably get patched eventually but apparently last 5 vault levels always act as claims 56-60
        // idk if this is implemented correctly but thisll have to be removed when that gets fixed!!
        if (claim in (maxClaims - 4)..maxClaims && Galapagos.save.cosmetics["Abomination Robe"]?.isOwned == true) {
            return 12000
        }

        xpPerLevel.forEach { (range, xp) ->
            if (claim in range) return xp
        }

        Galapagos.logger.warn("Claim $claim does not fall in the valid range of weekly vault claims")
        return 0
    }

    fun getTotalXP(): Int {
        var xp = progress

        repeat(claims) {
            xp += claimXP(it + 1)
        }

        return xp.coerceIn(0..getMaxXPNeeded())
    }

    fun getMaxXPNeeded(): Int {
        var xp = 0

        repeat(maxClaims) {
            xp += claimXP(it + 1)
        }

        return xp
    }

    fun getXpPerDay(): Int {
        return (getMaxXPNeeded() - getTotalXP()) / daysLeft
    }

    override fun init() {
        ContainerOpenEvent.EVENT.register { packet -> containerOpen(packet) }
        ItemTooltipCallback.EVENT.register { stack, _, _, components -> tooltipAdd(stack, components) }
    }

    fun containerOpen(packet: ClientboundContainerSetContentPacket) {
        val screen = Minecraft.getInstance().screen ?: return
        if (!screen.title.string.contains("ISLAND REWARDS")) return

        val vault = packet.items[16]
        if (vault.itemName.string != "Weekly Vault") return

        val storedRewardsMatch = vault.findLore(storedRewardsRegex) ?: return
        claims = storedRewardsMatch["claims"]?.value?.toIntOrNull() ?: return
        maxClaims = storedRewardsMatch["max"]?.value?.toIntOrNull() ?: return

        val progressMatch = vault.findLore(progressRegex) ?: return
        progress = progressMatch["xp"]?.value?.replace(",", "")?.toIntOrNull() ?: return

        val daysMatch = vault.findLore(claimableInRegex)
        daysLeft = daysMatch?.get("days")?.value?.toIntOrNull() ?: 1
    }

    fun tooltipAdd(stack: ItemStack, components: MutableList<Component>) {
        if (!enabled) return
        val screen = Minecraft.getInstance().screen ?: return
        if (!screen.title.string.contains("ISLAND REWARDS")) return

        if (stack.itemName.string != "Weekly Vault") return

        var progressIndex = components.indexOfFirst { it.string.contains("Progress: ") }
        if (progressIndex == -1) return

        progressIndex++

        if (Config.values::weeklyVaultInfoShowTotalProgress.get()) {
            components.add(progressIndex,
                mcciProgressBar(getTotalXP().toDouble() / getMaxXPNeeded().toDouble(), 10)
                    .append(Component.literal(" ${(getTotalXP().toDouble() / getMaxXPNeeded().toDouble() * 100.0).toInt()}%"))
            )

            progressIndex++

            components.add(progressIndex,
                Component.literal("Overall Progress: ").withColor(ChatFormatting.GRAY.color!!)
                    .append(Component.literal("%,d".format(getTotalXP())).withColor(ChatFormatting.WHITE.color!!))
                    .append(Component.literal("/" + "%,d".format(getMaxXPNeeded())).withColor(ChatFormatting.DARK_GRAY.color!!))
                    .append(Component.literal(" XP").withColor(ChatFormatting.GRAY.color!!))
            )

            progressIndex++
        }

        if (Config.values::weeklyVaultInfoShowNeededXPPerDay.get()) {
            components.add(progressIndex,
                Component.literal("~" + "%,d".format(getXpPerDay())).withColor(ChatFormatting.WHITE.color!!)
                    .append(Component.literal(" XP needed per day").withColor(ChatFormatting.GRAY.color!!))
            )
        }
    }
}