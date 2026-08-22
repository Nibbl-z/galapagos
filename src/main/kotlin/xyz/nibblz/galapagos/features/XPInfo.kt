package xyz.nibblz.galapagos.features

import com.noxcrew.noxesium.core.mcc.ClientboundMccServerPacket
import com.noxcrew.noxesium.core.mcc.ClientboundMccStatisticPacket
import com.noxcrew.sheeplib.DialogContainer
import kotlinx.serialization.Serializable
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.Style
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import net.minecraft.world.item.ItemStack
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.core.GameStateHandler
import xyz.nibblz.galapagos.core.game_state_handlers.*
import xyz.nibblz.galapagos.data.StarLevelGame
import xyz.nibblz.galapagos.data.XP_TABLE
import xyz.nibblz.galapagos.dialogs.XPInfoDialog
import xyz.nibblz.galapagos.events.ContainerOpenEvent
import xyz.nibblz.galapagos.events.ContainerSetSlotEvent
import xyz.nibblz.galapagos.events.MCCServerEvent
import xyz.nibblz.galapagos.events.MCCStatisticEvent
import xyz.nibblz.galapagos.mixin.GuiAccessor
import xyz.nibblz.galapagos.util.Glyphs
import xyz.nibblz.galapagos.util.Vector2
import xyz.nibblz.galapagos.util.findLore
import xyz.nibblz.galapagos.util.onIsland
import kotlin.math.roundToInt
import kotlin.time.Clock

object XPInfo : Feature {
    override val id: String = "xp_info"
    override val name: String = "XP Info"
    override val description: List<Component> = listOf(
        Component.literal("Displays a multitude of different statistics relating to Island XP, including:"),
        Component.literal("- XP Info Window: Displays today's XP, XP of your current game (with the option to view a breakdown of all XP earned today), and progress towards Daily Meter, Weekly Vault, Star Level, Faction Level, and any event-related meters if relevant."),
        Component.literal("- Navigator XP Stats: Displays today's XP, average XP per game from today's stats, and average XP per game from all-time stats under each game in the navigator."),
        Component.literal("- Projected XP: Shows how much XP you'll earn while in-game."),
        Component.literal("Note: Projected XP as of now only supports Battle Box, Battle Box Arena, Sky Battle Solo and Hole in the Wall. Other games will be added in a future update.").withStyle(Style.EMPTY.withItalic(true))
    )
    override val image: Config.ConfigImage = Config.ConfigImage("xp_info.png", 533, 908)

    @Serializable
    enum class XPSource(val serverTypes: List<String>, val lobbyServerType: String, val sprite: String, val label: String, val starLevelGame: StarLevelGame?, val stateHandler: Handler? = null) {
        BATTLE_BOX_QUADS(
            listOf("battle_box", "!arena"),
            "battle_box",
            "island_interface/game/battle_box/icon",
            "Battle Box",
            StarLevelGame.BATTLE_BOX,
            BattleBox
        ),
        BATTLE_BOX_ARENA(
            listOf("battle_box", "arena"),
            "battle_box",
            "island_interface/game/battle_box_arena/icon",
            "Battle Box Arena",
            StarLevelGame.BATTLE_BOX,
            BattleBoxArena
        )
        ,
        SKY_BATTLE_QUADS(listOf("sky_battle", "team"),
            "sky_battle",
            "island_interface/game/sky_battle/icon",
            "Sky Battle",
            StarLevelGame.SKY_BATTLE
        ),
        SKY_BATTLE_SOLOS(
            listOf("sky_battle", "solo"),
            "sky_battle",
            "island_interface/game/sky_battle_solo/icon",
            "Sky Battle Solo",
            StarLevelGame.SKY_BATTLE,
            SkyBattleSolo
        ),
        DYNABALL(
            listOf("dynaball"),
            "dynaball",
            "island_interface/game/dynaball/icon",
            "Dynaball"
            , StarLevelGame.DYNABALL
        ),
        TGTTOS(listOf("tgttos"),
            "tgttos",
            "island_interface/game/tgttosawaf/icon", // awaf? but there is no fans.. only chicken..
            "To Get To The Other Side",
            StarLevelGame.TGTTOS
        ),
        HOLE_IN_THE_WALL(
            listOf("hole_in_the_wall"),
            "hole_in_the_wall",
            "island_interface/game/hole_in_the_wall/icon",
            "Hole in the Wall",
            StarLevelGame.HOLE_IN_THE_WALL,
            HoleInTheWall
        ),
        PW_SURVIVAL(listOf("parkour_warrior", "survival"),
            "parkour_warrior",
            "island_interface/game/parkour_warrior/icon",
            "Parkour Warrior Survivor",
            StarLevelGame.PARKOUR_WARRIOR,
            ParkourWarriorSurvivor
        ),
        PW_SOLO(listOf("parkour_warrior", "dojo"),
            "parkour_warrior",
            "island_interface/game/parkour_warrior/solo/icon",
            "Parkour Warrior Dojo",
            StarLevelGame.PARKOUR_WARRIOR
        ),
        ROCKET_SPLEEF(
            listOf("rocket_spleef"),
            "rocket_spleef",
            "island_interface/game/rocket_spleef/icon",
            "Rocket Spleef Rush",
            StarLevelGame.ROCKET_SPLEEF
        ),
        FISHING(listOf("fishing"),
            "fishing",
            "island_interface/fishing/perk_icon/speedy_rod",
            "Fishing",
            null
        );

