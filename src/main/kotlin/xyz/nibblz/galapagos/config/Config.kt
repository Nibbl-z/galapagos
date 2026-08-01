package xyz.nibblz.galapagos.config

import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler
import dev.isxander.yacl3.config.v2.api.SerialEntry
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder
import dev.isxander.yacl3.dsl.YetAnotherConfigLib
import dev.isxander.yacl3.dsl.binding
import dev.isxander.yacl3.dsl.enumSwitch
import dev.isxander.yacl3.dsl.slider
import dev.isxander.yacl3.dsl.tickBox
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

    // Coin Tracking
    @SerialEntry
    var coinTrackingEnabled: Boolean = true

    // Quest Tracking
    @SerialEntry
    var questTrackingEnabled: Boolean = true

    // Crate Chances
    @SerialEntry
    var crateChancesEnabled: Boolean = true
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
    var cosmeticMachineChancesEnabled: Boolean = true
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
    var exchangeUnitPriceEnabled: Boolean = true
    @SerialEntry
    var exchangeShowUnitPrice: Boolean = true
    @SerialEntry
    var exchangeShowSoulEquivalent: Boolean = true
    @SerialEntry
    var exchangeShowWispEquivalent: Boolean = true

    // Crafting Instructions
    @SerialEntry
    var craftingInstructionsEnabled: Boolean = true
    @SerialEntry
    var craftingInstructionsShowCraftTime: Boolean = true
    @SerialEntry
    var craftingInstructionsShowGloop: Boolean = true

    // Blueprint Assembler Info
    @SerialEntry
    var assemblerInfoEnabled: Boolean = true
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
    var weeklyVaultInfoEnabled: Boolean = true
    @SerialEntry
    var weeklyVaultInfoShowTotalProgress: Boolean = true
    @SerialEntry
    var weeklyVaultInfoShowNeededXPPerDay: Boolean = true

    // Trophy Tracking
    @SerialEntry
    var trophyTrackingEnabled: Boolean = true
    @SerialEntry
    var trophyTrackingShowTypeBreakdown: Boolean = true
    @SerialEntry
    var trophyTrackingShowCategoryBreakdown: Boolean = true

    // Average Income
    @SerialEntry
    var averageIncomeEnabled: Boolean = true
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

    // Reward Chances
    @SerialEntry
    var rewardChancesEnabled: Boolean = true

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

            categories.register("settings") {
                name(Component.literal("Settings"))

                groups.register("api") {
                    name(Component.literal("API"))

                    options.register("api_use_key") {
                        name(Component.literal("Use Own API Key"))
                        description(OptionDescription.of(
                            Component.literal("The MCC Island API is utilized in this mod to fetch cosmetic ownership, infinibag, and infinivault state."),
                            Component.empty(),
                            Component.literal("Therefore, please ensure that the Infinibag and Collections APIs are enabled in Pocket Menu->Settings->API Settings for the mod to function!"),
                            Component.empty(),
                            Component.literal("If enabled, API features will require the use of your own API key by running the command /galapagos api set <API_KEY>."),
                            Component.literal("If you do not have an API key, you can generate one at ").append(
                                Component.literal("https://gateway.noxcrew.com/.").setStyle(Style.EMPTY
                                    .withUnderlined(true)
                                    .withColor(ChatFormatting.AQUA.color!!)
                                )
                            ),
                            Component.empty(),
                            Component.literal("Using your own API key is preferred, as you can make requests much more often."),
                            Component.empty(),
                            Component.literal("If you are unable to supply your own API key, a different endpoint will be used, making API calls with the developer's own API key. This means you can make less requests per minute, and uptime of this backend is not guaranteed.")
                        ))
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
                            description(OptionDescription.createBuilder()
                                .text(feature.description)
                                .image(Identifier.fromNamespaceAndPath("galapagos", "textures/config/${feature.image.path}"), feature.image.w, feature.image.h)
                                .build()
                            )
                            controller(tickBox())
                            binding(feature.enabledProperty, true)
                        }
                    }
                }

                groups.register("crate_chances") {
                    name(Component.literal("Crate Chances"))

                    options.register("crate_chances_highlight_rep") {
                        name(Component.literal("Highlight Best Rep Chance"))
                        description(OptionDescription.of(
                            Component.literal("Highlights the standard and exclusive crates with the highest chance for new royal reputation.")
                        ))
                        controller(tickBox())
                        binding(values::highlightBestRepChance, true)
                    }

                    options.register("crate_chances_highlight_cosmetic") {
                        name(Component.literal("Highlight Best Cosmetic Chance"))
                        description(OptionDescription.of(
                            Component.literal("Highlights the standard and exclusive crates with the highest chance for a new cosmetic.")
                        ))
                        controller(tickBox())
                        binding(values::highlightBestCosmeticChance, true)
                    }

                    options.register("crate_chances_show_cosmetic_chance") {
                        name(Component.literal("Show New Cosmetic Chance"))
                        description(OptionDescription.of(
                            Component.literal("Shows the percent chance that the crate will give a new cosmetic.")
                        ))
                        controller(tickBox())
                        binding(values::showNewCosmeticChance, true)
                    }

                    options.register("crate_chances_show_rep_chance") {
                        name(Component.literal("Show New Royal Reputation Chance"))
                        description(OptionDescription.of(
                            Component.literal("Shows the percent chance that the crate will give new royal reputation.")
                        ))
                        controller(tickBox())
                        binding(values::showNewRepChance, true)
                    }

                    options.register("crate_chances_show_trophies_per_roll") {
                        name(Component.literal("Show Trophies per Roll"))
                        description(OptionDescription.of(
                            Component.literal("Shows the average amount of trophies you will earn from opening a crate, including the trophies from new cosmetics and royal reputation.")
                        ))
                        controller(tickBox())
                        binding(values::showTrophiesPerRoll, true)
                    }

                    options.register("crate_chances_show_mythic_cores_per_roll") {
                        name(Component.literal("Show Mythic Cores per Roll"))
                        description(OptionDescription.of(
                            Component.literal("Shows the average amount of mythic cores you will earn from opening a crate. If the crate is exclusive, this is the mythic cores you will earn from scavenging any earned arcane cores.")
                        ))
                        controller(tickBox())
                        binding(values::showMythicCoresPerRoll, true)
                    }

                    options.register("crate_chances_show_arcane_cores_per_roll") {
                        name(Component.literal("Show Arcane Cores per Roll"))
                        description(OptionDescription.of(
                            Component.literal("Shows the average amount of arcane cores you will earn from opening a crate. If the crate is standard, this is the arcane cores you will earn from upcrafting any earned mythic cores.")
                        ))
                        controller(tickBox())
                        binding(values::showArcaneCoresPerRoll, true)
                    }

                    options.register("crate_chances_show_max_cosmetic_crates") {
                        name(Component.literal("Maxed Cosmetic Crate Icon"))
                        description(OptionDescription.of(
                            Component.literal("Shows a style trophy icon in the corner of crates with all cosmetics earned.")
                        ))
                        controller(tickBox())
                        binding(values::showMaxCosmeticCrates, true)
                    }

                    options.register("crate_chances_show_max_rep_crates") {
                        name(Component.literal("Maxed Royal Reputation Crate Icon"))
                        description(OptionDescription.of(
                            Component.literal("Shows a royal reputation icon in the corner of crates with all royal reputation earned.")
                        ))
                        controller(tickBox())
                        binding(values::showMaxRepCrates, true)
                    }
                }

                groups.register("cosmetic_machine") {
                    name(Component.literal("Cosmetic Machine"))

                    options.register("cosmetic_machine_detailed_chances") {
                        name(Component.literal("Detailed Chances"))
                        description(OptionDescription.createBuilder()
                            .text(Component.literal("Shows the specific chance for non-exclusive, exclusive, and arcane pulls per rarity in the tooltips of the pull buttons."))
                            .image(Identifier.fromNamespaceAndPath("galapagos", "textures/config/detailed_cosmetic_machine.png"), 400, 427)
                            .build()
                        )
                        controller(tickBox())
                        binding(values::detailedCosmeticMachineChances, true)
                    }

                    options.register("cosmetic_machine_show_cosmetic_chance") {
                        name(Component.literal("Show New Cosmetic Chance"))
                        description(OptionDescription.of(
                            Component.literal("Shows the percent chance that the pull will give a new cosmetic.")
                        ))
                        controller(tickBox())
                        binding(values::showNewCosmeticChancePerPull, true)
                    }

                    options.register("cosmetic_machine_show_rep_chance") {
                        name(Component.literal("Show New Royal Reputation Chance"))
                        description(OptionDescription.of(
                            Component.literal("Shows the percent chance that the pull will give new royal reputation.")
                        ))
                        controller(tickBox())
                        binding(values::showNewRepChancePerPull, true)
                    }

                    options.register("cosmetic_machine_show_trophies_per_roll") {
                        name(Component.literal("Show Trophies per Pull"))
                        description(OptionDescription.of(
                            Component.literal("Shows the average amount of trophies you will earn from pulling a key, including the trophies from new cosmetics and royal reputation.")
                        ))
                        controller(tickBox())
                        binding(values::showTrophiesPerPull, true)
                    }

                    options.register("cosmetic_machine_show_mythic_cores_per_roll") {
                        name(Component.literal("Show Mythic Cores per Pull"))
                        description(OptionDescription.of(
                            Component.literal("Shows the average amount of mythic cores you will earn from pulling a key. This includes mythic cores you will earn from scavenging any earned arcane cores.")
                        ))
                        controller(tickBox())
                        binding(values::showMythicCoresPerPull, true)
                    }

                    options.register("cosmetic_machine_show_arcane_cores_per_roll") {
                        name(Component.literal("Show Arcane Cores per Pull"))
                        description(OptionDescription.of(
                            Component.literal("Shows the average amount of arcane cores you will earn from pulling a key. This includesarcane cores you will earn from upcrafting any earned mythic cores.")
                        ))
                        controller(tickBox())
                        binding(values::showArcaneCoresPerPull, true)
                    }
                }

                groups.register("island_exchange") {
                    name(Component.literal("Island Exchange"))

                    options.register("island_exchange_unit_price") {
                        name(Component.literal("Show Listing Unit Price"))
                        description(OptionDescription.of(
                            Component.literal("If a listing on Island Exchange contains multiple of one item, the price per unit will show in the tooltip.")
                        ))
                        controller(tickBox())
                        binding(values::exchangeShowUnitPrice, true)
                    }

                    options.register("island_exchange_soul_equivalent") {
                        name(Component.literal("Show Style Soul Equivalent"))
                        description(OptionDescription.of(
                            Component.literal("Shows the equivalent of a cosmetic listing on Island Exchange in style souls if scavenged.")
                        ))
                        controller(tickBox())
                        binding(values::exchangeShowSoulEquivalent, true)
                    }

                    options.register("island_exchange_wisp_equivalent") {
                        name(Component.literal("Show Weapon Wisp Equivalent"))
                        description(OptionDescription.of(
                            Component.literal("Shows the equivalent of a weapon skin listing on Island Exchange in weapon wisps if scavenged.")
                        ))
                        controller(tickBox())
                        binding(values::exchangeShowWispEquivalent, true)
                    }
                }

                groups.register("crafting_instructions") {
                    name(Component.literal("Crafting Instructions"))

                    options.register("crafting_instructions_show_time") {
                        name(Component.literal("Show Crafting Time"))
                        description(OptionDescription.of(
                            Component.literal("Shows crafting time for items that need to be crafted, as well as total craft time, in the list of instructions.")
                        ))
                        controller(tickBox())
                        binding(values::craftingInstructionsShowCraftTime, true)
                    }

                    options.register("crafting_instructions_show_gloop") {
                        name(Component.literal("Show Material Gloop"))
                        description(OptionDescription.of(
                            Component.literal("Shows material gloop cost for items that need to be purchased from the material market, as well as total gloop cost, in the list of instructions.")
                        ))
                        controller(tickBox())
                        binding(values::craftingInstructionsShowGloop, true)
                    }
                }

                groups.register("assembler_info") {
                    name(Component.literal("Blueprint Assembler Info"))

                    options.register("assembler_show_new_trophies") {
                        name(Component.literal("Show New Trophies"))
                        description(OptionDescription.of(
                            Component.literal("Shows the total style trophies earnable from new cosmetic blueprints.")
                        ))
                        controller(tickBox())
                        binding(values::assemblerInfoShowNewTrophies, true)
                    }

                    options.register("assembler_show_new_rep") {
                        name(Component.literal("Show New Royal Reputation"))
                        description(OptionDescription.of(
                            Component.literal("Shows the total royal reputation earnable from blueprints.")
                        ))
                        controller(tickBox())
                        binding(values::assemblerInfoShowNewRep, true)
                    }

                    options.register("assembler_show_standard_cores") {
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

                    options.register("assembler_show_exclusive_cores") {
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

                    options.register("assembler_show_mythic_cores") {
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

                    options.register("assembler_show_arcane_cores") {
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

                groups.register("weekly_vault_info") {
                    name(Component.literal("Weekly Vault Info"))

                    options.register("weekly_vault_info_show_total_progress") {
                        name(Component.literal("Show Total Progress"))
                        description(OptionDescription.of(
                            Component.literal("Shows the total XP you've earned towards reaching max claims on your weekly vault.")
                        ))
                        controller(tickBox())
                        binding(values::weeklyVaultInfoShowTotalProgress, true)
                    }

                    options.register("weekly_vault_info_show_needed_xp_per_day") {
                        name(Component.literal("Show Needed XP Per Day"))
                        description(OptionDescription.of(
                            Component.literal("Shows an average amount of XP to earn each day in order to reach max claims on your weekly vault.")
                        ))
                        controller(tickBox())
                        binding(values::weeklyVaultInfoShowNeededXPPerDay, true)
                    }
                }

                groups.register("trophy_tracking") {
                    name(Component.literal("Trophy Tracking"))

                    options.register("trophy_tracking_show_type_breakdown") {
                        name(Component.literal("Show Trophy Type Breakdown"))
                        description(OptionDescription.of(
                            Component.literal("For each day listed, shows a breakdown of how many of each type of trophy is obtained, including Skill, Style, and Angler.")
                        ))
                        controller(tickBox())
                        binding(values::trophyTrackingShowTypeBreakdown, true)
                    }

                    options.register("trophy_tracking_show_category_breakdown") {
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

                groups.register("average_income") {
                    name(Component.literal("Average Income"))

                    options.register("average_income_include_scrolls") {
                        name(Component.literal("Include Quest Scrolls alongside Dailies"))
                        description(OptionDescription.of(
                            Component.literal("Adds another line under any average income including daily quests, making the assumption that you will complete whatever your current highest rarity of quest scroll is alongside the daily quest.")
                        ))
                        controller(tickBox())
                        binding(values::averageIncomeIncludeQuestScrolls, true)
                    }

                    options.register("average_income_dailies") {
                        name(Component.literal("Average Daily Quests/Day"))
                        description(OptionDescription.of(
                            Component.literal("Set here how many daily quests you think you will complete on average every day."),
                            Component.empty(),
                            Component.literal("This value will be clamped to whatever your actual max daily quest count is.")
                        ))
                        controller(slider(0..10))
                        binding(values::averageIncomeDailies, 10)
                    }

                    options.register("average_income_weeklies") {
                        name(Component.literal("Average Weekly Quests/Week"))
                        description(OptionDescription.of(
                            Component.literal("Set here how many weekly quests you think you will complete on average every week."),
                            Component.empty(),
                            Component.literal("This value will be clamped to whatever your actual max weekly quest count is.")
                        ))
                        controller(slider(0..10))
                        binding(values::averageIncomeWeeklies, 10)
                    }

                    options.register("average_income_meters") {
                        name(Component.literal("Average Daily Meter Claims/Day"))
                        description(OptionDescription.of(
                            Component.literal("Set here how many daily meter claims you think you will complete on average every day."),
                            Component.empty(),
                            Component.literal("This value will be clamped to whatever your actual max daily meter claims are.")
                        ))
                        controller(slider(0..15))
                        binding(values::averageIncomeMeters, 15)
                    }

                    options.register("average_income_vault_claims") {
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
        }.generateScreen(parent)
    }
}