package xyz.nibblz.galapagos.data.game

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.util.Glyphs
import xyz.nibblz.galapagos.util.mcciTextureComponent


enum class DeathCause(val messages: List<String> = listOf(), val sprite: String = "", val label: String = "Unknown") {
    NONE,
    MELEE(listOf("slain"), "\uE00D", "Melee"),
    RANGED(listOf("shot"), "\uE00E", "Ranged"),
    EXPLOSIVE(listOf("blown up"), "island_interface/social/session/settings/enable_tnt_crates", "Explosive"), // ig it works
    MAGIC(listOf("eliminated with magic"), "\uE007", "Magic"), // ig it works
    FALL_DAMAGE(listOf("hit the ground"), "island_interface/social/session/settings/fall_damage", "Fall Damage"),
    VOID(listOf("the same world", "fell out of the world"), "_fonts/icon/skull_small", "Void"),
    SUFFOCATE(listOf("suffocat"), "\uE008", "Suffocation"),
    SPLEEF(listOf("spleefed"), "\uE009", "Spleefed"),
    LAVA(listOf("lava"), "\uE00C", "Lava"),
    KNOCKBACK(listOf("knocked back"), "\uE00B", "Knockback Spark"), // i believe this is from someone dying to the void (or maybe fall damage too?) via the knockback effect
    DROWNED(listOf("drowned"), "\uE00A", "Drowned"),
    DISCONNECT(listOf("logged out"), "_fonts/icon/leave", "Disconnect"),
    UNKNOWN(listOf("died"), "_fonts/icon/skull_small"); // probably from disconnect?

    fun createIconComponent(tooltip: Boolean): MutableComponent {
        val component = if (sprite.contains("_fonts")) Glyphs.getGlyphComponent("${sprite}.png")
        else if (sprite.contains("/")) mcciTextureComponent(sprite)
        else Component.literal(sprite).withColor(0xffffff).withStyle(Style.EMPTY.withFont(Galapagos.font))

        return if (!tooltip) component else component.withStyle(Style.EMPTY.withHoverEvent(
            HoverEvent.ShowText(Component.literal(label))
        ))
    }

    fun createIconAndCountComponent(tooltip: Boolean, count: Int): MutableComponent {
        return if (!tooltip)
            Component.empty().append(createIconComponent(false)).append(Component.literal(" $count"))
        else
            Component.empty().append(createIconComponent(true)).append(Component.literal(" $count").withStyle(Style.EMPTY.withHoverEvent(
            HoverEvent.ShowText(Component.literal(label))
        )))
    }

    fun createIconAndLabelComponent(): MutableComponent {
        return Component.empty().append(createIconComponent(false)).append(Component.literal(" $label"))
    }
}