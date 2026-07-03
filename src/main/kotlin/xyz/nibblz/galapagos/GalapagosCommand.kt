package xyz.nibblz.galapagos

import com.mojang.brigadier.CommandDispatcher
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

object GalapagosCommand {
    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        Command("galapagos") {
            literal("debug") {
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