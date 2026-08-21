package xyz.nibblz.galapagos.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface GuiAccessor {
    @Accessor("overlayMessageString")
    @Nullable Component galapagos$getOverlayMesssageString();
}
