package xyz.nibblz.galapagos.mixin;

import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nibblz.galapagos.events.JoinMCCIEvent;

// basically the exact same as pe3ep's mixin because im not going to chase down wherever the "server connect" function is
// https://github.com/pe3ep/Trident/blob/master/src/main/java/cc/pe3epwithyou/trident/mixin/connection/ClientHandshakePacketListenerImplMixin.java

@Mixin(ClientHandshakePacketListenerImpl.class)
public class ClientHandshakePacketListenerImplMixin {
    @Shadow
    @Final
    private @Nullable ServerData serverData;

    @Inject(method = "handleLoginFinished", at = @At("HEAD"))
    private void handleLoginFinished(ClientboundLoginFinishedPacket packet, CallbackInfo ci) {
        if (this.serverData == null) return;
        if (!this.serverData.ip.toLowerCase().contains("mccisland.net")) return;

        JoinMCCIEvent.INSTANCE.getEVENT().invoker().invoke();
    }
}
