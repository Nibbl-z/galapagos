package xyz.nibblz.galapagos

object Glyphs {
    val glyphs: HashMap<String, String> = hashMapOf()

    fun addGlyph(path: String, glyph: String) {
        glyphs[path] = glyph
    }

    fun getGlyph(path: String): String {
        return glyphs[path] ?: "?????"
    }
}