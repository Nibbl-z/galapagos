package xyz.nibblz.galapagos.util

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier

object Glyphs {
    val glyphs: HashMap<String, String> = hashMapOf()

    fun addGlyph(path: String, glyph: String) {
        glyphs[path] = glyph
    }

    fun getGlyphComponent(path: String): MutableComponent {
        // is this the worst wordl ever?
        return Component.literal(getGlyph(path))
            .withStyle(Style.EMPTY.withFont(FontDescription.Resource(Identifier.fromNamespaceAndPath("mcc", "icon"))))
            .withColor(0xffffff)
    }

    fun getGlyph(path: String): String {
        return glyphs[path] ?: "?????"
    }
}