package xyz.nibblz.galapagos.util

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.objects.AtlasSprite
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundSource
import kotlin.math.floor

fun mcciTextureComponent(path: String): MutableComponent {
    return Component.`object`(AtlasSprite(AtlasSprite.DEFAULT_ATLAS, Identifier.fromNamespaceAndPath("mcc", path)))
}

fun playMcciSound(path: String, source: SoundSource = SoundSource.PLAYERS) {
    Minecraft.getInstance().soundManager.play(SimpleSoundInstance(
        Identifier.fromNamespaceAndPath("mcc", path),
        source,
        1.0f, 1.0f,
        SoundInstance.createUnseededRandom(),
        false,
        0,
        SoundInstance.Attenuation.NONE,
        0.0, 0.0, 0.0, true
    ))
}

fun mcciProgressBar(percent: Double, segments: Int): MutableComponent {
    val percent = percent.coerceIn(0.0..1.0)

    val component: MutableComponent = Component.empty()

    val outOf = segments * 5
    val filled = (percent * outOf)
    val hasHalf = filled != floor(filled)

    var i = 0

    repeat(floor(filled).toInt()) {
        component.append(Glyphs.getGlyphComponent("_fonts/icon/progress_counter/full.png"))
        i++
        if (i % 5 == 0) component.append(Component.literal("\uE001").withStyle(Style.EMPTY.withFont(FontDescription.Resource(Identifier.withDefaultNamespace("padding")))))
    }

    if (hasHalf) {
        component.append(Glyphs.getGlyphComponent("_fonts/icon/progress_counter/half.png"))
        i++
        if (i % 5 == 0) component.append(Component.literal("\uE001").withStyle(Style.EMPTY.withFont(FontDescription.Resource(Identifier.withDefaultNamespace("padding")))))
    }

    repeat(outOf - floor(filled).toInt() - (if (hasHalf) 1 else 0)) {
        component.append(Glyphs.getGlyphComponent("_fonts/icon/progress_counter/empty.png"))
        i++
        if (i == outOf) return@repeat
        if (i % 5 == 0) component.append(Component.literal("\uE001").withStyle(Style.EMPTY.withFont(FontDescription.Resource(Identifier.withDefaultNamespace("padding")))))
    }

    return component
}