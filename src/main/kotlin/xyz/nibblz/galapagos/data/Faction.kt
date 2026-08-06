package xyz.nibblz.galapagos.data

enum class Faction(val label: String, val spriteFolder: String) {
    RED_RABBITS("Red Rabbits", "red"),
    ORANGE_OCELOTS("Orange Ocelots", "orange"),
    YELLOW_YAKS("Yellow Yaks", "yellow"),
    LIME_LLAMAS("Lime Llamas", "lime"),
    GREEN_GECKOS("Green Geckos", "green"),
    CYAN_COYOTES("Cyan Coyotes", "cyan"),
    AQUA_AXOLOTLS("Aqua Axolotls", "aqua"),
    BLUE_BATS("Blue Bats", "blue"),
    PURPLE_PANDAS("Purple Pandas", "purple"),
    PINK_PARROTS("Pink Parrots", "pink"); // The Best! :pink:

    fun getSprite(level: Int): String {
        val evolution = (level / 30).coerceIn(0..10)
        return "_fonts/icon/prestige/small/$spriteFolder/$evolution.png"
    }
}

val FACTION_XP_PER_LEVEL: HashMap<IntRange, Int> = hashMapOf(
    0..9 to 1000,
    10..19 to 1500,
    20..29 to 2000,
    30..39 to 3000,
    40..49 to 4000,
    50..59 to 5000,
    60..69 to 6000,
    70..79 to 7000,
    80..89 to 8000,
    90..99 to 10000,
    100..109 to 12000,
    110..119 to 14000,
    120..129 to 16000,
    130..139 to 18000,
    140..149 to 20000,
    150..159 to 23000,
    160..169 to 26000,
    170..179 to 29000,
    180..189 to 32000,
    190..199 to 35000,
    200..209 to 40000,
    210..219 to 45000,
    220..229 to 50000,
    230..239 to 55000,
    240..249 to 60000,
    250..259 to 65000,
    260..269 to 70000,
    270..279 to 80000,
    280..289 to 90000,
    290..Int.MAX_VALUE to 100000
)

fun getFactionLevelAndProgress(xp: Int): Pair<Int, Int> {
    var level = 0
    var progress = xp

    while(true) {
        val xpAtLevel = FACTION_XP_PER_LEVEL.entries.find { level in it.key }?.value
            ?: throw IllegalStateException("Attempted to get XP at invalid level $level")

        if (progress >= xpAtLevel) progress -= xpAtLevel else break
        level++
    }

    return level to progress
}