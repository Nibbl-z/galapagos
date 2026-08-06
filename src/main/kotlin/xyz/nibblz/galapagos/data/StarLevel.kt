package xyz.nibblz.galapagos.data

enum class StarLevel(val level: Int) {
    GRAY(0),
    WHITE(10),
    GREEN(20),
    BLUE(40),
    PURPLE(65),
    ORANGE(100),
    RED(150),
    AQUA(250),
    PRISM(350),
    ROSE(500),
    DARK(700),
    RAINBOW(1000);

    fun getSprite(): String {
        return "_fonts/icon/stars/${name.lowercase()}"
    }
}

fun getStarLevelEvolution(level: Int): StarLevel {
    return StarLevel.entries.findLast { level >= it.level } ?: StarLevel.GRAY
}

enum class StarLevelGame(val statistic: String, val sprite: String, val label: String) {
    BATTLE_BOX("battle_box_xp_earned", "island_interface/game/battle_box/icon", "Battle Box"),
    DYNABALL("dynaball_xp_earned", "island_interface/game/dynaball/icon", "Dynaball"),
    HOLE_IN_THE_WALL("hole_in_the_wall_xp_earned", "island_interface/game/hole_in_the_wall/icon", "Hole in the Wall"),
    PARKOUR_WARRIOR("pw_xp_earned", "island_interface/game/parkour_warrior/icon", "Parkour Warrior"),
    ROCKET_SPLEEF("rocket_spleef_xp_earned", "island_interface/game/rocket_spleef/icon", "Rocket Spleef Rush"),
    SKY_BATTLE("sky_battle_xp_earned", "island_interface/game/sky_battle/icon", "Sky Battle"),
    TGTTOS("tgttos_xp_earned", "island_interface/game/tgttosawaf/icon", "TGTTOS")
}