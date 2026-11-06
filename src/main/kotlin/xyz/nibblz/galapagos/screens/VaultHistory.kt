package xyz.nibblz.galapagos.screens

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.Identifier
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.ui.*
import kotlin.time.Instant

class VaultHistory : LayoutScreen() {
    fun updateContent(content: Flow) {
        val sortedVaults = Galapagos.save.weeklyVaultHistory.sortedByDescending { it.timestamp }
        var currentVault = sortedVaults.size

        content.child(Spacer(vertical = Sizing.fixed(5)))

        sortedVaults.forEach {
            val date = Instant.fromEpochSeconds(it.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())

            val changeContainer = Flow.vertical(Sizing.fill(100), Sizing.content())
            changeContainer.gap(5)
            changeContainer.verticalAlignment(Alignment.CENTER)

            changeContainer.child(Label(
                Component.literal("Vault #$currentVault")
                    .append(Component.literal(" (${it.claims}/${it.maxClaims})").withColor(TextColor.GRAY.value))
            ))

            changeContainer.child(Label(
                Component.literal("Claimed ${date.month.name.lowercase().replaceFirstChar { char -> char.uppercase() }} ${date.day}, ${date.year}")
                        .withColor(TextColor.GRAY.value)
            ))

            val rewardLayout = Flow.horizontal(Sizing.fill(100), Sizing.content())

            it.rewards.keys.sortedBy { rarity -> rarity.ordinal }.forEach { rarity ->
                val count = it.rewards[rarity]
                rewardLayout.child(Flow.vertical(Sizing.fill(20), Sizing.content())
                    .child(Texture(
                        Identifier.fromNamespaceAndPath("mcc", "textures/island_items/infinibag/openable/questing_crate_${rarity.name.lowercase()}.png"),
                        0, 0, 16, 16, 16, 16
                    ))
                    .child(Label(Component.literal("x$count"))
                        .horizontalTextAlignment(Alignment.CENTER))
                    .gap(3)
                    .horizontalAlignment(Alignment.CENTER)
                )
            }

            if (it.anomalies > 0) {
                rewardLayout.child(Flow.vertical(Sizing.fill(20), Sizing.content())
                    .child(Texture(
                        Identifier.fromNamespaceAndPath("mcc", "textures/island_items/infinibag/openable/arcane_anomaly.png"),
                        0, 48, 16, 16, 16, 160
                    )) // its not animated, too bad, so sad, theres been like 3 arcane anomalies from vaults in total i dont want to implement my own animation extension to this
                    .child(Label(Component.literal("x${it.anomalies}"))
                        .horizontalTextAlignment(Alignment.CENTER))
                    .gap(3)
                    .horizontalAlignment(Alignment.CENTER)
                )
            }

            changeContainer.child(rewardLayout)

            changeContainer.padding(Insets.all(4))
            changeContainer.surface(Surface.VANILLA_TRANSLUCENT)

            content.child(changeContainer)
            currentVault--
        }

        content.child(Spacer(vertical = Sizing.fixed(10)))
    }

    override fun build(rootComponent: Flow) {
        rootComponent
            .horizontalAlignment(Alignment.CENTER)
            .verticalAlignment(Alignment.CENTER)

        val content = Flow.vertical(Sizing.content(), Sizing.content())
        content.padding(Insets.all(5))
        content.gap(3)

        updateContent(content)

        setupMcciScreen(rootComponent, content, "VAULT HISTORY", "textures/island_interface/quest_log/meters/daily_vault_full.png")
        // well of course, the Daily Vault.
    }
}