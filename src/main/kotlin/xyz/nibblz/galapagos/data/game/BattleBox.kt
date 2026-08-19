package xyz.nibblz.galapagos.data.game

import kotlinx.serialization.Serializable
import net.minecraft.ChatFormatting

enum class BattleBoxKit(val label: String, val bbColor: Int) {
    RANGER("Ranger", 0xffffff),
    FLANKER("Flanker", 0x5bfca4),
    SPECTRE("Spectre", 0xffffff),
    TRICKSTER("Trickster", 0xb95bfc),
    BALLER("Baller", 0x5aaffa),
    SHARPSHOOTER("Sharpshooter", 0xfac55a),
    SCRAPPER("Scrapper", 0xeb4b4b),
    HERO("Hero", 0xffffff),
    HEALER("Healer", 0x6fff4f),
    GADGETEER("Gadgeteer", 0xed702d);

    fun bbSprite(): String { return "island_items/battle_box/classic_kit/${name.lowercase()}" }
    fun bbaSprite(): String { return "island_items/battle_box/kit/${name.lowercase()}" }
}

enum class BattleBoxArenaCoreKits(val label: String) {
    ABSORPTION("Absorption"), // unused
    BASTION("Bastion"),
    BATTLE("Battle"),
    BLIND("Blind"),
    BURST("Burst"),
    COBWEB("Cobweb"),
    DUAL_BOW("Dual Bow"),
    DUNK("Dunk"),
    ELIMINATION("Elimination"), // unused
    ESCAPE("Escape"),
    FLIGHT("Flight"),
    HOVER("Hover"), // unused
    INVISIBILITY("Invisibility"), // unused
    JUMPSCARE("Jumpscare"),
    KNOCKBACK("Knockback"), // unused
    LAUNCH("Launch"),
    MULTISHOT("Multishot"),
    OPORTUNE("Opportune"),
    ORB("Orb"),
    PATH("Path"),
    PEEK("Peek"),
    PIERCING("Piercing"), // unused
    POISON("Poison"), // unused
    POWER("Power"),
    PULL("Pull"), // unused.. what is this???
    PUSH("Push"), // unused
    RAPID_BOW("Rapid Bow"),
    REBOUND("Rebound"),
    REVIVE("Revive"),
    RUSH("Rush"), // unused
    SCOUT("Scout"),
    SEEK("Seek"),
    SHIELD("Shield"), // unused
    SPEED("Speed"), // unused
    STANDARD("Standard"),
    SUSTENANCE("Sustenance"),
    SWORD("Sword"),
    TELE("Tele"),
    TNT("TNT"),
    VOID("Void"), // unused
    NONE("");

    fun sprite(): String {
        if (this == NONE) return ""
        return "island_items/battle_box/core_kit_types/${name.lowercase()}"
    }
}

enum class BattleBoxRound(val scoreboardLetter: Char, val color: Int) {
    WIN('W', ChatFormatting.GREEN.color!!),
    LOSS('L', ChatFormatting.RED.color!!),
    DRAW('D', ChatFormatting.YELLOW.color!!)
}

@Serializable
data class BattleBoxArenaKitChoice(
    val kit: BattleBoxKit,
    val core: BattleBoxArenaCoreKits,
    val round: Int
)