package xyz.nibblz.galapagos.data.game

enum class BattleBoxKit(val label: String) {
    RANGER("Ranger"),
    FLANKER("Flanker"),
    SPECTRE("Spectre"),
    TRICKSTER("Trickster"),
    BALLER("Baller"),
    SHARPSHOOTER("Sharpshooter"),
    SCRAPPER("Scrapper"),
    HERO("Hero"),
    HEALER("Healer"),
    GADGETEER("Gadgeteer");

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
    VOID("Void"); // unused

    fun sprite(): String {
        return "island_items/battle_box/core_kit_types/${name.lowercase()}"
    }
}

enum class BattleBoxRound(val scoreboardLetter: Char) {
    WIN('W'),
    LOSS('L'),
    DRAW('D')
}