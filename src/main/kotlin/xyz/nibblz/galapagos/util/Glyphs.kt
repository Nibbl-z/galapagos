package xyz.nibblz.galapagos.util

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier
import java.util.Collections

object Glyphs {
    data class GlyphData(
        val path: String,
        val glyph: String
    )

    val glyphs: HashMap<String, String> = hashMapOf()
    val allGlyphs: MutableList<GlyphData> = mutableListOf()
    val syncGlyphs: MutableMap<String, String> = Collections.synchronizedMap(glyphs)
    val syncAllGlyphs: MutableList<GlyphData> = Collections.synchronizedList(allGlyphs)

    fun addGlyph(path: String, glyph: String) {
        syncGlyphs[path] = glyph
        syncAllGlyphs.add(GlyphData(path, glyph))
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

    fun getGlyphs(path: String): List<String> {
        return allGlyphs.filter { it.path == path }.map { it.glyph }
    }
}