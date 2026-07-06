package xyz.nibblz.galapagos.events

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.inventory.ContainerInput
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

object SlotClickEvent {
    val EVENT: Event<SlotClickCallback> = EventFactory.createArrayBacked(
        SlotClickCallback::class.java
    ) { listeners ->
        SlotClickCallback {
            screen, containerInput, ci, button -> listeners.forEach { it.invoke(screen, containerInput, ci, button) }
        }
    }

    fun interface SlotClickCallback {
        fun invoke(screen: ContainerScreen, containerInput: ContainerInput, ci: CallbackInfo, button: Int)
    }
}