        val xpStatistic = "${name.lowercase()}_xp_earned"
        val gamesPlayedStatistic = "${name.lowercase()}_games_played"
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

    var projectedXP = 0

    var dialog: XPInfoDialog? = null
    var dialogLocation: Vector2 = Vector2(10, 10)
    var currentGames: MutableList<XPSource> = mutableListOf()
    var currentStarLevelGame: StarLevelGame? = null
    var inLobby: Boolean = true
    var dailyMeter: Claimable = Claimable(0, 7, 0, 500)
    var weeklyVault: Claimable = Claimable(0, 20, 0, 500)

    // Event related
    var seaMonstersActive = false
    var seaMonstersEnergyMeter: Claimable = Claimable(0, 40, 0, 500)

    override fun init() {
        MCCStatisticEvent.EVENT.register { packet -> mccStatistic(packet) }
        MCCServerEvent.EVENT.register { packet -> mccServer(packet) }
        ContainerOpenEvent.EVENT.register { packet -> containerOpen(packet) }
        ItemTooltipCallback.EVENT.register { item, _, _, components -> tooltipAdd(item, components) }
        ContainerSetSlotEvent.EVENT.register { packet -> containerSetSlot(packet) }
        HudElementRegistry.addFirst(Identifier.fromNamespaceAndPath(Galapagos.MOD_ID, id), hotbarXPInfoLayer())
    }

    fun mccStatistic(packet: ClientboundMccStatisticPacket) {
        handleXPStatistic(packet)
        handleProjectedXPStatistics(packet)
    }

    fun refreshDialog() {
        if (Config.values::xpInfoWindow.get() == Config.XPInfoDisplay.DISABLED || !enabled) {
            dialog?.close()
            return
        }

        dialogLocation = Vector2(dialog?.x ?: 10, dialog?.y ?: 10)

        if (dialog != null) {
            dialog!!.close()
        }

        dialog = XPInfoDialog(dialogLocation.x, dialogLocation.y)
        DialogContainer += dialog!!
    }

    fun getXPBoost(): Double {
        val actionBar = (Minecraft.getInstance().gui as GuiAccessor).`galapagos$getOverlayMesssageString`() ?: Component.empty()

        return when {
            actionBar.string.contains(Glyphs.getGlyph("_fonts/icon/xp_bonus_20.png")) -> 1.2
            actionBar.string.contains(Glyphs.getGlyph("_fonts/icon/xp_bonus.png")) -> 1.3
            actionBar.string.contains(Glyphs.getGlyph("_fonts/icon/xp_bonus_50.png")) -> 1.5
            else -> 1.0
        }
    }

    fun handleXPStatistic(packet: ClientboundMccStatisticPacket) {
        val source = XPSource.entries.find { packet.statistic == it.xpStatistic }
        if (source == null) return

        // fishing xp stat returns the boosted amount, games don't, soo...
        val amount = if (source == XPSource.FISHING) packet.value else (packet.value.toDouble() * getXPBoost()).roundToInt()

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

        seaMonstersEnergyMeter.currentXP = (seaMonstersEnergyMeter.currentXP + amount).coerceIn(0..seaMonstersEnergyMeter.requiredXP)

        if (source.starLevelGame != null) {
            Galapagos.save.starLevelXP[source.starLevelGame] = Galapagos.save.starLevelXP[source.starLevelGame]!! + packet.value
        }

        if (Galapagos.save.selectedFaction != null) {
            Galapagos.save.factionXP[Galapagos.save.selectedFaction!!] = Galapagos.save.factionXP[Galapagos.save.selectedFaction]!! + amount
        }

        refreshDialog()
    }

