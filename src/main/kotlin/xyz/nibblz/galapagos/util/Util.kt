package xyz.nibblz.galapagos.util

import net.minecraft.client.Minecraft

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