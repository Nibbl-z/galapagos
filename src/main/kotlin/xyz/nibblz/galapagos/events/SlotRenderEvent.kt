package xyz.nibblz.galapagos.events

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.inventory.Slot

object SlotRenderEvent {
    val EVENT: Event<SlotRenderEvent> = EventFactory.createArrayBacked(
        SlotRenderEvent::class.java
    ) { listeners ->
        SlotRenderEvent {
            graphics, slot -> listeners.forEach { it.invoke(graphics, slot) }
        }
    }

    fun interface SlotRenderEvent {
        fun invoke(graphics: GuiGraphicsExtractor, slot: Slot)
    }
}