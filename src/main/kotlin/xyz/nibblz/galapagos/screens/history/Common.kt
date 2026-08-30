package xyz.nibblz.galapagos.screens.history

import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.Color
import io.wispforest.owo.ui.core.Sizing
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.data.game.DeathCause
import xyz.nibblz.galapagos.util.Glyphs
import xyz.nibblz.galapagos.util.mcciTextureComponent
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.forEach

val graphColors: List<Int> = listOf(
    0x77ff6f,
    0x6fef67,
    0x67df5f,
    0x5fcf57,
    0x5bbf4f,
    0x53af47,
    0x4b9f3f,
    0x439337,
    0x3f832f,
    0x37732b,
    0x2f6323,
    0x27531b,
    0x1f4317,
    0x17330f,
    0x13230b,
    0x0b1707
)

fun createCauseGraph(data: HashMap<DeathCause, Int>, label: String): FlowLayout {
    val percents: HashMap<DeathCause, Double> = hashMapOf()
    val sum = data.values.sumOf { it }

    data.forEach { (cause, count) ->
        percents[cause] = (count.toDouble() / sum.toDouble())
    }

    val sorted = percents.entries.sortedByDescending { it.value }

    val mult = graphColors.size / sorted.size

    val graph = UIContainers.horizontalFlow(Sizing.expand(), Sizing.fixed(10))
    sorted.forEachIndexed { index, (_, percent) ->
        graph.child(
            UIComponents.box(Sizing.fill((percent * 100).toInt()), Sizing.fill())
                .fill(true)
                .color(Color.ofRgb(graphColors.getOrNull(index * mult) ?: 0xFFFFFF))
        )
    }

    val breakdownComponent = with(Component.empty()) {
        sorted.forEachIndexed { index, (cause, _) ->
            if (cause.sprite.contains("_fonts")) append(Glyphs.getGlyphComponent("${cause.sprite}.png"))
            else if (cause.sprite.contains("/")) append(mcciTextureComponent(cause.sprite))
            else append(Component.literal(cause.sprite).withColor(0xffffff).withStyle(Style.EMPTY.withFont(Galapagos.font).withHoverEvent(
                HoverEvent.ShowText(Component.literal(cause.label))
            )))
            append(Component.literal(" ${data[cause]} ").withColor(graphColors.getOrNull(index * mult) ?: 0xFFFFFF).withStyle(Style.EMPTY.withHoverEvent(
                HoverEvent.ShowText(Component.literal(cause.label))
            )))
        }

        append(Component.empty()) // goog
    }

    val root = UIContainers.verticalFlow(Sizing.fill(), Sizing.content())
        .child(UIContainers.horizontalFlow(Sizing.fill(), Sizing.fixed(10))
            .child(UIComponents.label(Component.literal(label)).shadow(true).horizontalSizing(Sizing.fill(15)))
            .child(graph))
        .child(UIComponents.label(breakdownComponent).shadow(true))
        .gap(2)


    return root
}