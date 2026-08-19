package xyz.nibblz.galapagos.screens

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.*
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import xyz.nibblz.galapagos.Galapagos
import kotlin.time.Instant

class VaultHistory : BaseOwoScreen<FlowLayout>() {
    override fun createAdapter(): OwoUIAdapter<FlowLayout?> {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow)
    }

    fun updateContent(content: FlowLayout) {
        val sortedVaults = Galapagos.save.weeklyVaultHistory.sortedByDescending { it.timestamp }
        var currentVault = sortedVaults.size

        content.child(UIComponents.spacer().verticalSizing(Sizing.fixed(5   )))

        sortedVaults.forEach {
            val date = Instant.fromEpochSeconds(it.timestamp).toLocalDateTime(TimeZone.currentSystemDefault())

            val changeContainer = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content())
            changeContainer.gap(5)
            changeContainer.verticalAlignment(VerticalAlignment.CENTER)

            changeContainer.child(UIComponents.label(
                Component.literal("Vault #$currentVault")
                    .append(Component.literal(" (${it.claims}/${it.maxClaims})").withColor(ChatFormatting.GRAY.color!!))
            ))

            changeContainer.child(UIComponents.label(
                Component.literal("Claimed ${date.month.name.lowercase().replaceFirstChar { char -> char.uppercase() }} ${date.day}, ${date.year}")
                        .withColor(ChatFormatting.GRAY.color!!)
            ))

            val rewardLayout = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content())

            it.rewards.keys.sortedBy { rarity -> rarity.ordinal }.forEach { rarity ->
                val count = it.rewards[rarity]
                rewardLayout.child(UIContainers.verticalFlow(Sizing.fill(20), Sizing.content())
                    .child(UIComponents.texture(
                        Identifier.fromNamespaceAndPath("mcc", "textures/island_items/infinibag/openable/questing_crate_${rarity.name.lowercase()}.png"),
                        0, 0, 16, 16, 16, 16
                    ))
                    .child(UIComponents.label(Component.literal("x$count"))
                        .horizontalTextAlignment(HorizontalAlignment.CENTER))
                    .gap(3)
                    .horizontalAlignment(HorizontalAlignment.CENTER)
                )
            }

            if (it.anomalies > 0) {
                rewardLayout.child(UIContainers.verticalFlow(Sizing.fill(20), Sizing.content())
                    .child(UIComponents.texture(
                        Identifier.fromNamespaceAndPath("mcc", "textures/island_items/infinibag/openable/arcane_anomaly.png"),
                        0, 48, 16, 16, 16, 160
                    )) // its not animated, too bad, so sad, theres been like 3 arcane anomalies from vaults in total i dont want to implement my own animation extension to this
                    .child(UIComponents.label(Component.literal("x${it.anomalies}"))
                        .horizontalTextAlignment(HorizontalAlignment.CENTER))
                    .gap(3)
                    .horizontalAlignment(HorizontalAlignment.CENTER)
                )
            }

            changeContainer.child(rewardLayout)

            changeContainer.padding(Insets.of(4))
            changeContainer.surface(Surface.VANILLA_TRANSLUCENT)

            content.child(changeContainer)
            currentVault--
        }

        content.child(UIComponents.spacer().verticalSizing(Sizing.fixed(10)))
    }

    override fun build(rootComponent: FlowLayout) {
        rootComponent
            .surface(Surface.VANILLA_TRANSLUCENT)
            .horizontalAlignment(HorizontalAlignment.CENTER)
            .verticalAlignment(VerticalAlignment.CENTER)

        val content = UIContainers.verticalFlow(Sizing.content(), Sizing.content())
        content.padding(Insets.of(5))
        content.gap(3)

        updateContent(content)

        setupMcciScreen(rootComponent, content, "VAULT HISTORY", "textures/island_interface/quest_log/meters/daily_vault_full.png")
        // well of course, the Daily Vault.
    }
}