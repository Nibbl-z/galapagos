package xyz.nibblz.galapagos.data.game

import kotlinx.serialization.Serializable
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import xyz.nibblz.galapagos.Galapagos
import xyz.nibblz.galapagos.util.mcciTextureComponent

enum class BattleBoxKit(val label: String, val bbColor: Int, val bbaColor: Int) {
    RANGER("Ranger", 0xffffff, 0xeb4b4b),
    FLANKER("Flanker", 0x5bfca4, 0xed702d),
    SPECTRE("Spectre", 0xffffff, 0xfac55a),
    TRICKSTER("Trickster", 0xb95bfc, 0x6fff4f),
    BALLER("Baller", 0x5aaffa, 0x5afad2),
    SHARPSHOOTER("Sharpshooter", 0xfac55a, 0x5aaffa),
    SCRAPPER("Scrapper", 0xeb4b4b, 0x4f55ff),
    HERO("Hero", 0xffffff, 0x835bfc),
    HEALER("Healer", 0x6fff4f, 0xf15bfc),
    GADGETEER("Gadgeteer", 0xed702d, 0xa3a3a3);

    fun bbSprite(): String { return "island_items/battle_box/classic_kit/${name.lowercase()}" }
    fun bbaSprite(): String { return "island_items/battle_box/kit/${name.lowercase()}" }
}

