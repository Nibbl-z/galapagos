package xyz.nibblz.galapagos.util

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
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

fun getScoreboardLines(): List<String> = getScoreboardLinesComponents().map { it.string }

fun getScoreboardLinesComponents(): List<Component> {
    val scoreboard = Minecraft.getInstance().level?.scoreboard ?: return listOf()
    val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return listOf()
    val lines: MutableList<Component> = mutableListOf()

    scoreboard.listPlayerScores(objective).forEach {
        if (it.display == null) return@forEach
        lines.add(it.display!!)
    }

    return lines
}