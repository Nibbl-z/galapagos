package xyz.nibblz.galapagos.util

import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.config.Config
import xyz.nibblz.galapagos.screens.CoinHistory
import xyz.nibblz.galapagos.screens.Intro
import xyz.nibblz.galapagos.screens.QuestHistory

object GalapagosCommand {
    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        Command("galapagos") {
            literal("api") {
                literal("set") {
                    argument("key") {
                        executes {
                            val key = it.getArgument("key", String::class.java)
                            Galapagos.save.apiKey = key

                            sendGalapagosChatMessage(Component.literal("API key has been set!").withColor(ChatFormatting.AQUA.color!!))
                            Config.values::usePersonalApiKey.set(true)

                            if (OOBE.state == OOBE.OOBEState.SET_API_KEY) {
                                sendGalapagosChatMessage(Component.literal("If you just enabled your API settings, you may have to try again in a few minutes by running /galapagos api manualFetch!").withColor(ChatFormatting.BLUE.color!!))
                                Galapagos.save.finishedOOBE = true
                                OOBE.active = false
                            }

                            val status = PlayerData.fetchAPI()
                            if (status) sendGalapagosChatMessage(Component.literal("Successfully updated player state from API!").withColor(ChatFormatting.AQUA.color!!))
                        }
                    }
                }

                literal("reset") {
                    executes {
                        Galapagos.save.apiKey = ""
                        Config.values::usePersonalApiKey.set(false)

                        sendGalapagosChatMessage(Component.literal("API key has been removed! The custom API endpoint will now be used instead.").withColor(ChatFormatting.AQUA.color!!))
                    }
                }

                literal("manualFetch") {
                    executes {
                        sendGalapagosChatMessage(Component.literal("Fetching MCC Island API...").withColor(ChatFormatting.AQUA.color!!))
                        val status = PlayerData.fetchAPI()
                        if (status) sendGalapagosChatMessage(Component.literal("Successfully updated player state from API!").withColor(ChatFormatting.AQUA.color!!))
                    }
                }
            }

            literal("debug") {
                literal("fusionforge") {
                    executes {
                        Minecraft.getInstance().gui.chat.addClientSystemMessage(
                            Component.literal(Galapagos.save.fusionForge.toString())
                        )
                    }
                }

                literal("coinhistory") {
                    executes {
                        Minecraft.getInstance().execute {
                            Minecraft.getInstance().setScreen(CoinHistory())
                        }
                    }
                }

                literal("intro_test") {
                    executes {
                        Minecraft.getInstance().execute {
                            Minecraft.getInstance().setScreen(Intro())
                        }
                    }
                }

                literal("questhistory") {
                    executes {
                        Minecraft.getInstance().execute {
                            Minecraft.getInstance().setScreen(QuestHistory())
                        }
                    }
                }

                literal("cosmetic") {
                    argument("name") {
                        suggests { _, builder ->
                            Galapagos.save.cosmetics.keys.forEach { builder.suggest(it) }
                            builder.buildFuture()
                        }

                        executes {
                            val nameArg = it.getArgument("name", String::class.java)
                            if (Galapagos.save.cosmetics[nameArg] == null) return@executes

                            Minecraft.getInstance().gui.chat.addClientSystemMessage(
                                Component.literal(Galapagos.save.cosmetics[nameArg]!!.toString())
                            )
                        }
                    }
                }

                literal("bagitem") {
                    argument("name") {
                        suggests { _, builder ->
                            Galapagos.save.infinibag.keys.forEach { builder.suggest(it) }
                            builder.buildFuture()
                        }

                        executes {
                            val nameArg = it.getArgument("name", String::class.java)
                            if (Galapagos.save.infinibag[nameArg] == null) return@executes

                            Minecraft.getInstance().gui.chat.addClientSystemMessage(
                                Component.literal(Galapagos.save.infinibag[nameArg]!!.toString())
                            )
                        }
                    }
                }
            }
        }.register(dispatcher)
    }
}