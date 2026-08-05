package xyz.nibblz.galapagos.events

import com.noxcrew.noxesium.core.mcc.ClientboundMccStatisticPacket
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory

object MCCStatisticEvent {
    // it gets even stupider
    val EVENT: Event<MCCStatisticCallback> = EventFactory.createArrayBacked(
        MCCStatisticCallback::class.java
    ) { listeners ->
        MCCStatisticCallback {
            packet -> listeners.forEach { it.invoke(packet) }
        }
    }

    fun interface MCCStatisticCallback {
        fun invoke(packet: ClientboundMccStatisticPacket)
    }
}