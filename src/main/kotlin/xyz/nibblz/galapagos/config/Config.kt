package xyz.nibblz.galapagos.config

import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler
import dev.isxander.yacl3.config.v2.api.SerialEntry
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder
import dev.isxander.yacl3.dsl.*
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import xyz.nibblz.galapagos.Galapagos
import java.text.DecimalFormat

class Config {
    data class ConfigImage(
        val path: String,
        val w: Int,
        val h: Int
    )

    // API
    @SerialEntry
    var usePersonalApiKey: Boolean = false

    // Features

    @SerialEntry
    var features: MutableMap<String, Boolean> = mutableMapOf()

    // Crate Chances
    @SerialEntry
    var highlightBestRepChance: Boolean = true
    @SerialEntry
    var highlightBestCosmeticChance: Boolean = true
    @SerialEntry
    var showNewCosmeticChance: Boolean = true
    @SerialEntry
    var showNewRepChance: Boolean = true
    @SerialEntry
    var showTrophiesPerRoll: Boolean = true
    @SerialEntry
    var showMythicCoresPerRoll: Boolean = true
    @SerialEntry
    var showArcaneCoresPerRoll: Boolean = true
    @SerialEntry
    var showMaxRepCrates: Boolean = true
    @SerialEntry
    var showMaxCosmeticCrates: Boolean = true

    // Cosmetic Machine Chances
    @SerialEntry
    var detailedCosmeticMachineChances: Boolean = true
    @SerialEntry
    var showNewCosmeticChancePerPull: Boolean = true
    @SerialEntry
    var showNewRepChancePerPull: Boolean = true
    @SerialEntry
    var showTrophiesPerPull: Boolean = true
    @SerialEntry
    var showMythicCoresPerPull: Boolean = true
    @SerialEntry
    var showArcaneCoresPerPull: Boolean = true

    // Island Exchange Unit Price
    @SerialEntry
    var exchangeShowUnitPrice: Boolean = true
    @SerialEntry
    var exchangeShowSoulEquivalent: Boolean = true
    @SerialEntry
    var exchangeShowWispEquivalent: Boolean = true

    // Crafting Instructions
    @SerialEntry
    var craftingInstructionsShowCraftTime: Boolean = true
    @SerialEntry
    var craftingInstructionsShowGloop: Boolean = true

    // Blueprint Assembler Info
    @SerialEntry
    var assemblerInfoShowNewTrophies: Boolean = true
    @SerialEntry
    var assemblerInfoShowNewRep: Boolean = true

    enum class AssemblerCoreInfoType(val label: String, val description: String) {
        DISABLED("Disabled", "Disables showing info of this core type."),
        ENABLED("Enabled", "Shows only how many of this core type will directly be earned from scavenging."),
        CONVERSION("Enabled with Conversions", "Shows how many of this core type will be earned both directly from scavenging and from upcrafting/downcrafting cores of other types.");

        fun descriptionComponent(): MutableComponent {
            return Component.empty()
                .append(Component.literal(label).withStyle(Style.EMPTY.withBold(true)))
                .append(Component.literal(" - $description"))
        }
    }

    @SerialEntry
    var assemblerInfoStandardCores: AssemblerCoreInfoType = AssemblerCoreInfoType.CONVERSION
    @SerialEntry
    var assemblerInfoExclusiveCores: AssemblerCoreInfoType = AssemblerCoreInfoType.CONVERSION
    @SerialEntry
    var assemblerInfoMythicCores: AssemblerCoreInfoType = AssemblerCoreInfoType.CONVERSION
    @SerialEntry
    var assemblerInfoArcaneCores: AssemblerCoreInfoType = AssemblerCoreInfoType.CONVERSION

    // Weekly Vault Info
    @SerialEntry
    var weeklyVaultInfoShowTotalProgress: Boolean = true
    @SerialEntry
    var weeklyVaultInfoShowNeededXPPerDay: Boolean = true

    // Trophy Tracking
    @SerialEntry
    var trophyTrackingShowTypeBreakdown: Boolean = true
    @SerialEntry
    var trophyTrackingShowCategoryBreakdown: Boolean = true

    // Average Income
    @SerialEntry
    var averageIncomeIncludeQuestScrolls: Boolean = true
    @SerialEntry
    var averageIncomeDailies: Int = 10
    @SerialEntry
    var averageIncomeWeeklies: Int = 10
    @SerialEntry
    var averageIncomeMeters: Int = 15
    @SerialEntry
    var averageIncomeVaultClaims: Int = 60

    // XP Info

