package xyz.nibblz.galapagos.ui

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style

// call layout() after changing the tree
abstract class LayoutScreen(title: Component = Component.empty()) : Screen(title) {
    protected val root: Flow = Flow.vertical(Sizing.fill(), Sizing.fill())
    private var built = false
    private var registeredWidgets = emptySet<AbstractWidget>()

    protected abstract fun build(rootComponent: Flow)

    override fun init() {
        if (!built) {
            build(root)
            built = true
        }
        // widgets just got cleared
        registeredWidgets = emptySet()
        layout()
    }

    fun layout() {
        root.inflate(Size(width, height))
        root.mount(0, 0)

        val widgets = root.widgets().toSet()
        (registeredWidgets - widgets).forEach { removeWidget(it) }
        (widgets - registeredWidgets).forEach { addRenderableWidget(it) }
        registeredWidgets = widgets
        root.updateWidgetVisibility(true)
    }

    protected open fun extractScreenBackground(graphics: GuiGraphicsExtractor) {
        extractTransparentBackground(graphics)
    }

    final override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        extractScreenBackground(graphics)
        // no subtitles without this
        minecraft.gui.hud.extractDeferredSubtitles()
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        root.updateWidgetVisibility(true)
        root.render(RenderContext(graphics, mouseX, mouseY, 0, 0, width, height))
        if (!isOverWidget(mouseX.toDouble(), mouseY.toDouble())) {
            root.styleAt(mouseX, mouseY)?.let { extractStyleEffects(graphics, it, mouseX, mouseY) }
        }
        super.extractRenderState(graphics, mouseX, mouseY, a)
    }

    private fun isOverWidget(mouseX: Double, mouseY: Double) = children().any { it.isMouseOver(mouseX, mouseY) }

    private fun extractStyleEffects(graphics: GuiGraphicsExtractor, style: Style, mouseX: Int, mouseY: Int) {
        when (val hover = style.hoverEvent) {
            is HoverEvent.ShowText -> graphics.setTooltipForNextFrame(font, font.split(hover.value(), (width / 2).coerceAtLeast(200)), mouseX, mouseY)
            is HoverEvent.ShowItem -> graphics.setTooltipForNextFrame(font, hover.item().create(), mouseX, mouseY)
            else -> {}
        }
        if (style.clickEvent != null) graphics.requestCursor(CursorTypes.POINTING_HAND)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (super.mouseClicked(event, doubleClick)) return true
        if (root.mouseClicked(event.x(), event.y(), event.button())) return true
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false

        val clickEvent = root.styleAt(event.x().toInt(), event.y().toInt())?.clickEvent ?: return false
        defaultHandleGameClickEvent(clickEvent, minecraft, this)
        return true
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean =
        root.mouseDragged(event.x(), event.y()) || super.mouseDragged(event, dragX, dragY)

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        root.mouseReleased()
        return super.mouseReleased(event)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, scrollX: Double, scrollY: Double): Boolean =
        root.mouseScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
}
