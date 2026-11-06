package xyz.nibblz.galapagos.ui

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ActiveTextCollector
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.state.gui.GuiTextRenderState
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import net.minecraft.util.FormattedCharSequence
import org.joml.Matrix3x2f

abstract class Element {
    var horizontalSizing: Sizing = Sizing.content()
    var verticalSizing: Sizing = Sizing.content()
    var margins: Insets = Insets.NONE
    var positioning: Positioning = Positioning.Layout
    var surface: Surface? = null
    var visible = true

    var x = 0
        private set
    var y = 0
        private set
    var width = 0
        protected set
    var height = 0
        protected set

    val fullWidth get() = width + margins.horizontal
    val fullHeight get() = height + margins.vertical

    open fun inflate(space: Size) {
        width = horizontalSizing.resolve(space.width - margins.horizontal) { contentWidth() }
        height = verticalSizing.resolve(space.height - margins.vertical) { contentHeight() }
    }

    protected open fun contentWidth() = 0
    protected open fun contentHeight() = 0

    open fun mount(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    fun render(context: RenderContext) {
        surface?.draw(context.graphics, this)
        draw(context)
    }

    protected abstract fun draw(context: RenderContext)

    fun contains(mouseX: Number, mouseY: Number): Boolean {
        val mx = mouseX.toDouble()
        val my = mouseY.toDouble()
        return mx >= x && mx < x + width && my >= y && my < y + height
    }

    open fun styleAt(mouseX: Int, mouseY: Int): Style? = null

    open fun mouseClicked(mouseX: Double, mouseY: Double, button: Int) = false
    open fun mouseDragged(mouseX: Double, mouseY: Double) = false
    open fun mouseReleased() {}
    open fun mouseScrolled(mouseX: Double, mouseY: Double, amount: Double) = false

    open fun widgets(): List<AbstractWidget> = emptyList()

    open fun updateWidgetVisibility(parentVisible: Boolean) {}
}

fun <T : Element> T.sizing(horizontal: Sizing, vertical: Sizing): T = apply {
    horizontalSizing = horizontal
    verticalSizing = vertical
}

fun <T : Element> T.horizontalSizing(sizing: Sizing): T = apply { horizontalSizing = sizing }
fun <T : Element> T.verticalSizing(sizing: Sizing): T = apply { verticalSizing = sizing }
fun <T : Element> T.margins(insets: Insets): T = apply { margins = insets }
fun <T : Element> T.positioning(positioning: Positioning): T = apply { this.positioning = positioning }
fun <T : Element> T.surface(surface: Surface): T = apply { this.surface = surface }

class Label(text: Component) : Element() {
    private val font get() = Minecraft.getInstance().font

    var text: Component = text
        private set
    private var color = -1
    private var shadow = false
    private var maxWidth = Int.MAX_VALUE
    private var textAlignment = Alignment.START
    private var lines: List<FormattedCharSequence> = emptyList()

    fun text(text: Component) = apply { this.text = text }
    fun color(argb: Int) = apply { color = argb }
    fun shadow(shadow: Boolean) = apply { this.shadow = shadow }
    fun maxWidth(maxWidth: Int) = apply { this.maxWidth = maxWidth }
    fun horizontalTextAlignment(alignment: Alignment) = apply { textAlignment = alignment }

    override fun inflate(space: Size) {
        if (horizontalSizing.isContent) {
            lines = wrap(maxWidth)
            width = lines.maxOfOrNull { font.width(it) } ?: 0
        } else {
            width = horizontalSizing.resolve(space.width - margins.horizontal) { 0 }
            lines = wrap(width)
        }
        height = verticalSizing.resolve(space.height - margins.vertical) {
            (lines.size * (LINE_HEIGHT + LINE_SPACING) - LINE_SPACING).coerceAtLeast(0)
        }
    }

    private fun wrap(width: Int) = font.split(text, width.coerceAtLeast(1))

    private fun lineX(line: FormattedCharSequence) = x + textAlignment.align(font.width(line), width)
    private fun lineY(index: Int) = y + index * (LINE_HEIGHT + LINE_SPACING)

    override fun draw(context: RenderContext) {
        lines.forEachIndexed { index, line ->
            context.graphics.text(font, line, lineX(line), lineY(index), color, shadow)
        }
    }

    override fun styleAt(mouseX: Int, mouseY: Int): Style? {
        if (!contains(mouseX, mouseY)) return null
        val index = (mouseY - y) / (LINE_HEIGHT + LINE_SPACING)
        val line = lines.getOrNull(index) ?: return null

        var found: Style? = null
        val textState = GuiTextRenderState(font, line, Matrix3x2f(), lineX(line), lineY(index), color, 0, shadow, true, null)
        ActiveTextCollector.findElementUnderCursor(textState, mouseX.toFloat(), mouseY.toFloat()) { found = it }

        return found?.takeIf { it.hoverEvent != null || it.clickEvent != null }
    }

    companion object {
        const val LINE_HEIGHT = 9
        const val LINE_SPACING = 2
    }
}

class Texture(
    private val texture: Identifier,
    private val u: Int,
    private val v: Int,
    private val regionWidth: Int,
    private val regionHeight: Int,
    private val textureWidth: Int,
    private val textureHeight: Int
) : Element() {
    override fun contentWidth() = regionWidth
    override fun contentHeight() = regionHeight

    override fun draw(context: RenderContext) {
        context.graphics.blit(
            RenderPipelines.GUI_TEXTURED, texture, x, y,
            u.toFloat(), v.toFloat(), regionWidth, regionHeight, textureWidth, textureHeight
        )
    }
}

class Box(horizontal: Sizing, vertical: Sizing, private val color: Int) : Element() {
    init {
        sizing(horizontal, vertical)
    }

    override fun draw(context: RenderContext) {
        context.graphics.fill(x, y, x + width, y + height, color)
    }
}

class Spacer(horizontal: Sizing = Sizing.expand(), vertical: Sizing = Sizing.expand()) : Element() {
    init {
        sizing(horizontal, vertical)
    }

    override fun draw(context: RenderContext) {}
}

// doesnt work inside a ScrollContainer, it never hands its widgets to the screen
class WidgetElement(val widget: AbstractWidget) : Element() {
    override fun contentWidth() = widget.width
    override fun contentHeight() = widget.height

    override fun mount(x: Int, y: Int) {
        super.mount(x, y)
        widget.setPosition(x, y)
    }

    override fun draw(context: RenderContext) {}

    override fun widgets() = listOf(widget)

    override fun updateWidgetVisibility(parentVisible: Boolean) {
        widget.visible = parentVisible && visible
    }
}
