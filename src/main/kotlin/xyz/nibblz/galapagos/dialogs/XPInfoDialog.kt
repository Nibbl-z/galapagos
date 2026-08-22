package xyz.nibblz.galapagos.dialogs

import com.noxcrew.sheeplib.dialog.Dialog
import com.noxcrew.sheeplib.dialog.title.TextTitleWidget
import com.noxcrew.sheeplib.layout.linear
import com.noxcrew.sheeplib.theme.Themed
import com.noxcrew.sheeplib.widget.TextWidgets
import com.noxcrew.sheeplib.widget.ThemedButton
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.network.chat.Component
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.data.FACTION_XP_PER_LEVEL
import xyz.nibblz.galapagos.data.getFactionLevelAndProgress
import xyz.nibblz.galapagos.data.getStarLevelEvolution
import xyz.nibblz.galapagos.features.XPInfo
import xyz.nibblz.galapagos.util.Glyphs
import xyz.nibblz.galapagos.util.mcciProgressBar
import xyz.nibblz.galapagos.util.mcciTextureComponent
import java.util.*
import kotlin.time.Clock
import kotlin.time.Instant

class XPInfoDialog(x: Int, y: Int) : Dialog(x, y), Themed by GalapagosTheme {
    var showBreakdown = false
    val todayXP = XPInfo.XPSource.entries.associateWithTo(EnumMap(XPInfo.XPSource::class.java)) { 0 }
    val todayXPEntries = XPInfo.XPSource.entries.associateWithTo(EnumMap(XPInfo.XPSource::class.java)) { 0 }

    fun shouldDisplay(displayType: Config.XPInfoDisplay): Boolean {
        return when(displayType) {
            Config.XPInfoDisplay.DISABLED -> false
            Config.XPInfoDisplay.ENABLED -> true
            Config.XPInfoDisplay.ENABLED_LOBBY -> XPInfo.inLobby
            Config.XPInfoDisplay.ENABLED_GAMES -> !XPInfo.inLobby
        }
    }