    enum class XPInfoDisplay(val label: String, val description: String) {
        DISABLED("Disabled", "Will never show up."),
        ENABLED("Enabled", "Will always show up."),
        ENABLED_LOBBY("Enabled in Lobbies", "Will only show up in lobbies or fishing."),
        ENABLED_GAMES("Enabled in Games", "Will only show up while in-game.");

        fun descriptionComponent(): MutableComponent {
            return Component.empty()
                .append(Component.literal(label).withStyle(Style.EMPTY.withBold(true)))
                .append(Component.literal(" - $description"))
        }
    }

    @SerialEntry
    var xpInfoWindow: XPInfoDisplay = XPInfoDisplay.ENABLED
    @SerialEntry
    var xpInfoWindowCompact: Boolean = false
    @SerialEntry
    var xpInfoDailyMeter: XPInfoDisplay = XPInfoDisplay.ENABLED
    @SerialEntry
    var xpInfoDisableDailyMeterIfMax: Boolean = true
    @SerialEntry
    var xpInfoWeeklyVault: XPInfoDisplay = XPInfoDisplay.ENABLED
    @SerialEntry
    var xpInfoDisableWeeklyVaultIfMax: Boolean = true
    @SerialEntry
    var xpInfoStarLevel: XPInfoDisplay = XPInfoDisplay.ENABLED
    @SerialEntry
    var xpInfoFaction: XPInfoDisplay = XPInfoDisplay.ENABLED
    @SerialEntry
    var xpInfoTodaysXP: XPInfoDisplay = XPInfoDisplay.ENABLED
    @SerialEntry
    var xpInfoGameXP: XPInfoDisplay = XPInfoDisplay.ENABLED
    @SerialEntry
    var xpInfoSeaMonstersEnergyMeter: XPInfoDisplay = XPInfoDisplay.ENABLED
    @SerialEntry
    var xpInfoDisableSeaMonstersEnergyMeterIfMax: Boolean = true // best config key ever?
    @SerialEntry
    var xpInfoNavigatorTodayXP: Boolean = true
    @SerialEntry
    var xpInfoNavigatorTodayAverageXP: Boolean = true
    @SerialEntry
    var xpInfoNavigatorAlltimeAverageXP: Boolean = true
    @SerialEntry
    var xpInfoNavigatorViewHistoryEnabled: Boolean = true

    // Misc
    @SerialEntry
    var twentyFourHourTime: Boolean = false
    @SerialEntry
    var startDayAtQuestRefresh: Boolean = false
    @SerialEntry
    var decimalPoints: Int = 3