    fun handleProjectedXPStatistics(packet: ClientboundMccStatisticPacket) {
        if (currentGames.size != 1) return
        val currentGame = currentGames.first()
        val xpData = XP_TABLE[currentGame] ?: return
        xpData.basicStatisticTable.forEach { (statistic, xp) ->
            if (packet.statistic == statistic) projectedXP += xp
        }
    }

    fun mccServer(packet: ClientboundMccServerPacket) {
        if (dialog == null || dialog?.state?.isClosing == true) {
            dialog = XPInfoDialog(dialogLocation.x, dialogLocation.y)
            DialogContainer += dialog!!
        }

        currentGames.clear()
        currentStarLevelGame = null

        inLobby = packet.server == "lobby" || packet.server == "fishing"

        if (packet.server == "lobby") {
            XPSource.entries.forEach { if (packet.types.contains(it.lobbyServerType)) currentGames.add(it) }
            currentStarLevelGame = StarLevelGame.entries.find { packet.types.contains(it.name.lowercase()) }
        } else {
            XPSource.entries.forEach {
                if (it.serverTypes.all { type ->
                    if (type.startsWith("!")) !packet.types.contains(type.removePrefix("!")) else packet.types.contains(type)
                }) currentGames.add(it)
            }

            projectedXP = 0
        }

        if (
            ((packet.server == "lobby" || packet.server == "fishing") && Config.values::xpInfoWindow.get() == Config.XPInfoDisplay.ENABLED_GAMES)
            || (packet.server == "game" && Config.values::xpInfoWindow.get() == Config.XPInfoDisplay.ENABLED_LOBBY)
        ) {
            dialog?.close()
            return
        }
        refreshDialog()
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

    fun updateSeaMonstersEnergyMeter(item: ItemStack) {
        val meterClaimsMatch = item.findLore(meterClaimsRegex) ?: return
        val completedMeterClaims = meterClaimsMatch["completed"]?.value?.toIntOrNull() ?: return
        val totalMeterClaims = meterClaimsMatch["total"]?.value?.toIntOrNull() ?: return

        val meterProgressMatch = item.findLore(xpProgressRegex) ?: return
        val meterProgressCompleted = meterProgressMatch["completed"]?.value?.replace(",", "")?.toIntOrNull() ?: return
        val meterProgressTotal = meterProgressMatch["total"]?.value?.replace(",", "")?.toIntOrNull() ?: return

        seaMonstersEnergyMeter = Claimable(
            completedMeterClaims,
            totalMeterClaims,
            meterProgressCompleted,
            meterProgressTotal
        )

        Galapagos.logger.info("$seaMonstersEnergyMeter")
    }

    fun containerOpen(packet: ClientboundContainerSetContentPacket) {
        val screen = Minecraft.getInstance().screen ?: return

        if (screen.title.string.contains("ISLAND REWARDS")) {
            val dailyMeterItem = packet.items[13]
            val weeklyVaultItem = packet.items[16]

            updateDailyMeter(dailyMeterItem)
            updateWeeklyVault(weeklyVaultItem)

            val eventOrdersItem = packet.items.getOrNull(67) // 6767676767
            seaMonstersActive = eventOrdersItem?.itemName?.string == "Event Orders"
        }

        if (screen.title.string.contains("EVENT ORDERS")) {
            val energyMeterItem = packet.items[20]

            updateSeaMonstersEnergyMeter(energyMeterItem)
        }

        refreshDialog()
    }

    fun tooltipAdd(item: ItemStack, components: MutableList<Component>) {
        if (!enabled) return

        val screen = Minecraft.getInstance().screen ?: return
        if (!screen.title.string.contains("NAVIGATOR")) return

        val game = XPSource.entries.find { it.label == item.itemName.string } ?: return
        var index = components.indexOfFirst { it.string.contains("Click to") }
        if (index == -1) return

        if (Config.values::xpInfoNavigatorTodayXP.get()) {
            components.add(index,
                Component.literal("Today's XP: ").withColor(ChatFormatting.AQUA.color!!)
                    .append(Component.literal("%,d".format(dialog?.todayXP[game])).withColor(0xFFFFFF))
            )
            index++
        }

        if (game != XPSource.PW_SOLO && game != XPSource.FISHING) { // doesn't make sense to have this line for these games
            if (dialog?.todayXPEntries[game]!! != 0 && Config.values::xpInfoNavigatorTodayAverageXP.get()) {
                components.add(index,
                    Component.literal("Average XP/Game: ").withColor(ChatFormatting.AQUA.color!!)
                        .append(Component.literal("%,d".format(dialog?.todayXP[game]!! / dialog?.todayXPEntries[game]!!)).withColor(0xFFFFFF))
                        .append(Component.literal(" (Today)").withColor(ChatFormatting.GRAY.color!!))
                )
                index++
            }

            if (Galapagos.save.gamesPlayed[game]!! != 0 && Config.values::xpInfoNavigatorAlltimeAverageXP.get()) {
                components.add(index,
                    Component.literal("Average XP/Game: ").withColor(ChatFormatting.AQUA.color!!)
                        .append(Component.literal("%,d".format(Galapagos.save.gameXP[game]!! / Galapagos.save.gamesPlayed[game]!!)).withColor(0xFFFFFF))
                        .append(Component.literal(" (All-Time, Unmultiplied)").withColor(ChatFormatting.GRAY.color!!))
                )
                index++
            }
        }

        //whatever bro
        if (Config.values::xpInfoNavigatorAlltimeAverageXP.get() || Config.values::xpInfoNavigatorTodayXP.get() || Config.values::xpInfoNavigatorTodayAverageXP.get()) {
            components.add(index, Component.empty())
        }

        var endIndex = components.indexOfFirst { it.string.contains("minecraft:") } // if you have f3+h on :P
        if (endIndex == -1) { endIndex = components.size - 1 } // if you dont !

        if (game.stateHandler != null) {
            components.add(
                endIndex, Component.empty()
                    .append(Glyphs.getGlyphComponent("_fonts/icon/click_action_shift.png"))
                    .append(Component.literal("+").withColor(0xecd584))
                    .append(Glyphs.getGlyphComponent("_fonts/icon/click_action_right.png"))
                    .append(Component.literal(" > ").withColor(ChatFormatting.DARK_GRAY.color!!))
                    .append(Component.literal("Shift-Right-Click to ").withColor(0xecd584))
                    .append(Component.literal("View Past Games").withColor(0xfee761))
            )
        } else if (game != XPSource.PW_SOLO && game != XPSource.FISHING) {
            components.add(
                endIndex, Component.empty()
                    .append(Glyphs.getGlyphComponent("_fonts/icon/warning_blue.png"))
                    .append(Component.literal(" History for this game is coming Soon™!").withColor(ChatFormatting.AQUA.color!!))
            )
        }

    }

    fun containerSetSlot(packet: ClientboundContainerSetSlotPacket) {
        val screen = Minecraft.getInstance().screen ?: return

        if (packet.item.itemName.string == "Daily Meter" && screen.title.string.contains("ISLAND REWARDS")) updateDailyMeter(packet.item)
        if (packet.item.itemName.string == "Weekly Vault" && screen.title.string.contains("ISLAND REWARDS")) updateWeeklyVault(packet.item)
        // Event
        if (packet.item.itemName.string == "Event Energy Meter" && screen.title.string.contains("EVENT ORDERS")) updateSeaMonstersEnergyMeter(packet.item)

        refreshDialog()
    }

    fun hotbarXPInfoLayer(): HudElement {
        return element@{ graphics, _ ->
            if (!enabled) return@element
            if (!onIsland()) return@element
            if (GameStateHandler.currentState == null) return@element
            if (GameStateHandler.currentState is GameStateHandler.GameState.ParkourWarriorSurvivor) return@element // temporary hardcoding bc the xp tracking here is broken and ill fix this stuff later
            val font = FontDescription.Resource(Identifier.fromNamespaceAndPath("mcc", "hud"))

            graphics.text(
                Minecraft.getInstance().font,
                Component.literal("Projected XP: ").withColor(ChatFormatting.GRAY.color!!).withStyle(Style.EMPTY.withFont(font))
                    .append(Component.literal("%,d".format(((GameStateHandler.currentState?.projectedXP()?.toDouble() ?: 0.0) * getXPBoost()).roundToInt())).withColor(0xFFFFFF)),
                graphics.guiWidth() / 2 + 93, graphics.guiHeight() - 22,
                ARGB.opaque(0xFFFFFF)
            )
        }
    }
}