    override fun layout() = linear(LinearLayout.Orientation.VERTICAL) {
        val font = Minecraft.getInstance().font

        val today = if (Config.values::startDayAtQuestRefresh.get())
            Instant.fromEpochSeconds(Clock.System.now().epochSeconds - (60 * 60 * 10)).toLocalDateTime(TimeZone.UTC)
        else Instant.fromEpochSeconds(Clock.System.now().epochSeconds).toLocalDateTime(TimeZone.currentSystemDefault())

        Galapagos.logger.info("${today.day}")
        todayXP.replaceAll { _, _ -> 0 }
        todayXPEntries.replaceAll { _, _ -> 0 }

        Galapagos.save.xpGains.forEach {
            val date = if (Config.values::startDayAtQuestRefresh.get())
                Instant.fromEpochSeconds(it.timestamp - (60 * 60 * 10)).toLocalDateTime(TimeZone.UTC)
            else Instant.fromEpochSeconds(it.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())

            if (date.day != today.day || date.month != today.month || date.year != today.year) return@forEach

            todayXP[it.source] = todayXP[it.source]!! + it.amount
            todayXPEntries[it.source] = todayXPEntries[it.source]!! + 1
        }

        val totalXPToday = todayXP.entries.sumOf { it.value }

        if (shouldDisplay(Config.values::xpInfoTodaysXP.get())) {
            +StringWidget(Component.literal("Today's XP: ${"%,d".format(totalXPToday)}"), font)
        }

        if (shouldDisplay(Config.values::xpInfoGameXP.get())) {
            if (!showBreakdown) {
                XPInfo.currentGames.forEach {
                    +StringWidget(Component.empty()
                        .append(mcciTextureComponent(it.sprite))
                        .append(Component.literal(" ${it.label} XP: ${"%,d".format(todayXP[it])}")), font)
                }
            } else {
                var component = Component.empty()
                val games = todayXP.toList().sortedByDescending { it.second }.filter { it.second != 0 }
                games.forEachIndexed { index, (game, xp) ->
                    component = component.append(
                        Component.empty()
                            .append(mcciTextureComponent(game.sprite))
                            .append(Component.literal(" ${game.label} XP: ${"%,d".format(xp)} "))
                            .append(Component.literal("(${(xp.toDouble() / totalXPToday.toDouble() * 100.0).toInt()}%)${if (index != games.size - 1) "\n" else ""}").withColor(ChatFormatting.GRAY.color!!))
                    )
                }

                +TextWidgets.multiLine(component)
            }
        }

        val meterProgress = XPInfo.dailyMeter.currentXP.toDouble() / XPInfo.dailyMeter.requiredXP.toDouble()
        val meterSprite = when {
            meterProgress < 0.25 -> "island_interface/quest_log/daily/daily_meter_0"
            meterProgress < 0.5 -> "island_interface/quest_log/daily/daily_meter_1"
            meterProgress < 0.75 -> "island_interface/quest_log/daily/daily_meter_2"
            meterProgress < 1.0 -> "island_interface/quest_log/daily/daily_meter_3"
            meterProgress == 1.0 && XPInfo.dailyMeter.completed != XPInfo.dailyMeter.total -> "island_interface/quest_log/quest_glow"
            XPInfo.dailyMeter.completed == XPInfo.dailyMeter.total -> "island_interface/quest_log/quest_complete"
            else -> "island_interface/quest_log/daily/daily_meter_0"
        }

        if (shouldDisplay(Config.values::xpInfoDailyMeter.get()) && !(Config.values::xpInfoDisableDailyMeterIfMax.get() && meterSprite == "island_interface/quest_log/quest_complete")) {
            +TextWidgets.multiLine(mcciTextureComponent(meterSprite)
                .append(Component.literal(" Daily Meter: ").withColor(ChatFormatting.GRAY.color!!))
                .append(Component.literal("${XPInfo.dailyMeter.completed}").withColor(ChatFormatting.WHITE.color!!))
                .append(Component.literal("/${XPInfo.dailyMeter.total}, ").withColor(ChatFormatting.DARK_GRAY.color!!))
                .append(Component.literal("%,d".format(XPInfo.dailyMeter.currentXP)).withColor(ChatFormatting.WHITE.color!!))
                .append(Component.literal("/" + "%,d".format(XPInfo.dailyMeter.requiredXP)).withColor(ChatFormatting.DARK_GRAY.color!!))
                .append(Component.literal(" XP\n").withColor(ChatFormatting.GRAY.color!!))
                .append(mcciProgressBar(meterProgress, 5))
                .append(Component.literal(" ${((XPInfo.dailyMeter.currentXP.toDouble() / XPInfo.dailyMeter.requiredXP.toDouble() * 100.0).toInt())}%"))
            )
        }


        val vaultSprite = when (XPInfo.weeklyVault.completed) {
            0 -> "island_interface/quest_log/meters/daily_vault_empty"
            XPInfo.weeklyVault.total -> "island_interface/quest_log/meters/daily_vault_full"
            else -> "island_interface/quest_log/meters/daily_vault_partly_full"
        }

        if (shouldDisplay(Config.values::xpInfoWeeklyVault.get()) && !(Config.values::xpInfoDisableWeeklyVaultIfMax.get() && vaultSprite == "island_interface/quest_log/meters/daily_vault_full")) {
            +TextWidgets.multiLine(
                mcciTextureComponent(vaultSprite)
                    .append(Component.literal(" Weekly Vault: ").withColor(ChatFormatting.GRAY.color!!))
                    .append(Component.literal("${XPInfo.weeklyVault.completed}").withColor(ChatFormatting.WHITE.color!!))
                    .append(Component.literal("/${XPInfo.weeklyVault.total}, ").withColor(ChatFormatting.DARK_GRAY.color!!))
                    .append(Component.literal("%,d".format(XPInfo.weeklyVault.currentXP)).withColor(ChatFormatting.WHITE.color!!))
                    .append(Component.literal("/" + "%,d".format(XPInfo.weeklyVault.requiredXP)).withColor(ChatFormatting.DARK_GRAY.color!!))
                    .append(Component.literal(" XP\n").withColor(ChatFormatting.GRAY.color!!))
                    .append(mcciProgressBar(XPInfo.weeklyVault.currentXP.toDouble() / XPInfo.weeklyVault.requiredXP.toDouble(), 5))
                    .append(Component.literal(" ${((XPInfo.weeklyVault.currentXP.toDouble() / XPInfo.weeklyVault.requiredXP.toDouble() * 100.0).toInt())}%"))
            )
        }

        if ((XPInfo.seaMonstersActive && shouldDisplay(Config.values::xpInfoSeaMonstersEnergyMeter.get()))
            && !(
                Config.values::xpInfoDisableSeaMonstersEnergyMeterIfMax.get()
                && XPInfo.seaMonstersEnergyMeter.completed == XPInfo.seaMonstersEnergyMeter.total
            )
        ) {
            val energyMeterProgress = XPInfo.seaMonstersEnergyMeter.currentXP.toDouble() / XPInfo.seaMonstersEnergyMeter.requiredXP.toDouble()

            +TextWidgets.multiLine(Component.empty()
                .append(mcciTextureComponent("island_interface/navigator/sea_monster_event"))
                .append(Component.literal(" Energy Meter: ").withColor(ChatFormatting.GRAY.color!!))
                .append(Component.literal("${XPInfo.seaMonstersEnergyMeter.completed}").withColor(ChatFormatting.WHITE.color!!))
                .append(Component.literal("/${XPInfo.seaMonstersEnergyMeter.total}, ").withColor(ChatFormatting.DARK_GRAY.color!!))
                .append(Component.literal("%,d".format(XPInfo.seaMonstersEnergyMeter.currentXP)).withColor(ChatFormatting.WHITE.color!!))
                .append(Component.literal("/" + "%,d".format(XPInfo.seaMonstersEnergyMeter.requiredXP)).withColor(ChatFormatting.DARK_GRAY.color!!))
                .append(Component.literal(" XP\n").withColor(ChatFormatting.GRAY.color!!))
                .append(mcciProgressBar(energyMeterProgress, 5))
                .append(Component.literal(" ${((XPInfo.seaMonstersEnergyMeter.currentXP.toDouble() / XPInfo.seaMonstersEnergyMeter.requiredXP.toDouble() * 100.0).toInt())}%"))
            )
        }

        if (XPInfo.currentStarLevelGame != null && shouldDisplay(Config.values::xpInfoStarLevel.get())) {
            val starLevel = Galapagos.save.starLevelXP[XPInfo.currentStarLevelGame]!! / 3000
            val currentXP = Galapagos.save.starLevelXP[XPInfo.currentStarLevelGame]!! - (starLevel * 3000)

            +TextWidgets.multiLine(
                mcciTextureComponent(XPInfo.currentStarLevelGame!!.sprite)
                    .append(Component.literal(" ${XPInfo.currentStarLevelGame!!.label}: ").withColor(ChatFormatting.GRAY.color!!))
                    .append(Glyphs.getGlyphComponent(getStarLevelEvolution(starLevel).getSprite() + ".png"))
                    .append(Component.literal("${starLevel}, ").withColor(ChatFormatting.WHITE.color!!))
                    .append(Component.literal("%,d".format(currentXP)).withColor(ChatFormatting.WHITE.color!!))
                    .append(Component.literal("/" + "%,d".format(3000)).withColor(ChatFormatting.DARK_GRAY.color!!))
                    .append(Component.literal(" XP\n").withColor(ChatFormatting.GRAY.color!!))
                .append(mcciProgressBar(currentXP.toDouble() / 3000.0, 5))
                .append(Component.literal(" ${((currentXP.toDouble() / 3000.0 * 100.0).toInt())}%"))
            )
        }

        if (Galapagos.save.selectedFaction != null && shouldDisplay(Config.values::xpInfoFaction.get())) {
            val factionData = getFactionLevelAndProgress(Galapagos.save.factionXP[Galapagos.save.selectedFaction] ?: 0)
            val factionLevel = factionData.first
            val factionProgress = factionData.second
            val factionRequiredXP = FACTION_XP_PER_LEVEL.entries.find { factionLevel in it.key }?.value
                ?: throw IllegalStateException("Attempted to get XP at invalid level $factionLevel")

            +TextWidgets.multiLine(
                Component.empty()
                    .append(Glyphs.getGlyphComponent(Galapagos.save.selectedFaction!!.getSprite(factionLevel)))
                    .append(Component.literal(" ${Galapagos.save.selectedFaction!!.label}: Level ").withColor(ChatFormatting.GRAY.color!!))
                    .append(Component.literal("${factionLevel}, ").withColor(ChatFormatting.WHITE.color!!))
                    .append(Component.literal("%,d".format(factionProgress)).withColor(ChatFormatting.WHITE.color!!))
                    .append(Component.literal("/" + "%,d".format(factionRequiredXP)).withColor(ChatFormatting.DARK_GRAY.color!!))
                    .append(Component.literal(" XP\n").withColor(ChatFormatting.GRAY.color!!))
                    .append(mcciProgressBar(factionProgress.toDouble() / factionRequiredXP.toDouble(), 5))
                    .append(Component.literal(" ${((factionProgress.toDouble() / factionRequiredXP.toDouble() * 100.0).toInt())}%"))
            )
        }

        if (shouldDisplay(Config.values::xpInfoGameXP.get())) {
            +ThemedButton(Component.literal("${if (showBreakdown) "Hide" else "View"} XP Breakdown"), theme=this@XPInfoDialog) {
                showBreakdown = !showBreakdown
                super.init()
            }
        }
    }

    override val title = TextTitleWidget(this,
        Component.empty()
            .append(Glyphs.getGlyphComponent("_fonts/icon/stars/rainbow.png"))
            .append(Component.literal(" XP Info"))
    )
}