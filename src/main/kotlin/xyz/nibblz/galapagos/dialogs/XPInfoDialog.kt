package xyz.nibblz.galapagos.dialogs

import com.noxcrew.sheeplib.dialog.Dialog
import com.noxcrew.sheeplib.dialog.title.TextTitleWidget
import com.noxcrew.sheeplib.layout.linear
import com.noxcrew.sheeplib.theme.Themed
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.StringWidget
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.network.chat.Component
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.features.XPInfo
import xyz.nibblz.galapagos.util.Glyphs
import xyz.nibblz.galapagos.util.mcciProgressBar
import xyz.nibblz.galapagos.util.mcciTextureComponent
import java.util.EnumMap
import kotlin.time.Clock
import kotlin.time.Instant

class XPInfoDialog(x: Int, y: Int) : Dialog(x, y), Themed by GalapagosTheme {
    fun refresh() {
        super.init()
    }

    override fun layout() = linear(LinearLayout.Orientation.VERTICAL) {
        val font = Minecraft.getInstance().font

        val today = if (Config.values::startDayAtQuestRefresh.get())
            Instant.fromEpochSeconds(Clock.System.now().epochSeconds + (60 * 60 * 10)).toLocalDateTime(TimeZone.UTC)
        else Instant.fromEpochSeconds(Clock.System.now().epochSeconds).toLocalDateTime(TimeZone.currentSystemDefault())

        Galapagos.logger.info("${today.day}")

        val todayXP = XPInfo.XPSource.entries.associateWithTo(EnumMap(XPInfo.XPSource::class.java)) { 0 }

        Galapagos.save.xpGains.forEach {
            val date = if (Config.values::startDayAtQuestRefresh.get())
                Instant.fromEpochSeconds(it.timestamp + (60 * 60 * 10)).toLocalDateTime(TimeZone.UTC)
            else Instant.fromEpochSeconds(it.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())

            if (date.day != today.day || date.month != today.month || date.year != today.year) return@forEach

            todayXP[it.source] = todayXP[it.source]!! + it.amount
        }

        +StringWidget(Component.literal("Today's XP: ${todayXP.entries.sumOf { it.value }}"), font)

        XPInfo.currentGames.forEach {
            +StringWidget(Component.empty()
                .append(mcciTextureComponent(it.sprite))
                .append(Component.literal(" ${it.label} XP: ${todayXP[it]}")), font)
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

        +StringWidget(
            mcciTextureComponent(meterSprite)
            .append(Component.literal(" Daily Meter: ").withColor(ChatFormatting.GRAY.color!!))
                .append(Component.literal("${XPInfo.dailyMeter.completed}").withColor(ChatFormatting.WHITE.color!!))
                .append(Component.literal("/${XPInfo.dailyMeter.total}, ").withColor(ChatFormatting.DARK_GRAY.color!!))
                .append(Component.literal("%,d".format(XPInfo.dailyMeter.currentXP)).withColor(ChatFormatting.WHITE.color!!))
                .append(Component.literal("/" + "%,d".format(XPInfo.dailyMeter.requiredXP)).withColor(ChatFormatting.DARK_GRAY.color!!))
                .append(Component.literal(" XP").withColor(ChatFormatting.GRAY.color!!)),
                font)
        +StringWidget(Component.empty()
            .append(mcciProgressBar(meterProgress, 5))
            .append(Component.literal(" ${((XPInfo.dailyMeter.currentXP.toDouble() / XPInfo.dailyMeter.requiredXP.toDouble() * 100.0).toInt())}%")), font)

        val vaultSprite = when (XPInfo.weeklyVault.completed) {
            0 -> "island_interface/quest_log/meters/daily_vault_empty"
            XPInfo.weeklyVault.total -> "island_interface/quest_log/meters/daily_vault_full"
            else -> "island_interface/quest_log/meters/daily_vault_partly_full"
        }

        +StringWidget(
            mcciTextureComponent(vaultSprite)
                .append(Component.literal(" Weekly Vault: ").withColor(ChatFormatting.GRAY.color!!))
                .append(Component.literal("${XPInfo.weeklyVault.completed}").withColor(ChatFormatting.WHITE.color!!))
                .append(Component.literal("/${XPInfo.weeklyVault.total}, ").withColor(ChatFormatting.DARK_GRAY.color!!))
                .append(Component.literal("%,d".format(XPInfo.weeklyVault.currentXP)).withColor(ChatFormatting.WHITE.color!!))
                .append(Component.literal("/" + "%,d".format(XPInfo.weeklyVault.requiredXP)).withColor(ChatFormatting.DARK_GRAY.color!!))
                .append(Component.literal(" XP").withColor(ChatFormatting.GRAY.color!!)),
            font)
        +StringWidget(Component.empty()
            .append(mcciProgressBar(XPInfo.weeklyVault.currentXP.toDouble() / XPInfo.weeklyVault.requiredXP.toDouble(), 5))
            .append(Component.literal(" ${((XPInfo.weeklyVault.currentXP.toDouble() / XPInfo.weeklyVault.requiredXP.toDouble() * 100.0).toInt())}%")), font)
    }

    override val title = TextTitleWidget(this,
        Component.empty()
            .append(Glyphs.getGlyphComponent("_fonts/icon/stars/rainbow.png"))
            .append(Component.literal(" XP Info"))
    )


}