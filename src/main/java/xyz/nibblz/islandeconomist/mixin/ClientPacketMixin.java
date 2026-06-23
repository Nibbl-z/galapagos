package xyz.nibblz.islandeconomist.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nibblz.islandeconomist.UtilKt;
import xyz.nibblz.islandeconomist.features.CoinTracking;

@Mixin(ClientPacketListener.class)
public class ClientPacketMixin {
    @Inject(method = "handleContainerContent", at = @At("TAIL"))
    private void handleContainerContent(ClientboundContainerSetContentPacket packet, CallbackInfo ci) {
        if (!UtilKt.onIsland() || !Minecraft.getInstance().isSameThread()) return;

        CoinTracking.INSTANCE.handleContainerContent(packet);
    }
}
