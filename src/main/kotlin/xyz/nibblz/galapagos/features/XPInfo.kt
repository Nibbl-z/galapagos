package xyz.nibblz.galapagos.features

import com.noxcrew.noxesium.core.mcc.ClientboundMccServerPacket
import com.noxcrew.noxesium.core.mcc.ClientboundMccStatisticPacket
import com.noxcrew.sheeplib.DialogContainer
import kotlinx.serialization.Serializable
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.world.item.ItemStack
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.data.Rank
import xyz.nibblz.galapagos.data.StarLevelGame
import xyz.nibblz.galapagos.dialogs.XPInfoDialog
import xyz.nibblz.galapagos.events.ContainerOpenEvent
import xyz.nibblz.galapagos.events.ContainerSetSlotEvent
import xyz.nibblz.galapagos.events.MCCServerEvent
import xyz.nibblz.galapagos.events.MCCStatisticEvent
import xyz.nibblz.galapagos.util.findLore
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty0
import kotlin.time.Clock

object XPInfo : Feature {
    override val id: String = "xp_info"
    override val name: String = "XP Info"
    override val description: List<Component> = listOf()
    override val enabledProperty: KMutableProperty0<Boolean> = Config.values::xpInfoEnabled
    override val image: Config.ConfigImage = Config.ConfigImage("weekly_vault_info.png", 470, 341)

    @Serializable
    enum class XPSource(val statistic: String, val serverTypes: List<String>, val lobbyServerType: String, val sprite: String, val label: String, val starLevelGame: StarLevelGame?) {
        BATTLE_BOX_QUADS("battle_box_quads_xp_earned", listOf("battle_box"), "battle_box", "island_interface/game/battle_box/icon", "Battle Box", StarLevelGame.BATTLE_BOX),
        BATTLE_BOX_ARENA("battle_box_arena_xp_earned", listOf("battle_box"), "battle_box", "island_interface/game/battle_box_arena/icon", "Battle Box Arena", StarLevelGame.BATTLE_BOX),
        SKY_BATTLE_QUADS("sky_battle_quads_xp_earned", listOf("sky_battle"), "sky_battle", "island_interface/game/sky_battle/icon", "Sky Battle", StarLevelGame.SKY_BATTLE),
        SKY_BATTLE_SOLOS("sky_battle_solos_xp_earned", listOf("sky_battle", "solo"), "sky_battle", "island_interface/game/sky_battle_solo/icon", "Sky Battle Solo", StarLevelGame.SKY_BATTLE),
        DYNABALL("dynaball_xp_earned", listOf("dynaball"), "dynaball", "island_interface/game/dynaball/icon", "Dynaball", StarLevelGame.DYNABALL),
        TGTTOS("tgttos_xp_earned", listOf("tgttos"), "tgttos", "island_interface/game/tgttosawaf/icon", "To Get To The Other Side", StarLevelGame.TGTTOS), // awaf? but there is no fans.. only chicken..
        HOLE_IN_THE_WALL("hole_in_the_wall_xp_earned", listOf("hole_in_the_wall"), "hole_in_the_wall", "island_interface/game/hole_in_the_wall/icon", "Hole in the Wall", StarLevelGame.HOLE_IN_THE_WALL),
        PW_SURVIVAL("pw_survival_xp_earned", listOf("parkour_warrior", "survival"), "parkour_warrior", "island_interface/game/parkour_warrior/icon", "Parkour Warrior Survivor", StarLevelGame.PARKOUR_WARRIOR),
        PW_SOLO("pw_solo_xp_earned", listOf("parkour_warrior", "dojo"), "parkour_warrior", "island_interface/game/parkour_warrior/solo/icon", "Parkour Warrior Dojo", StarLevelGame.PARKOUR_WARRIOR),
        ROCKET_SPLEEF("rocket_spleef_xp_earned", listOf("rocket_spleef"), "rocket_spleef", "island_interface/game/rocket_spleef/icon", "Rocket Spleef Rush", StarLevelGame.ROCKET_SPLEEF),
        FISHING("fishing_xp_earned", listOf("fishing"), "fishing", "island_interface/fishing/perk_icon/speedy_rod", "Fishing", null),
    }

