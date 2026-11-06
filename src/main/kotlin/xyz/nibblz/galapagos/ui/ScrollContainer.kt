package xyz.nibblz.galapagos.ui

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.network.chat.Style
import net.minecraft.util.ARGB
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

class ScrollContainer(horizontal: Sizing, vertical: Sizing, private val child: Element) : Element() {
    private var scrollbarThickness = 3
    private var scrollbarColor = -1
    private var scrollStep = 20

    private var offset = 0.0
    private var targetOffset = 0.0
    private var maxOffset = 0
    private var draggingScrollbar = false
    private var grabOffset = 0.0
    private var lastFrame = System.nanoTime()
    private var scrollbarVisibleUntil = 0L

    init {
        sizing(horizontal, vertical)
    }

    fun scrollbarThickness(thickness: Int) = apply { scrollbarThickness = thickness }
    fun scrollbarColor(argb: Int) = apply { scrollbarColor = argb }
    fun scrollStep(step: Int) = apply { scrollStep = step }

    override fun inflate(space: Size) {
        // only needed for content sizing
        if (horizontalSizing.isContent || verticalSizing.isContent) {
            child.inflate(Size(space.width - margins.horizontal - scrollbarThickness, space.height - margins.vertical))
        }
        super.inflate(space)
        child.inflate(Size(width - scrollbarThickness, height))

        maxOffset = (child.fullHeight - height).coerceAtLeast(0)
        targetOffset = targetOffset.coerceIn(0.0, maxOffset.toDouble())
        offset = offset.coerceIn(0.0, maxOffset.toDouble())
    }

    override fun contentWidth() = child.fullWidth + scrollbarThickness
    override fun contentHeight() = child.fullHeight

    override fun mount(x: Int, y: Int) {
        super.mount(x, y)
        child.mount(x + child.margins.left, y + child.margins.top)
    }

    private val scrolled get() = offset.roundToInt()
    private val scrollbarLength get() = (height * height / child.fullHeight.coerceAtLeast(1)).coerceAtLeast(10).coerceAtMost(height)
    private val scrollbarX get() = x + width - scrollbarThickness
    private val scrollbarY get() = y + ((height - scrollbarLength) * (offset / maxOffset.coerceAtLeast(1))).roundToInt()

    // the bar is tiny so make it easier to grab
    private fun overScrollbar(mouseX: Number, mouseY: Number) =
        maxOffset > 0 && contains(mouseX, mouseY) && mouseX.toDouble() >= x + width - scrollbarThickness.coerceAtLeast(SCROLLBAR_GRAB_WIDTH)

    override fun draw(context: RenderContext) {
        val now = System.nanoTime()
        offset += (targetOffset - offset) * (1 - exp(-(now - lastFrame) / 1e9 * 20))
        if (abs(targetOffset - offset) < 0.5) offset = targetOffset
        lastFrame = now

        val graphics = context.graphics
        val mouseInside = contains(context.mouseX, context.mouseY)
        if (draggingScrollbar || overScrollbar(context.mouseX, context.mouseY)) showScrollbar()

        graphics.enableScissor(x, y, x + width, y + height)
        graphics.pose().pushMatrix()
        graphics.pose().translate(0f, -scrolled.toFloat())
        child.render(RenderContext(
            graphics,
            if (mouseInside) context.mouseX else Int.MIN_VALUE,
            if (mouseInside) context.mouseY + scrolled else Int.MIN_VALUE,
            x, y + scrolled, width, height
        ))
        graphics.pose().popMatrix()
        graphics.disableScissor()

        if (maxOffset == 0) return
        val fade = ((scrollbarVisibleUntil - System.currentTimeMillis()).coerceIn(0, 750) / 750f)
        if (fade <= 0f) return

        val color = ARGB.color((ARGB.alpha(scrollbarColor) * fade).roundToInt(), scrollbarColor)
        graphics.fill(scrollbarX, scrollbarY, scrollbarX + scrollbarThickness, scrollbarY + scrollbarLength, color)
    }

    private fun showScrollbar() {
        scrollbarVisibleUntil = System.currentTimeMillis() + 1500
    }

    private fun dragScrollbarTo(mouseY: Double) {
        val progress = (mouseY - grabOffset - y) / (height - scrollbarLength).coerceAtLeast(1)
        targetOffset = (progress * maxOffset).coerceIn(0.0, maxOffset.toDouble())
        offset = targetOffset
        showScrollbar()
    }

    override fun styleAt(mouseX: Int, mouseY: Int): Style? {
        if (!contains(mouseX, mouseY)) return null
        return child.styleAt(mouseX, mouseY + scrolled)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!contains(mouseX, mouseY)) return false
        if (button == InputConstants.MOUSE_BUTTON_LEFT && overScrollbar(mouseX, mouseY)) {
            // clicking the track centers the bar on the mouse
            val relative = mouseY - scrollbarY
            grabOffset = if (relative in 0.0..scrollbarLength.toDouble()) relative else scrollbarLength / 2.0
            draggingScrollbar = true
            dragScrollbarTo(mouseY)
            return true
        }
        return child.mouseClicked(mouseX, mouseY + scrolled, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double): Boolean {
        if (!draggingScrollbar) return false
        dragScrollbarTo(mouseY)
        return true
    }

    override fun mouseReleased() {
        draggingScrollbar = false
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double): Boolean {
        if (!contains(mouseX, mouseY)) return false
        if (child.mouseScrolled(mouseX, mouseY + scrolled, amount)) return true
        if (maxOffset == 0) return false

        targetOffset = (targetOffset - amount * scrollStep).coerceIn(0.0, maxOffset.toDouble())
        showScrollbar()
        return true
    }

    companion object {
        private const val SCROLLBAR_GRAB_WIDTH = 8
    }
}
