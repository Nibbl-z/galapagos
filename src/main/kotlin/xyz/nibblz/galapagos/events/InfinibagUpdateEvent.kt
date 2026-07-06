package xyz.nibblz.galapagos.events

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory

object InfinibagUpdateEvent {
    val EVENT: Event<InfinibagUpdateCallback> = EventFactory.createArrayBacked(
        InfinibagUpdateCallback::class.java
    ) { listeners ->
        InfinibagUpdateCallback {
            listeners.forEach { it.invoke() }
        }
    }

    fun interface InfinibagUpdateCallback {
        fun invoke()
    }
}