enum class BattleBoxArenaCoreKitType(val label: String) {
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
    OPORTUNE("Opportune"), // british spelling </3
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

val BATTLE_BOX_ARENA_MAP_DEFAULT_KITS: HashMap<String, HashMap<BattleBoxKit, BattleBoxArenaCoreKitType>> = hashMapOf(
    "Cargo" to hashMapOf(
        BattleBoxKit.BALLER to BattleBoxArenaCoreKitType.SCOUT,
        BattleBoxKit.FLANKER to BattleBoxArenaCoreKitType.OPORTUNE,
        BattleBoxKit.HERO to BattleBoxArenaCoreKitType.COBWEB,
        BattleBoxKit.RANGER to BattleBoxArenaCoreKitType.PEEK,
        BattleBoxKit.TRICKSTER to BattleBoxArenaCoreKitType.MULTISHOT,
        BattleBoxKit.SHARPSHOOTER to BattleBoxArenaCoreKitType.SUSTENANCE,
    ),
    "Cherry Blossom" to hashMapOf(
        BattleBoxKit.HEALER to BattleBoxArenaCoreKitType.SCOUT,
        BattleBoxKit.BALLER to BattleBoxArenaCoreKitType.DUNK,
        BattleBoxKit.SPECTRE to BattleBoxArenaCoreKitType.OPORTUNE,
        BattleBoxKit.GADGETEER to BattleBoxArenaCoreKitType.ESCAPE,
        BattleBoxKit.TRICKSTER to BattleBoxArenaCoreKitType.SCOUT,
        BattleBoxKit.SCRAPPER to BattleBoxArenaCoreKitType.RAPID_BOW,
    ),
    "Classic Unboxed" to hashMapOf(
        BattleBoxKit.BALLER to BattleBoxArenaCoreKitType.STANDARD,
        BattleBoxKit.SCRAPPER to BattleBoxArenaCoreKitType.STANDARD,
        BattleBoxKit.SHARPSHOOTER to BattleBoxArenaCoreKitType.OPORTUNE,
        BattleBoxKit.RANGER to BattleBoxArenaCoreKitType.STANDARD,
        BattleBoxKit.FLANKER to BattleBoxArenaCoreKitType.FLIGHT,
        BattleBoxKit.HERO to BattleBoxArenaCoreKitType.LAUNCH,
    ),
    "Courtyard Unboxed" to hashMapOf(
        BattleBoxKit.HEALER to BattleBoxArenaCoreKitType.REBOUND,
        BattleBoxKit.HERO to BattleBoxArenaCoreKitType.STANDARD,
        BattleBoxKit.BALLER to BattleBoxArenaCoreKitType.BATTLE,
        BattleBoxKit.TRICKSTER to BattleBoxArenaCoreKitType.POWER,
        BattleBoxKit.GADGETEER to BattleBoxArenaCoreKitType.OPORTUNE,
        BattleBoxKit.SCRAPPER to BattleBoxArenaCoreKitType.COBWEB,
    ),
    "Penthouse" to hashMapOf(
        BattleBoxKit.TRICKSTER to BattleBoxArenaCoreKitType.LAUNCH,
        BattleBoxKit.SPECTRE to BattleBoxArenaCoreKitType.BASTION,
        BattleBoxKit.RANGER to BattleBoxArenaCoreKitType.OPORTUNE,
        BattleBoxKit.SHARPSHOOTER to BattleBoxArenaCoreKitType.SEEK,
        BattleBoxKit.GADGETEER to BattleBoxArenaCoreKitType.TNT,
        BattleBoxKit.HEALER to BattleBoxArenaCoreKitType.FLIGHT,
    ),
    "Platform" to hashMapOf(
        BattleBoxKit.HERO to BattleBoxArenaCoreKitType.REBOUND,
        BattleBoxKit.FLANKER to BattleBoxArenaCoreKitType.PATH,
        BattleBoxKit.SHARPSHOOTER to BattleBoxArenaCoreKitType.BURST,
        BattleBoxKit.RANGER to BattleBoxArenaCoreKitType.REVIVE,
        BattleBoxKit.SCRAPPER to BattleBoxArenaCoreKitType.SWORD,
        BattleBoxKit.SPECTRE to BattleBoxArenaCoreKitType.SWORD,
    ),
    "Santorini" to hashMapOf(
        BattleBoxKit.HEALER to BattleBoxArenaCoreKitType.BATTLE,
        BattleBoxKit.SPECTRE to BattleBoxArenaCoreKitType.PATH,
        BattleBoxKit.FLANKER to BattleBoxArenaCoreKitType.DUAL_BOW,
        BattleBoxKit.SHARPSHOOTER to BattleBoxArenaCoreKitType.BASTION,
        BattleBoxKit.BALLER to BattleBoxArenaCoreKitType.SEEK,
        BattleBoxKit.TRICKSTER to BattleBoxArenaCoreKitType.STANDARD,
    ),
    "Spaceship Unboxed" to hashMapOf(
        BattleBoxKit.HEALER to BattleBoxArenaCoreKitType.STANDARD,
        BattleBoxKit.FLANKER to BattleBoxArenaCoreKitType.BATTLE,
        BattleBoxKit.RANGER to BattleBoxArenaCoreKitType.POWER,
        BattleBoxKit.SPECTRE to BattleBoxArenaCoreKitType.STANDARD,
        BattleBoxKit.SCRAPPER to BattleBoxArenaCoreKitType.BATTLE,
        BattleBoxKit.GADGETEER to BattleBoxArenaCoreKitType.BASTION,
    ),
    "Fusion Core" to hashMapOf(
        BattleBoxKit.HEALER to BattleBoxArenaCoreKitType.SEEK,
        BattleBoxKit.SCRAPPER to BattleBoxArenaCoreKitType.POWER,
        BattleBoxKit.SPECTRE to BattleBoxArenaCoreKitType.LAUNCH,
        BattleBoxKit.FLANKER to BattleBoxArenaCoreKitType.BATTLE,
        BattleBoxKit.RANGER to BattleBoxArenaCoreKitType.POWER,
        BattleBoxKit.GADGETEER to BattleBoxArenaCoreKitType.FLIGHT,
    ),
    "Gold Mine Unboxed" to hashMapOf(
        BattleBoxKit.FLANKER to BattleBoxArenaCoreKitType.FLIGHT,
        BattleBoxKit.RANGER to BattleBoxArenaCoreKitType.TNT,
        BattleBoxKit.HERO to BattleBoxArenaCoreKitType.LAUNCH,
        BattleBoxKit.SCRAPPER to BattleBoxArenaCoreKitType.LAUNCH,
        BattleBoxKit.SHARPSHOOTER to BattleBoxArenaCoreKitType.COBWEB,
        BattleBoxKit.BALLER to BattleBoxArenaCoreKitType.BURST,
    ),
    "Gold Mine Unboxed" to hashMapOf(
        BattleBoxKit.FLANKER to BattleBoxArenaCoreKitType.FLIGHT,
        BattleBoxKit.RANGER to BattleBoxArenaCoreKitType.TNT,
        BattleBoxKit.HERO to BattleBoxArenaCoreKitType.LAUNCH,
        BattleBoxKit.SCRAPPER to BattleBoxArenaCoreKitType.LAUNCH,
        BattleBoxKit.SHARPSHOOTER to BattleBoxArenaCoreKitType.COBWEB,
        BattleBoxKit.BALLER to BattleBoxArenaCoreKitType.BURST,
    ),
    "Heater" to hashMapOf(
        BattleBoxKit.HEALER to BattleBoxArenaCoreKitType.POWER,
        BattleBoxKit.BALLER to BattleBoxArenaCoreKitType.DUNK,
        BattleBoxKit.SPECTRE to BattleBoxArenaCoreKitType.COBWEB,
        BattleBoxKit.GADGETEER to BattleBoxArenaCoreKitType.LAUNCH,
        BattleBoxKit.TRICKSTER to BattleBoxArenaCoreKitType.TNT,
        BattleBoxKit.SCRAPPER to BattleBoxArenaCoreKitType.RAPID_BOW,
    ),
    "Slay Unboxed" to hashMapOf(
        BattleBoxKit.HERO to BattleBoxArenaCoreKitType.REBOUND,
        BattleBoxKit.SPECTRE to BattleBoxArenaCoreKitType.JUMPSCARE,
        BattleBoxKit.FLANKER to BattleBoxArenaCoreKitType.STANDARD,
        BattleBoxKit.RANGER to BattleBoxArenaCoreKitType.BASTION,
        BattleBoxKit.SHARPSHOOTER to BattleBoxArenaCoreKitType.STANDARD,
        BattleBoxKit.SCRAPPER to BattleBoxArenaCoreKitType.SWORD,
    ),
    "Slay Unboxed" to hashMapOf(
        BattleBoxKit.HERO to BattleBoxArenaCoreKitType.REBOUND,
        BattleBoxKit.SPECTRE to BattleBoxArenaCoreKitType.JUMPSCARE,
        BattleBoxKit.FLANKER to BattleBoxArenaCoreKitType.STANDARD,
        BattleBoxKit.RANGER to BattleBoxArenaCoreKitType.BASTION,
        BattleBoxKit.SHARPSHOOTER to BattleBoxArenaCoreKitType.STANDARD,
        BattleBoxKit.SCRAPPER to BattleBoxArenaCoreKitType.SWORD,
    ),
    "Street" to hashMapOf(
        BattleBoxKit.TRICKSTER to BattleBoxArenaCoreKitType.TELE,
        BattleBoxKit.SPECTRE to BattleBoxArenaCoreKitType.POWER,
        BattleBoxKit.RANGER to BattleBoxArenaCoreKitType.ORB,
        BattleBoxKit.SHARPSHOOTER to BattleBoxArenaCoreKitType.SEEK,
        BattleBoxKit.GADGETEER to BattleBoxArenaCoreKitType.BATTLE,
        BattleBoxKit.HEALER to BattleBoxArenaCoreKitType.FLIGHT,
    ),
    "Train Station Unboxed" to hashMapOf(
        BattleBoxKit.BALLER to BattleBoxArenaCoreKitType.BLIND,
        BattleBoxKit.FLANKER to BattleBoxArenaCoreKitType.BASTION,
        BattleBoxKit.HERO to BattleBoxArenaCoreKitType.SCOUT,
        BattleBoxKit.RANGER to BattleBoxArenaCoreKitType.PEEK,
        BattleBoxKit.TRICKSTER to BattleBoxArenaCoreKitType.MULTISHOT,
        BattleBoxKit.SHARPSHOOTER to BattleBoxArenaCoreKitType.MULTISHOT,
    )
)

enum class BattleBoxRound(val scoreboardLetter: Char, val color: Int) {
    WIN('W', ChatFormatting.GREEN.color!!),
    LOSS('L', ChatFormatting.RED.color!!),
    DRAW('D', ChatFormatting.YELLOW.color!!)
}

@Serializable
data class BattleBoxArenaKitChoice(
    val kit: BattleBoxKit,
    val core: BattleBoxArenaCoreKitType,
    val round: Int
)

fun bbaKitComponent(kit: BattleBoxKit, type: BattleBoxArenaCoreKitType): MutableComponent {
    return Component.empty()
        .append(mcciTextureComponent(kit.bbaSprite()))
        .append(Component.literal("\u200B").withStyle(Style.EMPTY.withFont(Galapagos.font)))
        .append(mcciTextureComponent(type.sprite()))
}