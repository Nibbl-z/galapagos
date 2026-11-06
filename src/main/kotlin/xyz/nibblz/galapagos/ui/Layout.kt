package xyz.nibblz.galapagos.ui

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import xyz.nibblz.galapagos.Galapagos
import kotlin.math.roundToInt

data class Size(val width: Int, val height: Int)

class Sizing private constructor(private val method: Method, private val value: Int) {
    private enum class Method { FIXED, CONTENT, FILL, EXPAND }

    val isContent get() = method == Method.CONTENT
    val isExpand get() = method == Method.EXPAND

    fun resolve(space: Int, content: () -> Int): Int = when (method) {
        Method.FIXED -> value
        Method.CONTENT -> content()
        Method.FILL, Method.EXPAND -> (value / 100f * space).roundToInt()
    }

    companion object {
        fun fixed(pixels: Int) = Sizing(Method.FIXED, pixels)
        fun content() = Sizing(Method.CONTENT, 0)
        fun fill(percent: Int = 100) = Sizing(Method.FILL, percent)
        fun expand(percent: Int = 100) = Sizing(Method.EXPAND, percent)
    }
}

data class Insets(val top: Int = 0, val bottom: Int = 0, val left: Int = 0, val right: Int = 0) {
    val horizontal get() = left + right
    val vertical get() = top + bottom

    companion object {
        val NONE = Insets()
        fun all(inset: Int) = Insets(inset, inset, inset, inset)
        fun top(inset: Int) = Insets(top = inset)
        fun bottom(inset: Int) = Insets(bottom = inset)
    }
}

sealed interface Positioning {
    data object Layout : Positioning
    data class Absolute(val x: Int, val y: Int) : Positioning
    data class Relative(val xPercent: Int, val yPercent: Int) : Positioning

    companion object {
        fun absolute(x: Int, y: Int) = Absolute(x, y)
        fun relative(xPercent: Int, yPercent: Int) = Relative(xPercent, yPercent)
    }
}

enum class Alignment {
    START, CENTER, END;

    fun align(content: Int, space: Int) = when (this) {
        START -> 0
        CENTER -> (space - content) / 2
        END -> space - content
    }
}

fun interface Surface {
    fun draw(graphics: GuiGraphicsExtractor, element: Element)

    companion object {
        val VANILLA_TRANSLUCENT = Surface { graphics, element ->
            graphics.fillGradient(element.x, element.y, element.x + element.width, element.y + element.height, 0xC0101010.toInt(), 0xD0101010.toInt())
        }

        val MCCI_PANEL = sprite(Identifier.fromNamespaceAndPath(Galapagos.MOD_ID, "mcci_panel"))

        // nine slice is set up in the .mcmeta
        fun sprite(sprite: Identifier) = Surface { graphics, element ->
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, element.x, element.y, element.width, element.height)
        }
    }
}

// inside a ScrollContainer the mouse + clip are already offset by the scroll amount
class RenderContext(
    val graphics: GuiGraphicsExtractor,
    val mouseX: Int,
    val mouseY: Int,
    val clipX: Int,
    val clipY: Int,
    val clipWidth: Int,
    val clipHeight: Int
) {
    fun isVisible(element: Element) =
        element.x < clipX + clipWidth && element.x + element.width > clipX &&
            element.y < clipY + clipHeight && element.y + element.height > clipY
}
