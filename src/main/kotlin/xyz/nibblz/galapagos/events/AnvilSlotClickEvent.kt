package xyz.nibblz.galapagos.events

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.client.gui.screens.inventory.AnvilScreen
import net.minecraft.world.inventory.ContainerInput
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

object AnvilSlotClickEvent {
    // it just gets stupider
    val EVENT: Event<AnvilSlotClickCallback> = EventFactory.createArrayBacked(
        AnvilSlotClickCallback::class.java
    ) { listeners ->
        AnvilSlotClickCallback {
            screen, containerInput, ci, button -> listeners.forEach { it.invoke(screen, containerInput, ci, button) }
        }
    }

    fun interface AnvilSlotClickCallback {
        fun invoke(screen: AnvilScreen, containerInput: ContainerInput, ci: CallbackInfo, button: Int)
    }
}