    @Serializable
    data class XPGain(
        val amount: Int,
        val timestamp: Long,
        val source: XPSource
    )

    data class Claimable(
        var completed: Int,
        val total: Int,
        var currentXP: Int,
        var requiredXP: Int
    )

    var dialog: XPInfoDialog? = null
    var currentGames: MutableList<XPSource> = mutableListOf()
    var currentStarLevelGame: StarLevelGame? = null
    var dailyMeter: Claimable = Claimable(0, 7, 0, 500)
    var weeklyVault: Claimable = Claimable(0, 20, 0, 500)

    override fun init() {
        MCCStatisticEvent.EVENT.register { packet -> mccStatistic(packet) }
        MCCServerEvent.EVENT.register { packet -> mccServer(packet) }
        ContainerOpenEvent.EVENT.register { packet -> containerOpen(packet) }
        ItemTooltipCallback.EVENT.register { item, _, _, components -> tooltipAdd(item, components) }
        ContainerSetSlotEvent.EVENT.register { packet -> containerSetSlot(packet) }
        //SlotClickEvent.EVENT.register { slot, screen, _, _, _ -> slotClick(slot, screen) }
        //ScoreboardTitleUpdateEvent.EVENT.register { scoreboardTitleChange() }
    }

//    fun scoreboardTitleChange() {
//        if (dialog == null || dialog?.state == Dialog.State.CLOSED) {
//            dialog = XPInfoDialog(10, 10)
//            DialogContainer += dialog!!
//        }
//    }

    fun mccStatistic(packet: ClientboundMccStatisticPacket) {
        val source = XPSource.entries.find { packet.statistic == it.statistic }
        if (source == null) return

        val bonus = 1.0 +
                (if (Galapagos.save.mccPlus) 0.3 else 0.0) +
                (if (Galapagos.save.rank == Rank.GRAND_CHAMP_SUPREME) 0.2 else 0.0)
        // todo: track party state to see if a gcs is in your party

        // fishing xp stat returns the boosted amount, games don't, soo...
        val amount = if (source == XPSource.FISHING) packet.value else (packet.value.toDouble() * bonus).roundToInt()

        val gain = XPGain(amount = amount, source = source, timestamp = Clock.System.now().epochSeconds)
        Galapagos.save.xpGains.add(gain)

        dailyMeter.currentXP = (dailyMeter.currentXP + amount).coerceIn(0..dailyMeter.requiredXP)
        weeklyVault.currentXP = (weeklyVault.currentXP + amount)
        if (weeklyVault.currentXP >= weeklyVault.requiredXP) {
            weeklyVault.completed++
            if (weeklyVault.completed != weeklyVault.total) {
                weeklyVault.currentXP -= weeklyVault.requiredXP
                weeklyVault.requiredXP = WeeklyVaultInfo.claimXP(weeklyVault.completed)
            } else {
                weeklyVault.currentXP = weeklyVault.requiredXP
            }
        }

        if (source.starLevelGame != null) {
            Galapagos.save.gameXP[source.starLevelGame] = Galapagos.save.gameXP[source.starLevelGame]!! + packet.value
        }

        if (Galapagos.save.selectedFaction != null) {
            Galapagos.save.factionXP[Galapagos.save.selectedFaction!!] = Galapagos.save.factionXP[Galapagos.save.selectedFaction]!! + amount
        }

        dialog?.refresh()
    }

    fun mccServer(packet: ClientboundMccServerPacket) {
        if (dialog == null || dialog?.state?.isClosing == true) {
            dialog = XPInfoDialog(10, 10)
            DialogContainer += dialog!!
        }

        currentGames.clear()
        currentStarLevelGame = null

        if (packet.server == "lobby") {
            XPSource.entries.forEach { if (packet.types.contains(it.lobbyServerType)) currentGames.add(it) }
            currentStarLevelGame = StarLevelGame.entries.find { packet.types.contains(it.name.lowercase()) }
        } else {
            XPSource.entries.forEach {
                if (it.serverTypes.all { type -> packet.types.contains(type) }) currentGames.add(it)
            }
        }

        dialog?.refresh()
    }

