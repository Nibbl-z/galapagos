package xyz.nibblz.galapagos.util

import net.minecraft.client.Minecraft
import net.minecraft.util.ARGB
import java.math.BigDecimal

// stealing from devcmb stealing from pe3ep part 1
// https://github.com/pe3ep/Trident/blob/master/src/main/kotlin/cc/pe3epwithyou/trident/state/MCCIState.kt
fun onIsland(): Boolean {
    val server = Minecraft.getInstance().currentServer ?: return false
    return server.ip.contains("mccisland.net", true)
}

fun formatTimeString(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds / 60) - (hours * 60)

    return "${if (hours > 0) "${hours}h${if (minutes > 0) " " else ""}" else ""}${if (minutes > 0) "${minutes}m" else ""}"
}

fun HashMap<Int, Int>.getHighest(value: Int): Int {
    return get(keys.sorted().lastOrNull { value >= it }) ?: get(keys.minOf { it })!!
}

fun HashMap<Int, Int>.getLowest(value: Int): Int {
    return get(keys.sorted().firstOrNull { value <= it }) ?: get(keys.maxOf { it })!!
}

data class Vector2(
    val x: Int,
    val y: Int
)

//fun shortenedNumberToInt(number: String): Int {
//    val input = number.uppercase()
//
//    if (input.endsWith("K")) {
//        return input.removeSuffix("K").toFloatOrNull()?.times(1000)?.toInt() ?: 0
//    }
//
//    if (input.endsWith("M")) {
//        return input.removeSuffix("M").toFloatOrNull()?.times(1000000)?.toInt() ?: 0
//    }
//
//    return number.toIntOrNull() ?: 0
//}

fun intToShortenedNumber(number: Int): String {
    if (number < 1000) return number.toString()
    return "${BigDecimal((number.toDouble() / 1000.0).toString()).stripTrailingZeros().toPlainString()}K"
}

fun percentageToColor(percent: Double): Int {
    return if (percent >= 0.5) {
        ARGB.color((-255 * ((percent - 0.5) * 2.0)).toInt() + 255, 255, 0)
    } else {
        ARGB.color(255, (255 * ((percent - 0.5) * 2.0)).toInt() + 255, 0)
    }
}