    companion object {
        val handler: ConfigClassHandler<Config> = ConfigClassHandler.createBuilder(Config::class.java)
            .id(Identifier.fromNamespaceAndPath(Galapagos.MOD_ID, "config"))
            .serializer { config -> GsonConfigSerializerBuilder.create(config)
                .setPath(FabricLoader.getInstance().configDir.resolve("galapagos.json"))
                .build()
            }

            .build()

        val values: Config
            get() {
                return handler.instance()
            }

        fun getScreen(parent: Screen): Screen = YetAnotherConfigLib("galapagos") {
            title(Component.literal("Galapagos"))
            save {
                handler.save()
            }

            categories.register("global") {
                name(Component.literal("Global"))

                groups.register("api") {
                    name(Component.literal("API"))

                    options.register("api_use_key") {
                        name(Component.literal("Use Own API Key"))
                        description(
                            OptionDescription.of(
                                Component.literal("The MCC Island API is utilized in this mod to fetch cosmetic ownership, infinibag, and infinivault state."),
                                Component.empty(),
                                Component.literal("Therefore, please ensure that the Infinibag and Collections APIs are enabled in Pocket Menu->Settings->API Settings for the mod to function!"),
                                Component.empty(),
                                Component.literal("If enabled, API features will require the use of your own API key by running the command /galapagos api set <API_KEY>."),
                                Component.literal("If you do not have an API key, you can generate one at ").append(
                                    Component.literal("https://gateway.noxcrew.com/.").setStyle(
                                        Style.EMPTY
                                            .withUnderlined(true)
                                            .withColor(ChatFormatting.AQUA.color!!)
                                    )
                                ),
                                Component.empty(),
                                Component.literal("Using your own API key is preferred, as you can make requests much more often."),
                                Component.empty(),
                                Component.literal("If you are unable to supply your own API key, a different endpoint will be used, making API calls with the developer's own API key. This means you can make less requests per minute, and uptime of this backend is not guaranteed.")
                            )
                        )
                        controller(tickBox())
                        binding(values::usePersonalApiKey, false)
                    }
                }

                groups.register("features") {
                    name(Component.literal("Features"))
                    tooltip(Component.literal("Toggles all functionality of individual features"))

                    Galapagos.features.forEach { feature ->
                        options.register(feature.id) {
                            name(Component.literal(feature.name))
                            description(
                                OptionDescription.createBuilder()
                                    .text(feature.description)
                                    .image(
                                        Identifier.fromNamespaceAndPath(
                                            "galapagos",
                                            "textures/config/${feature.image.path}"
                                        ), feature.image.w, feature.image.h
                                    )
                                    .build()
                            )
                            controller(tickBox())
                            binding(true, { feature.enabled }, { value -> feature.enabled = value })
                        }
                    }

                    options.register("game_history_enabled") {
                        name(Component.literal("Game History"))
                        description(
                            OptionDescription.createBuilder()
                            .text(listOf(
                                Component.literal("Allows you to view history and stats of past games, which currently only includes:"),
                                Component.literal("- Battle Box"),
                                Component.literal("- Battle Box Arena"),
                                Component.literal("- Sky Battle Solos"),
                            ))
                            .image(Identifier.fromNamespaceAndPath(
                                "galapagos",
                                "textures/config/game_history.png"
                            ), 616, 871)
                            .build()
                        )
                        controller(tickBox())
                        binding(values::xpInfoNavigatorViewHistoryEnabled, true)
                    }
                }

                groups.register("misc") {
                    name(Component.literal("Miscellaneous"))

                    options.register("24_hour_time") {
                        name(Component.literal("24-Hour Time"))
                        description(OptionDescription.of(
                            Component.literal("Switches timestamps wherever used to be 24-hour time instead of 12-hour time.")
                        ))
                        controller(tickBox())
                        binding(values::twentyFourHourTime, false)
                    }

                    options.register("start_day_at_quest_refresh") {
                        name(Component.literal("Start Day at Quest Refresh"))
                        description(OptionDescription.of(
                            Component.literal("In any history menu where items are categorized by day, enabling this setting will make the day start at 10 a.m UTC, which is when MCC Island refreshes quests.")
                        ))
                        controller(tickBox())
                        binding(values::startDayAtQuestRefresh, false)
                    }

                    options.register("decimal_points") {
                        name(Component.literal("Decimal Points"))
                        description(OptionDescription.of(
                            Component.literal("Changes the amount of decimal points shown for any information that Galapagos shows including decimals, such as chances or average amounts.")
                        ))
                        controller(slider(0..10))
                        addListener { option, _ ->
                            Galapagos.decimalFormat = DecimalFormat("#${if (option.pendingValue() > 0) "." else ""}${"#".repeat(option.pendingValue())}")
                        }
                        binding(values::decimalPoints, 3)
                    }
                }
            }

            categories.register("crate_chances") {
                name(Component.literal("Crate Chances"))

                rootOptions.register("crate_chances_highlight_rep") {
                    name(Component.literal("Highlight Best Rep Chance"))
                    description(OptionDescription.of(
                        Component.literal("Highlights the standard and exclusive crates with the highest chance for new royal reputation.")
                    ))
                    controller(tickBox())
                    binding(values::highlightBestRepChance, true)
                }

                rootOptions.register("crate_chances_highlight_cosmetic") {
                    name(Component.literal("Highlight Best Cosmetic Chance"))
                    description(OptionDescription.of(
                        Component.literal("Highlights the standard and exclusive crates with the highest chance for a new cosmetic.")
                    ))
                    controller(tickBox())
                    binding(values::highlightBestCosmeticChance, true)
                }

                rootOptions.register("crate_chances_show_cosmetic_chance") {
                    name(Component.literal("Show New Cosmetic Chance"))
                    description(OptionDescription.of(
                        Component.literal("Shows the percent chance that the crate will give a new cosmetic.")
                    ))
                    controller(tickBox())
                    binding(values::showNewCosmeticChance, true)
                }

                rootOptions.register("crate_chances_show_rep_chance") {
                    name(Component.literal("Show New Royal Reputation Chance"))
                    description(OptionDescription.of(
                        Component.literal("Shows the percent chance that the crate will give new royal reputation.")
                    ))
                    controller(tickBox())
                    binding(values::showNewRepChance, true)
                }

                rootOptions.register("crate_chances_show_trophies_per_roll") {
                    name(Component.literal("Show Trophies per Roll"))
                    description(OptionDescription.of(
                        Component.literal("Shows the average amount of trophies you will earn from opening a crate, including the trophies from new cosmetics and royal reputation.")
                    ))
                    controller(tickBox())
                    binding(values::showTrophiesPerRoll, true)
                }

                rootOptions.register("crate_chances_show_mythic_cores_per_roll") {
                    name(Component.literal("Show Mythic Cores per Roll"))
                    description(OptionDescription.of(
                        Component.literal("Shows the average amount of mythic cores you will earn from opening a crate. If the crate is exclusive, this is the mythic cores you will earn from scavenging any earned arcane cores.")
                    ))
                    controller(tickBox())
                    binding(values::showMythicCoresPerRoll, true)
                }

                rootOptions.register("crate_chances_show_arcane_cores_per_roll") {
                    name(Component.literal("Show Arcane Cores per Roll"))
                    description(OptionDescription.of(
                        Component.literal("Shows the average amount of arcane cores you will earn from opening a crate. If the crate is standard, this is the arcane cores you will earn from upcrafting any earned mythic cores.")
                    ))
                    controller(tickBox())
                    binding(values::showArcaneCoresPerRoll, true)
                }

                rootOptions.register("crate_chances_show_max_cosmetic_crates") {
                    name(Component.literal("Maxed Cosmetic Crate Icon"))
                    description(OptionDescription.of(
                        Component.literal("Shows a style trophy icon in the corner of crates with all cosmetics earned.")
                    ))
                    controller(tickBox())
                    binding(values::showMaxCosmeticCrates, true)
                }

                rootOptions.register("crate_chances_show_max_rep_crates") {
                    name(Component.literal("Maxed Royal Reputation Crate Icon"))
                    description(OptionDescription.of(
                        Component.literal("Shows a royal reputation icon in the corner of crates with all royal reputation earned.")
                    ))
                    controller(tickBox())
                    binding(values::showMaxRepCrates, true)
                }
            }

            categories.register("cosmetic_machine") {
                name(Component.literal("Cosmetic Machine"))

                rootOptions.register("cosmetic_machine_detailed_chances") {
                    name(Component.literal("Detailed Chances"))
                    description(OptionDescription.createBuilder()
                        .text(Component.literal("Shows the specific chance for non-exclusive, exclusive, and arcane pulls per rarity in the tooltips of the pull buttons."))
                        .image(Identifier.fromNamespaceAndPath("galapagos", "textures/config/detailed_cosmetic_machine.png"), 400, 427)
                        .build()
                    )
                    controller(tickBox())
                    binding(values::detailedCosmeticMachineChances, true)
                }

                rootOptions.register("cosmetic_machine_show_cosmetic_chance") {
                    name(Component.literal("Show New Cosmetic Chance"))
                    description(OptionDescription.of(
                        Component.literal("Shows the percent chance that the pull will give a new cosmetic.")
                    ))
                    controller(tickBox())
                    binding(values::showNewCosmeticChancePerPull, true)
                }

                rootOptions.register("cosmetic_machine_show_rep_chance") {
                    name(Component.literal("Show New Royal Reputation Chance"))
                    description(OptionDescription.of(
                        Component.literal("Shows the percent chance that the pull will give new royal reputation.")
                    ))
                    controller(tickBox())
                    binding(values::showNewRepChancePerPull, true)
                }

                rootOptions.register("cosmetic_machine_show_trophies_per_roll") {
                    name(Component.literal("Show Trophies per Pull"))
                    description(OptionDescription.of(
                        Component.literal("Shows the average amount of trophies you will earn from pulling a key, including the trophies from new cosmetics and royal reputation.")
                    ))
                    controller(tickBox())
                    binding(values::showTrophiesPerPull, true)
                }

                rootOptions.register("cosmetic_machine_show_mythic_cores_per_roll") {
                    name(Component.literal("Show Mythic Cores per Pull"))
                    description(OptionDescription.of(
                        Component.literal("Shows the average amount of mythic cores you will earn from pulling a key. This includes mythic cores you will earn from scavenging any earned arcane cores.")
                    ))
                    controller(tickBox())
                    binding(values::showMythicCoresPerPull, true)
                }

                rootOptions.register("cosmetic_machine_show_arcane_cores_per_roll") {
                    name(Component.literal("Show Arcane Cores per Pull"))
                    description(OptionDescription.of(
                        Component.literal("Shows the average amount of arcane cores you will earn from pulling a key. This includesarcane cores you will earn from upcrafting any earned mythic cores.")
                    ))
                    controller(tickBox())
                    binding(values::showArcaneCoresPerPull, true)
                }
            }

            categories.register("island_exchange") {
                name(Component.literal("Island Exchange"))

                rootOptions.register("island_exchange_unit_price") {
                    name(Component.literal("Show Listing Unit Price"))
                    description(OptionDescription.of(
                        Component.literal("If a listing on Island Exchange contains multiple of one item, the price per unit will show in the tooltip.")
                    ))
                    controller(tickBox())
                    binding(values::exchangeShowUnitPrice, true)
                }

                rootOptions.register("island_exchange_soul_equivalent") {
                    name(Component.literal("Show Style Soul Equivalent"))
                    description(OptionDescription.of(
                        Component.literal("Shows the equivalent of a cosmetic listing on Island Exchange in style souls if scavenged.")
                    ))
                    controller(tickBox())
                    binding(values::exchangeShowSoulEquivalent, true)
                }

                rootOptions.register("island_exchange_wisp_equivalent") {
                    name(Component.literal("Show Weapon Wisp Equivalent"))
                    description(OptionDescription.of(
                        Component.literal("Shows the equivalent of a weapon skin listing on Island Exchange in weapon wisps if scavenged.")
                    ))
                    controller(tickBox())
                    binding(values::exchangeShowWispEquivalent, true)
                }
            }

            categories.register("crafting_instructions") {
                name(Component.literal("Crafting Instructions"))

                rootOptions.register("crafting_instructions_show_time") {
                    name(Component.literal("Show Crafting Time"))
                    description(OptionDescription.of(
                        Component.literal("Shows crafting time for items that need to be crafted, as well as total craft time, in the list of instructions.")
                    ))
                    controller(tickBox())
                    binding(values::craftingInstructionsShowCraftTime, true)
                }

                rootOptions.register("crafting_instructions_show_gloop") {
                    name(Component.literal("Show Material Gloop"))
                    description(OptionDescription.of(
                        Component.literal("Shows material gloop cost for items that need to be purchased from the material market, as well as total gloop cost, in the list of instructions.")
                    ))
                    controller(tickBox())
                    binding(values::craftingInstructionsShowGloop, true)
                }
            }

            categories.register("assembler_info") {
                name(Component.literal("Blueprint Assembler Info"))

                rootOptions.register("assembler_show_new_trophies") {
                    name(Component.literal("Show New Trophies"))
                    description(OptionDescription.of(
                        Component.literal("Shows the total style trophies earnable from new cosmetic blueprints.")
                    ))
                    controller(tickBox())
                    binding(values::assemblerInfoShowNewTrophies, true)
                }

                rootOptions.register("assembler_show_new_rep") {
                    name(Component.literal("Show New Royal Reputation"))
                    description(OptionDescription.of(
                        Component.literal("Shows the total royal reputation earnable from blueprints.")
                    ))
                    controller(tickBox())
                    binding(values::assemblerInfoShowNewRep, true)
                }

                rootOptions.register("assembler_show_standard_cores") {
                    name(Component.literal("Standard Core Display"))
                    description(OptionDescription.of(
                        Component.literal("Controls how the info for Standard Cores obtained from blueprints is displayed."),
                        Component.empty(),
                        AssemblerCoreInfoType.DISABLED.descriptionComponent(),
                        AssemblerCoreInfoType.ENABLED.descriptionComponent(),
                        AssemblerCoreInfoType.CONVERSION.descriptionComponent()
                    ))
                    binding(values::assemblerInfoStandardCores, AssemblerCoreInfoType.CONVERSION)
                    controller(enumSwitch<AssemblerCoreInfoType> {
                        Component.literal(it.label)
                    })
                }

                rootOptions.register("assembler_show_exclusive_cores") {
                    name(Component.literal("Exclusive Core Display"))
                    description(OptionDescription.of(
                        Component.literal("Controls how the info for Exclusive Cores obtained from blueprints is displayed."),
                        Component.empty(),
                        AssemblerCoreInfoType.DISABLED.descriptionComponent(),
                        AssemblerCoreInfoType.ENABLED.descriptionComponent(),
                        AssemblerCoreInfoType.CONVERSION.descriptionComponent()
                    ))
                    binding(values::assemblerInfoExclusiveCores, AssemblerCoreInfoType.CONVERSION)
                    controller(enumSwitch<AssemblerCoreInfoType> {
                        Component.literal(it.label)
                    })
                }

                rootOptions.register("assembler_show_mythic_cores") {
                    name(Component.literal("Mythic Core Display"))
                    description(OptionDescription.of(
                        Component.literal("Controls how the info for Mythic Cores obtained from blueprints is displayed."),
                        Component.empty(),
                        AssemblerCoreInfoType.DISABLED.descriptionComponent(),
                        AssemblerCoreInfoType.ENABLED.descriptionComponent(),
                        AssemblerCoreInfoType.CONVERSION.descriptionComponent()
                    ))
                    binding(values::assemblerInfoMythicCores, AssemblerCoreInfoType.CONVERSION)
                    controller(enumSwitch<AssemblerCoreInfoType> {
                        Component.literal(it.label)
                    })
                }

                rootOptions.register("assembler_show_arcane_cores") {
                    name(Component.literal("Arcane Core Display"))
                    description(OptionDescription.of(
                        Component.literal("Controls how the info for Arcane Cores obtained from blueprints is displayed."),
                        Component.empty(),
                        AssemblerCoreInfoType.DISABLED.descriptionComponent(),
                        AssemblerCoreInfoType.ENABLED.descriptionComponent(),
                        AssemblerCoreInfoType.CONVERSION.descriptionComponent()
                    ))
                    binding(values::assemblerInfoArcaneCores, AssemblerCoreInfoType.CONVERSION)
                    controller(enumSwitch<AssemblerCoreInfoType> {
                        Component.literal(it.label)
                    })
                }
            }

            categories.register("weekly_vault_info") {
                name(Component.literal("Weekly Vault Info"))

                rootOptions.register("weekly_vault_info_show_total_progress") {
                    name(Component.literal("Show Total Progress"))
                    description(OptionDescription.of(
                        Component.literal("Shows the total XP you've earned towards reaching max claims on your weekly vault.")
                    ))
                    controller(tickBox())
                    binding(values::weeklyVaultInfoShowTotalProgress, true)
                }

                rootOptions.register("weekly_vault_info_show_needed_xp_per_day") {
                    name(Component.literal("Show Needed XP Per Day"))
                    description(OptionDescription.of(
                        Component.literal("Shows an average amount of XP to earn each day in order to reach max claims on your weekly vault.")
                    ))
                    controller(tickBox())
                    binding(values::weeklyVaultInfoShowNeededXPPerDay, true)
                }
            }

            categories.register("trophy_tracking") {
                name(Component.literal("Trophy Tracking"))

                rootOptions.register("trophy_tracking_show_type_breakdown") {
                    name(Component.literal("Show Trophy Type Breakdown"))
                    description(OptionDescription.of(
                        Component.literal("For each day listed, shows a breakdown of how many of each type of trophy is obtained, including Skill, Style, and Angler.")
                    ))
                    controller(tickBox())
                    binding(values::trophyTrackingShowTypeBreakdown, true)
                }

                rootOptions.register("trophy_tracking_show_category_breakdown") {
                    name(Component.literal("Show Trophy Source Breakdown"))
                    description(OptionDescription.of(
                        Component.literal("For each day listed, shows a breakdown of how many of each trophy gain source is obtained, including:"),
                        Component.literal("- Claiming badges"),
                        Component.literal("- Claiming cosmetics"),
                        Component.literal("- Royal reputation"),
                        Component.literal("- Obtaining max chromas on a cosmetic"),
                        Component.literal("- Collection bonuses"),
                        Component.literal("- Discovering new fish"),
                        Component.literal("- Claiming fishing research"),
                        Component.literal("- Purchasing fishing upgrades"),
                    ))
                    controller(tickBox())
                    binding(values::trophyTrackingShowCategoryBreakdown, true)
                }
            }

            categories.register("average_income") {
                name(Component.literal("Average Income"))

                rootOptions.register("average_income_include_scrolls") {
                    name(Component.literal("Include Quest Scrolls alongside Dailies"))
                    description(OptionDescription.of(
                        Component.literal("Adds another line under any average income including daily quests, making the assumption that you will complete whatever your current highest rarity of quest scroll is alongside the daily quest.")
                    ))
                    controller(tickBox())
                    binding(values::averageIncomeIncludeQuestScrolls, true)
                }

                rootOptions.register("average_income_dailies") {
                    name(Component.literal("Average Daily Quests/Day"))
                    description(OptionDescription.of(
                        Component.literal("Set here how many daily quests you think you will complete on average every day."),
                        Component.empty(),
                        Component.literal("This value will be clamped to whatever your actual max daily quest count is.")
                    ))
                    controller(slider(0..10))
                    binding(values::averageIncomeDailies, 10)
                }

                rootOptions.register("average_income_weeklies") {
                    name(Component.literal("Average Weekly Quests/Week"))
                    description(OptionDescription.of(
                        Component.literal("Set here how many weekly quests you think you will complete on average every week."),
                        Component.empty(),
                        Component.literal("This value will be clamped to whatever your actual max weekly quest count is.")
                    ))
                    controller(slider(0..10))
                    binding(values::averageIncomeWeeklies, 10)
                }

                rootOptions.register("average_income_meters") {
                    name(Component.literal("Average Daily Meter Claims/Day"))
                    description(OptionDescription.of(
                        Component.literal("Set here how many daily meter claims you think you will complete on average every day."),
                        Component.empty(),
                        Component.literal("This value will be clamped to whatever your actual max daily meter claims are.")
                    ))
                    controller(slider(0..15))
                    binding(values::averageIncomeMeters, 15)
                }

                rootOptions.register("average_income_vault_claims") {
                    name(Component.literal("Average Weekly Vault Claims/Week"))
                    description(OptionDescription.of(
                        Component.literal("Set here how many weekly vault claim you think you will complete on average every week."),
                        Component.empty(),
                        Component.literal("This value will be clamped to whatever your actual max weekly vault claims are.")
                    ))
                    controller(slider(0..60))
                    binding(values::averageIncomeVaultClaims, 60)
                }
            }

            categories.register("xp_info") {
                name(Component.literal("XP Info"))

                rootOptions.register("xp_info_window_compact") {
                    name(Component.literal("Compact XP Info Window"))
                    description(OptionDescription.of(
                        Component.literal("Removes/shortens some unnecessary things from the XP Info window's info, including:"),
                        Component.literal("- Meter names"),
                        Component.literal("- Smaller progress bars"),
                        Component.literal("- Truncated XP requirements")
                    ))
                    controller(tickBox())
                    binding(values::xpInfoWindowCompact, false)
                }

                rootOptions.register("xp_info_window") {
                    name(Component.literal("XP Info Window"))
                    description(OptionDescription.of(
                        Component.literal("Controls when the XP Info window should be open."),
                        XPInfoDisplay.DISABLED.descriptionComponent(),
                        XPInfoDisplay.ENABLED.descriptionComponent(),
                        XPInfoDisplay.ENABLED_GAMES.descriptionComponent(),
                        XPInfoDisplay.ENABLED_LOBBY.descriptionComponent(),
                    ))
                    controller(enumSwitch<XPInfoDisplay> {
                        Component.literal(it.label)
                    })
                    binding(values::xpInfoWindow, XPInfoDisplay.ENABLED)
                }

                rootOptions.register("xp_info_daily_meter") {
                    name(Component.literal("Daily Meter Display"))
                    description(OptionDescription.of(
                        Component.literal("Controls when the Daily Meter progress bar should be visible on the XP window."),
                        XPInfoDisplay.DISABLED.descriptionComponent(),
                        XPInfoDisplay.ENABLED.descriptionComponent(),
                        XPInfoDisplay.ENABLED_GAMES.descriptionComponent(),
                        XPInfoDisplay.ENABLED_LOBBY.descriptionComponent(),
                    ))
                    controller(enumSwitch<XPInfoDisplay> {
                        Component.literal(it.label)
                    })
                    binding(values::xpInfoDailyMeter, XPInfoDisplay.ENABLED)
                }

                rootOptions.register("xp_info_disable_daily_meter_if_max") {
                    name(Component.literal("Disable Daily Meter if Max"))
                    description(OptionDescription.of(
                        Component.literal("If your Daily Meter is at max claims, it will be hidden from the XP window.")
                    ))
                    controller(tickBox())
                    binding(values::xpInfoDisableDailyMeterIfMax, true)
                }

                rootOptions.register("xp_info_weekly_vault") {
                    name(Component.literal("Weekly Vault Display"))
                    description(OptionDescription.of(
                        Component.literal("Controls when the Weekly Vault progress bar should be visible on the XP window."),
                        XPInfoDisplay.DISABLED.descriptionComponent(),
                        XPInfoDisplay.ENABLED.descriptionComponent(),
                        XPInfoDisplay.ENABLED_GAMES.descriptionComponent(),
                        XPInfoDisplay.ENABLED_LOBBY.descriptionComponent(),
                    ))
                    controller(enumSwitch<XPInfoDisplay> {
                        Component.literal(it.label)
                    })
                    binding(values::xpInfoWeeklyVault, XPInfoDisplay.ENABLED)
                }

                rootOptions.register("xp_info_disable_weekly_vault_if_max") {
                    name(Component.literal("Disable Weekly Vault if Max"))
                    description(OptionDescription.of(
                        Component.literal("If your Weekly Vault is at max claims, it will be hidden from the XP window.")
                    ))
                    controller(tickBox())
                    binding(values::xpInfoDisableWeeklyVaultIfMax, true)
                }

                rootOptions.register("xp_info_star_level") {
                    name(Component.literal("Star Level Display"))
                    description(OptionDescription.of(
                        Component.literal("Controls when the Star Level progress bar should be visible on the XP window."),
                        XPInfoDisplay.DISABLED.descriptionComponent(),
                        XPInfoDisplay.ENABLED.descriptionComponent(),
                        XPInfoDisplay.ENABLED_GAMES.descriptionComponent(),
                        XPInfoDisplay.ENABLED_LOBBY.descriptionComponent(),
                    ))
                    controller(enumSwitch<XPInfoDisplay> {
                        Component.literal(it.label)
                    })
                    binding(values::xpInfoStarLevel, XPInfoDisplay.ENABLED)
                }

                rootOptions.register("xp_info_faction") {
                    name(Component.literal("Faction Display"))
                    description(OptionDescription.of(
                        Component.literal("Controls when the Faction progress bar should be visible on the XP window."),
                        XPInfoDisplay.DISABLED.descriptionComponent(),
                        XPInfoDisplay.ENABLED.descriptionComponent(),
                        XPInfoDisplay.ENABLED_GAMES.descriptionComponent(),
                        XPInfoDisplay.ENABLED_LOBBY.descriptionComponent(),
                    ))
                    controller(enumSwitch<XPInfoDisplay> {
                        Component.literal(it.label)
                    })
                    binding(values::xpInfoFaction, XPInfoDisplay.ENABLED)
                }

                rootOptions.register("xp_info_today_xp") {
                    name(Component.literal("Today's XP Display"))
                    description(OptionDescription.of(
                        Component.literal("Controls when the Today's XP label should be visible on the XP window."),
                        XPInfoDisplay.DISABLED.descriptionComponent(),
                        XPInfoDisplay.ENABLED.descriptionComponent(),
                        XPInfoDisplay.ENABLED_GAMES.descriptionComponent(),
                        XPInfoDisplay.ENABLED_LOBBY.descriptionComponent(),
                    ))
                    controller(enumSwitch<XPInfoDisplay> {
                        Component.literal(it.label)
                    })
                    binding(values::xpInfoTodaysXP, XPInfoDisplay.ENABLED)
                }

                rootOptions.register("xp_info_game_xp") {
                    name(Component.literal("Game XP Display"))
                    description(OptionDescription.of(
                        Component.literal("Controls when the Game XP label and the XP Breakdown button should be visible on the XP window."),
                        XPInfoDisplay.DISABLED.descriptionComponent(),
                        XPInfoDisplay.ENABLED.descriptionComponent(),
                        XPInfoDisplay.ENABLED_GAMES.descriptionComponent(),
                        XPInfoDisplay.ENABLED_LOBBY.descriptionComponent(),
                    ))
                    controller(enumSwitch<XPInfoDisplay> {
                        Component.literal(it.label)
                    })
                    binding(values::xpInfoGameXP, XPInfoDisplay.ENABLED)
                }

                rootOptions.register("xp_info_navigator_today_xp") {
                    name(Component.literal("Navigator Today's XP Display"))
                    description(OptionDescription.of(
                        Component.literal("Controls whether or not your total XP per game is shown on the tooltip of games in the navigator.")
                    ))
                    controller(tickBox())
                    binding(values::xpInfoNavigatorTodayXP, true)
                }

                rootOptions.register("xp_info_navigator_today_avg_xp") {
                    name(Component.literal("Navigator Average XP/Game Today Display"))
                    description(OptionDescription.of(
                        Component.literal("Controls whether or not your average XP per game from today's stats is shown on the tooltip of games in the navigator.")
                    ))
                    controller(tickBox())
                    binding(values::xpInfoNavigatorTodayAverageXP, true)
                }

                rootOptions.register("xp_info_navigator_alltime_avg_xp") {
                    name(Component.literal("Navigator Average XP/Game All-Time Display"))
                    description(OptionDescription.of(
                        Component.literal("Controls whether or not your average XP per game from all-time stats is shown on the tooltip of games in the navigator.")
                    ))
                    controller(tickBox())
                    binding(values::xpInfoNavigatorAlltimeAverageXP, true)
                }

                rootOptions.register("xp_info_sea_monsters_energy_meter") {
                    name(Component.literal("[Sea Monsters] Energy Meter Display"))
                    description(OptionDescription.of(
                        Component.literal("Controls when the Energy Meter progress bar should be visible on the XP window."),
                        Component.literal("Note: This feature is only relevant if a Sea Monsters event is currently active."),
                        XPInfoDisplay.DISABLED.descriptionComponent(),
                        XPInfoDisplay.ENABLED.descriptionComponent(),
                        XPInfoDisplay.ENABLED_GAMES.descriptionComponent(),
                        XPInfoDisplay.ENABLED_LOBBY.descriptionComponent(),
                    ))
                    controller(enumSwitch<XPInfoDisplay> {
                        Component.literal(it.label)
                    })
                    binding(values::xpInfoSeaMonstersEnergyMeter, XPInfoDisplay.ENABLED)
                }

                rootOptions.register("xp_info_sea_monsters_disable_energy_meter_if_max") {
                    name(Component.literal("[Sea Monsters] Disable Energy Meter if Max"))
                    description(OptionDescription.of(
                        Component.literal("If your Energy Meter is at max claims, it will be hidden from the XP window."),
                                Component.literal("Note: This feature is only relevant if a Sea Monsters event is currently active."),
                    ))
                    controller(tickBox())
                    binding(values::xpInfoDisableSeaMonstersEnergyMeterIfMax, true)
                }
            }
        }.generateScreen(parent)
    }
}