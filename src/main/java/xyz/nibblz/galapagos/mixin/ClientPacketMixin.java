package xyz.nibblz.galapagos.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nibblz.galapagos.events.ContainerOpenEvent;
import xyz.nibblz.galapagos.events.ContainerSetSlotEvent;
import xyz.nibblz.galapagos.events.SystemChatEvent;
import xyz.nibblz.galapagos.util.UtilKt;

@Mixin(ClientPacketListener.class)
public class ClientPacketMixin {
    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private void handleContainerContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        if (!UtilKt.onIsland() || !Minecraft.getInstance().isSameThread()) return;

        ContainerOpenEvent.INSTANCE.getEVENT().invoker().invoke(packet);
    }

    @Inject(method = "handleSystemChat", at = @At("HEAD"))
    private void handleSystemChat(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        if (!UtilKt.onIsland() || !Minecraft.getInstance().isSameThread()) return;

        SystemChatEvent.INSTANCE.getEVENT().invoker().invoke(packet);
    }

    @Inject(method = "handleContainerSetSlot", at = @At("TAIL"))
    private void handleContainerSetSlot(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        if (!UtilKt.onIsland() || !Minecraft.getInstance().isSameThread()) return;

        ContainerSetSlotEvent.INSTANCE.getEVENT().invoker().invoke(packet);
    }
}
