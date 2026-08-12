package xyz.nibblz.galapagos.util

import net.minecraft.client.Minecraft
import xyz.nibblz.galapagos.mixin.BossOverlayAccessor

fun findBossbarLine(regex: Regex): MatchGroupCollection? {
    getBossbarLines().forEach {
        val match = regex.find(it) ?: return@forEach
        return match.groups
    }

    return null
}

fun getBossbarLines(): List<String> {
    val bossOverlay = Minecraft.getInstance().gui.bossOverlay as BossOverlayAccessor
    val events = bossOverlay.`galapagos$getEvents`().values

    val lines: MutableList<String> = mutableListOf()

    events.forEach {
        lines.add(it.name.string)
    }

    return lines
}