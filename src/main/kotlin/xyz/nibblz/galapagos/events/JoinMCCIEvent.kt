package xyz.nibblz.galapagos.events

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory

object JoinMCCIEvent {
    val EVENT: Event<JoinMCCICallback> = EventFactory.createArrayBacked(
        JoinMCCICallback::class.java
    ) { listeners ->
        JoinMCCICallback {
            listeners.forEach { it.invoke() }
        }
    }

    fun interface JoinMCCICallback {
        fun invoke()
    }
}