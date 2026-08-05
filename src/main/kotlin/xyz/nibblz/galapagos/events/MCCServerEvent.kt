package xyz.nibblz.galapagos.events

import com.noxcrew.noxesium.core.mcc.ClientboundMccServerPacket
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory

object MCCServerEvent {
    // it gets even stupider
    val EVENT: Event<MCCServerCallback> = EventFactory.createArrayBacked(
        MCCServerCallback::class.java
    ) { listeners ->
        MCCServerCallback {
            packet -> listeners.forEach { it.invoke(packet) }
        }
    }

    fun interface MCCServerCallback {
        fun invoke(packet: ClientboundMccServerPacket)
    }
}