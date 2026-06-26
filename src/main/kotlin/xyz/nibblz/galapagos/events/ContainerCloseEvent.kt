package xyz.nibblz.galapagos.events

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory

object ContainerCloseEvent {
    val EVENT: Event<ContainerCloseCallback> = EventFactory.createArrayBacked(
        ContainerCloseCallback::class.java
    ) { listeners ->
        ContainerCloseCallback {
            listeners.forEach { it.invoke() }
        }
    }

    fun interface ContainerCloseCallback {
        fun invoke()
    }
}