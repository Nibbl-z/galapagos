package xyz.nibblz.galapagos.util

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import java.util.concurrent.CompletableFuture

// stealing from devcmb stealing from pe3ep part 2
// because dear god i dont understand this stuff I JUST WANT DEBUG COMMANDS!!!
// https://github.com/pe3ep/Trident/blob/master/src/main/kotlin/cc/pe3epwithyou/trident/utils/Command.kt
// as devcmb says, "true lifesaver for my tired ass"
// as i, nibbles, say, "true lifesaver for my very awake ass"

/**
 * Simple DSL to create commands
 *
 * Example usage:
 * ```kt
 * // Simple command without arguments
 * val simpleCommand = Command("simple") {
 *   executes {
 *     // ...
 *   }
 * }
 *
 * // Command with arguments
 * val commandWithArgs = Command("foo") {
 *   argument("bar") {
 *     executes {
 *       val arg = it.getArgument("bar", String::class.java)
 *       // ...
 *     }
 *   }
 * }
 * ```
 */
@Suppress("unused")
class Command(
    name: String, block: Builder.() -> Unit
) {
    private val root: LiteralArgumentBuilder<FabricClientCommandSource> =
        ClientCommands.literal(name)

    init {
        Builder(root).block()
    }

    fun build(): LiteralArgumentBuilder<FabricClientCommandSource> = root

    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(root)
    }

    class Builder(
        private val node: LiteralArgumentBuilder<FabricClientCommandSource>
    ) {

        /** Context variant: executes { ctx -> ... } */
        fun executes(block: (CommandContext<FabricClientCommandSource>) -> Unit) {
            node.executes { ctx ->
                block(ctx)
                0
            }
        }

        fun literal(
            name: String, block: Builder.() -> Unit
        ) {
            val literal = ClientCommands.literal(name)
            Builder(literal).block()
            node.then(literal)
        }

        fun <T : Any> argument(
            name: String, type: ArgumentType<T>, block: ArgumentBuilder<T>.() -> Unit
        ) {
            val argNode = ClientCommands.argument(name, type)
            ArgumentBuilder(argNode).block()
            node.then(argNode)
        }

        /** Convenience: string argument */
        fun argument(
            name: String, block: ArgumentBuilder<String>.() -> Unit
        ) {
            argument(name, StringArgumentType.string(), block)
        }
    }

    class ArgumentBuilder<T>(
        private val node: RequiredArgumentBuilder<FabricClientCommandSource, T>
    ) {
        fun executes(block: (CommandContext<FabricClientCommandSource>) -> Unit) {
            node.executes { ctx ->
                block(ctx)
                0
            }
        }

        fun suggests(
            provider: (CommandContext<FabricClientCommandSource>, SuggestionsBuilder) -> CompletableFuture<Suggestions>
        ) {
            node.suggests(provider)
        }

        fun literal(
            name: String, block: Builder.() -> Unit
        ) {
            val literal = ClientCommands.literal(name)
            Builder(literal).block()
            node.then(literal)
        }

        fun <U : Any> argument(
            name: String, type: ArgumentType<U>, block: ArgumentBuilder<U>.() -> Unit
        ) {
            val argNode = ClientCommands.argument(name, type)
            ArgumentBuilder(argNode).block()
            node.then(argNode)
        }

        fun argument(
            name: String, block: ArgumentBuilder<String>.() -> Unit
        ) {
            argument(name, StringArgumentType.string(), block)
        }
    }
}