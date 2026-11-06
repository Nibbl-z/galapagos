package xyz.nibblz.galapagos.ui

import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.network.chat.Style
import kotlin.math.roundToInt

// fill = % of the inner size, expand = % of whatever the other children left over
class Flow private constructor(private val vertical: Boolean) : Element() {
    val children = mutableListOf<Element>()
    private var gap = 0
    private var padding = Insets.NONE
    private var horizontalAlignment = Alignment.START
    private var verticalAlignment = Alignment.START
    private var stackLength = 0

    fun child(child: Element) = apply { children.add(child) }
    fun clearChildren() = apply { children.clear() }
    fun gap(gap: Int) = apply { this.gap = gap }
    fun padding(padding: Insets) = apply { this.padding = padding }
    fun horizontalAlignment(alignment: Alignment) = apply { horizontalAlignment = alignment }
    fun verticalAlignment(alignment: Alignment) = apply { verticalAlignment = alignment }

    private val Element.main get() = if (vertical) fullHeight else fullWidth
    private val Element.cross get() = if (vertical) fullWidth else fullHeight
    private val Element.mainSizing get() = if (vertical) verticalSizing else horizontalSizing

    override fun inflate(space: Size) {
        val available = Size(space.width - margins.horizontal, space.height - margins.vertical)
        val ownWidth = if (horizontalSizing.isContent) null else horizontalSizing.resolve(available.width) { 0 }
        val ownHeight = if (verticalSizing.isContent) null else verticalSizing.resolve(available.height) { 0 }
        val inner = Size((ownWidth ?: available.width) - padding.horizontal, (ownHeight ?: available.height) - padding.vertical)

        val stacked = children.filter { it.positioning == Positioning.Layout }
        val (expanding, regular) = stacked.partition { it.mainSizing.isExpand }
        regular.forEach { it.inflate(inner) }

        val gaps = gap * (stacked.size - 1).coerceAtLeast(0)
        val leftover = ((if (vertical) inner.height else inner.width) - regular.sumOf { it.main } - gaps).coerceAtLeast(0)
        expanding.forEach { it.inflate(if (vertical) Size(inner.width, leftover) else Size(leftover, inner.height)) }

        children.filter { it.positioning != Positioning.Layout }.forEach { it.inflate(inner) }

        stackLength = stacked.sumOf { it.main } + gaps
        val crossLength = stacked.maxOfOrNull { it.cross } ?: 0

        width = ownWidth ?: ((if (vertical) crossLength else stackLength) + padding.horizontal)
        height = ownHeight ?: ((if (vertical) stackLength else crossLength) + padding.vertical)
    }

    override fun mount(x: Int, y: Int) {
        super.mount(x, y)
        val innerX = x + padding.left
        val innerY = y + padding.top
        val innerWidth = width - padding.horizontal
        val innerHeight = height - padding.vertical

        var cursor = if (vertical) verticalAlignment.align(stackLength, innerHeight) else horizontalAlignment.align(stackLength, innerWidth)

        children.forEach { child ->
            when (val positioning = child.positioning) {
                Positioning.Layout -> {
                    if (vertical) {
                        child.mount(
                            innerX + child.margins.left + horizontalAlignment.align(child.fullWidth, innerWidth),
                            innerY + child.margins.top + cursor
                        )
                    } else {
                        child.mount(
                            innerX + child.margins.left + cursor,
                            innerY + child.margins.top + verticalAlignment.align(child.fullHeight, innerHeight)
                        )
                    }
                    cursor += child.main + gap
                }
                is Positioning.Absolute -> child.mount(
                    innerX + child.margins.left + positioning.x,
                    innerY + child.margins.top + positioning.y
                )
                is Positioning.Relative -> child.mount(
                    innerX + child.margins.left + (positioning.xPercent / 100f * (innerWidth - child.fullWidth)).roundToInt(),
                    innerY + child.margins.top + (positioning.yPercent / 100f * (innerHeight - child.fullHeight)).roundToInt()
                )
            }
        }
    }

    override fun draw(context: RenderContext) {
        children.forEach { if (it.visible && context.isVisible(it)) it.render(context) }
    }

    // panels block the mouse for anything behind them
    private inline fun <T : Any> topmost(mouseX: Number, mouseY: Number, handle: (Element) -> T?): T? {
        for (child in children.asReversed()) {
            if (!child.visible) continue
            handle(child)?.let { return it }
            if (child.surface != null && child.contains(mouseX, mouseY)) return null
        }
        return null
    }

    override fun styleAt(mouseX: Int, mouseY: Int): Style? {
        if (!contains(mouseX, mouseY)) return null
        return topmost(mouseX, mouseY) { it.styleAt(mouseX, mouseY) }
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int) =
        topmost(mouseX, mouseY) { child -> if (child.mouseClicked(mouseX, mouseY, button)) true else null } ?: false

    override fun mouseDragged(mouseX: Double, mouseY: Double) = children.any { it.mouseDragged(mouseX, mouseY) }

    override fun mouseReleased() = children.forEach { it.mouseReleased() }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double) =
        topmost(mouseX, mouseY) { child -> if (child.mouseScrolled(mouseX, mouseY, amount)) true else null } ?: false

    override fun widgets(): List<AbstractWidget> = children.flatMap { it.widgets() }

    override fun updateWidgetVisibility(parentVisible: Boolean) =
        children.forEach { it.updateWidgetVisibility(parentVisible && visible) }

    companion object {
        fun vertical(horizontal: Sizing = Sizing.content(), vertical: Sizing = Sizing.content()) = Flow(true).sizing(horizontal, vertical)
        fun horizontal(horizontal: Sizing = Sizing.content(), vertical: Sizing = Sizing.content()) = Flow(false).sizing(horizontal, vertical)
    }
}
