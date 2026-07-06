package xyz.nibblz.galapagos.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nibblz.galapagos.UtilKt;
import xyz.nibblz.galapagos.events.ScoreboardTitleUpdateEvent;

@Mixin(Gui.class)
public class GuiMixin {
    @Inject(method = "displayScoreboardSidebar", at = @At("TAIL"))
    void displayScoreboardSidebar(GuiGraphicsExtractor graphics, Objective objective, CallbackInfo ci) {
        if (!UtilKt.onIsland()) return;
        String title = objective.getDisplayName().getString();
        if (!title.contains("MCCI: ")) return;
        if (!ScoreboardTitleUpdateEvent.INSTANCE.getPreviousTitle().equals(title)) {
            ScoreboardTitleUpdateEvent.INSTANCE.getEVENT().invoker().invoke(title);
        }

        ScoreboardTitleUpdateEvent.INSTANCE.setPreviousTitle(title);
    }
}
