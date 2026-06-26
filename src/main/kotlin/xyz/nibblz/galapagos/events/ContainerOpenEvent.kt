package xyz.nibblz.galapagos.events

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket

object ContainerOpenEvent {
    val EVENT: Event<ContainerOpenCallback> = EventFactory.createArrayBacked(
        ContainerOpenCallback::class.java
    ) { listeners ->
        ContainerOpenCallback {
            packet -> listeners.forEach { it.invoke(packet) }
        }
    }

    fun interface ContainerOpenCallback {
        fun invoke(packet: ClientboundContainerSetContentPacket)
    }
}