package xyz.nibblz.galapagos.data

enum class WallType {
    STANDARD,
    SIKE,
    RETURNING,
    RUSH,
    ENDER,
    WARP,
    WALTZ,
    TRICK,
    SPRINT,
    WEEPING,
    DANCE,
    DASH,
    SANDWICH,
    GHOST,
    PARTY,
    HIDDEN,
    BULLDOZE,
    JUMPSCARE,
    VENGEFUL,
    POSSESSED;

    // this Sucks
    val label = name.lowercase().replace(name.first().lowercase().first(), name.first())
}