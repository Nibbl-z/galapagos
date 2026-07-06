package xyz.nibblz.galapagos.events

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory

object ScoreboardTitleUpdateEvent {
    var previousTitle = ""

    val EVENT: Event<ScoreboardTitleUpdateCallback> = EventFactory.createArrayBacked(
        ScoreboardTitleUpdateCallback::class.java
    ) { listeners ->
        ScoreboardTitleUpdateCallback {
            title -> listeners.forEach { it.invoke(title) }
        }
    }

    fun interface ScoreboardTitleUpdateCallback {
        fun invoke(title: String)
    }
}