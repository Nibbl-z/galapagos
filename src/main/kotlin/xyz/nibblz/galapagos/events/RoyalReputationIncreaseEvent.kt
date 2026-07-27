package xyz.nibblz.galapagos.events

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory

object RoyalReputationIncreaseEvent {
    val EVENT: Event<RoyalReputationIncreaseCallback> = EventFactory.createArrayBacked(
        RoyalReputationIncreaseCallback::class.java
    ) { listeners ->
        RoyalReputationIncreaseCallback {
            cosmetic, count -> listeners.forEach { it.invoke(cosmetic, count) }
        }
    }

    fun interface RoyalReputationIncreaseCallback {
        fun invoke(cosmetic: String, count: Int)
    }
}