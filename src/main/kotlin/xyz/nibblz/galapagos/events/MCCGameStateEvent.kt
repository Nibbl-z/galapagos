package xyz.nibblz.galapagos.events

import com.noxcrew.noxesium.core.mcc.ClientboundMccGameStatePacket
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory

object MCCGameStateEvent {
    // it gets even stupiderer
    val EVENT: Event<MCCGameStateCallback> = EventFactory.createArrayBacked(
        MCCGameStateCallback::class.java
    ) { listeners ->
        MCCGameStateCallback {
            packet -> listeners.forEach { it.invoke(packet) }
        }
    }

    fun interface MCCGameStateCallback {
        fun invoke(packet: ClientboundMccGameStatePacket)
    }
}