package xyz.nibblz.galapagos.events

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.inventory.ContainerInput

object SlotClickEvent {
    val EVENT: Event<SlotClickCallback> = EventFactory.createArrayBacked(
        SlotClickCallback::class.java
    ) { listeners ->
        SlotClickCallback {
            screen, containerInput -> listeners.forEach { it.invoke(screen, containerInput) }
        }
    }

    fun interface SlotClickCallback {
        fun invoke(screen: ContainerScreen, containerInput: ContainerInput)
    }
}