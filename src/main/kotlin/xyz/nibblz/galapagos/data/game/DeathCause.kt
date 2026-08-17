package xyz.nibblz.galapagos.data.game


enum class DeathCause(val messages: List<String> = listOf()) {
    NONE,
    MELEE(listOf("slain")),
    RANGED(listOf("shot")),
    EXPLOSIVE(listOf("blown up")),
    MAGIC(listOf("eliminated with magic")),
    FALL_DAMAGE(listOf("hit the ground")),
    VOID(listOf("the same world", "fell out of the world")),
    SUFFOCATE(listOf("suffocat")),
    SPLEEF(listOf("spleefed")),
    LAVA(listOf("lava")),
    KNOCKBACK(listOf("knocked back")), // i believe this is from someone dying to the void (or maybe fall damage too?) via the knockback effect
    UNKNOWN(listOf("died")) // probably from disconnect?
}