    val meterClaimsRegex = Regex("Daily Claims: (?<completed>\\d+)/(?<total>\\d+)")
    val vaultRewardsRegex = Regex("Stored Rewards: (?<claims>\\d+)/(?<max>\\d+)")
    val xpProgressRegex = Regex("Progress: (?<completed>[\\d,]+)/(?<total>[\\d,]+)")

    fun updateDailyMeter(item: ItemStack) {
        val meterClaimsMatch = item.findLore(meterClaimsRegex) ?: return
        val completedMeterClaims = meterClaimsMatch["completed"]?.value?.toIntOrNull() ?: return
        val totalMeterClaims = meterClaimsMatch["total"]?.value?.toIntOrNull() ?: return

        val meterProgressMatch = item.findLore(xpProgressRegex) ?: return
        val meterProgressCompleted = meterProgressMatch["completed"]?.value?.replace(",", "")?.toIntOrNull() ?: return
        val meterProgressTotal = meterProgressMatch["total"]?.value?.replace(",", "")?.toIntOrNull() ?: return

        dailyMeter = Claimable(
            completedMeterClaims,
            totalMeterClaims,
            meterProgressCompleted,
            meterProgressTotal
        )
    }

    fun updateWeeklyVault(item: ItemStack) {
        val vaultRewardsMatch = item.findLore(vaultRewardsRegex) ?: return
        val completedVaultRewards = vaultRewardsMatch["claims"]?.value?.toIntOrNull() ?: return
        val totalVaultRewards = vaultRewardsMatch["max"]?.value?.toIntOrNull() ?: return

        val vaultProgressMatch = item.findLore(xpProgressRegex) ?: return
        val vaultProgressCompleted = vaultProgressMatch["completed"]?.value?.replace(",", "")?.toIntOrNull() ?: return
        val vaultProgressTotal = vaultProgressMatch["total"]?.value?.replace(",", "")?.toIntOrNull() ?: return

        weeklyVault = Claimable(
            completedVaultRewards,
            totalVaultRewards,
            vaultProgressCompleted,
            vaultProgressTotal
        )
    }

    fun containerOpen(packet: ClientboundContainerSetContentPacket) {
        val screen = Minecraft.getInstance().screen ?: return
        if (!screen.title.string.contains("ISLAND REWARDS")) return

        val dailyMeterItem = packet.items[13]
        val weeklyVaultItem = packet.items[16]

        updateDailyMeter(dailyMeterItem)
        updateWeeklyVault(weeklyVaultItem)

        dialog?.refresh()
    }

    fun tooltipAdd(item: ItemStack, components: MutableList<Component>) {
        val screen = Minecraft.getInstance().screen ?: return
        if (!screen.title.string.contains("NAVIGATOR")) return

        val game = XPSource.entries.find { it.label == item.itemName.string } ?: return
        var index = components.indexOfFirst { it.string.contains("Click to") }
        if (index == -1) return

        components.add(index,
            Component.literal("Today's XP: ").withColor(ChatFormatting.AQUA.color!!)
                .append(Component.literal("%,d".format(dialog?.todayXP[game])).withColor(0xFFFFFF))
        )
        index++

        if (game != XPSource.PW_SOLO && game != XPSource.FISHING && dialog?.todayXPEntries[game]!! != 0) { // doesn't make sense to have this line for these games
            components.add(index,
                Component.literal("Average XP/Game: ").withColor(ChatFormatting.AQUA.color!!)
                    .append(Component.literal("%,d".format(dialog?.todayXP[game]!! / dialog?.todayXPEntries[game]!!)).withColor(0xFFFFFF))
            )
            index++
        }

        components.add(index, Component.empty())
    }

    fun containerSetSlot(packet: ClientboundContainerSetSlotPacket) {
        val screen = Minecraft.getInstance().screen ?: return

        if (packet.item.itemName.string == "Daily Meter" && screen.title.string.contains("ISLAND REWARDS")) updateDailyMeter(packet.item)
        if (packet.item.itemName.string == "Weekly Vault" && screen.title.string.contains("ISLAND REWARDS")) updateWeeklyVault(packet.item)
    }
}