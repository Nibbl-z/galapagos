package xyz.nibblz.galapagos.mixin;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nibblz.galapagos.events.ContainerCloseEvent;
import xyz.nibblz.galapagos.util.UtilKt;

@Mixin(Player.class)
public class PlayerMixin {
    @Inject(method = "closeContainer", at = @At("TAIL"))
    private void closeContainer(CallbackInfo ci) {
        if (!UtilKt.onIsland()) return;

        ContainerCloseEvent.INSTANCE.getEVENT().invoker().invoke();
    }
}
