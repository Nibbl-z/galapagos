package xyz.nibblz.galapagos.util

import net.minecraft.client.Minecraft
import net.minecraft.world.scores.DisplaySlot

fun findScoreboardLine(regex: Regex): MatchGroupCollection? {
    getScoreboardLines().forEach {
        val match = regex.find(it) ?: return@forEach
        return match.groups
    }

    return null
}

fun findScoreboardLines(regex: Regex): Sequence<MatchResult>? {
    getScoreboardLines().forEach {
        val match = regex.findAll(it)
        if (match.none()) return@forEach
        return match
    }

    return null
}

fun getScoreboardLines(): List<String> {
    val scoreboard = Minecraft.getInstance().level?.scoreboard ?: return listOf()
    val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return listOf()
    val lines: MutableList<String> = mutableListOf()

    scoreboard.listPlayerScores(objective).forEach {
        if (it.display == null) return@forEach
        lines.add(it.display!!.string)
    }

    return lines
}