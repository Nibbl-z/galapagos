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
import kotlin.reflect.KMutableProperty0

object WeeklyVaultInfo : Feature {
    override val id: String = "weekly_vault_info"
    override val name: String = "Weekly Vault Info"
    override val description: List<Component> = listOf()
    override val enabledProperty: KMutableProperty0<Boolean> = Config.values::weeklyVaultInfoEnabled
    override val image: Config.ConfigImage = Config.ConfigImage("quest_tracking.png", 1097, 465)

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

    fun claimXP(claim: Int): Int {
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

        return xp
    }

    fun getMaxXPNeeded(): Int {
        var xp = 0

        repeat(maxClaims) {
            xp += claimXP(it + 1)
        }

        return xp
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

        val storedRewardsMatch = vault.findLore(Regex("Stored Rewards: (?<claims>\\d+)/(?<max>\\d+)")) ?: return
        claims = storedRewardsMatch["claims"]?.value?.toIntOrNull() ?: return
        maxClaims = storedRewardsMatch["max"]?.value?.toIntOrNull() ?: return

        val progressMatch = vault.findLore(Regex("Progress: (?<xp>[\\d,]+)")) ?: return
        progress = progressMatch["xp"]?.value?.replace(",", "")?.toIntOrNull() ?: return

        Galapagos.logger.info("$claims + $progress XP / $maxClaims ")
        Galapagos.logger.info("${getTotalXP()} / ${getMaxXPNeeded()}")
    }



    fun tooltipAdd(stack: ItemStack, components: MutableList<Component>) {
        val screen = Minecraft.getInstance().screen ?: return
        if (!screen.title.string.contains("ISLAND REWARDS")) return

        if (stack.itemName.string != "Weekly Vault") return

        var progressIndex = components.indexOfFirst { it.string.contains("Progress: ") }
        if (progressIndex == -1) progressIndex = 13 else progressIndex++

        components.add(progressIndex,
            mcciProgressBar(getTotalXP().toDouble() / getMaxXPNeeded().toDouble(), 10)
                .append(Component.literal(" ${(getTotalXP().toDouble() / getMaxXPNeeded().toDouble() * 100.0).toInt()}%"))
        )

        components.add(progressIndex + 1,
            Component.literal("Overall Progress: ").withColor(ChatFormatting.GRAY.color!!)
                .append(Component.literal("%,d".format(getTotalXP())).withColor(ChatFormatting.WHITE.color!!))
                .append(Component.literal("/" + "%,d".format(getMaxXPNeeded())).withColor(ChatFormatting.DARK_GRAY.color!!))
                .append(Component.literal(" XP").withColor(ChatFormatting.GRAY.color!!))
        )
    }
}