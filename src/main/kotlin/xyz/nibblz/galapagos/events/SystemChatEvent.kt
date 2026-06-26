package xyz.nibblz.galapagos.events

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket

object SystemChatEvent {
    val EVENT: Event<SystemChatCallback> = EventFactory.createArrayBacked(
        SystemChatCallback::class.java
    ) { listeners ->
        SystemChatCallback {
            packet -> listeners.forEach { it.invoke(packet) }
        }
    }

    fun interface SystemChatCallback {
        fun invoke(packet: ClientboundSystemChatPacket)
    }
}