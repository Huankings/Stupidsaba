package pro.fazeclan.river.stupid_express.client.mixin.role.initiate;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pro.fazeclan.river.stupid_express.constants.SERoles;

import java.awt.*;

@Mixin(value = WatheClient.class, priority = 500)
public class InitiateInstinctMixin {

    @Inject(
            method = "getInstinctHighlight",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void initiateHighlightColor(
            Entity target,
            CallbackInfoReturnable<Integer> cir
    ) {
        var gameWorldComponent = GameWorldComponent.KEY.get(Minecraft.getInstance().player.level());
        if (target instanceof Player targettedPlayer) {
            if (gameWorldComponent.isRole(targettedPlayer, SERoles.INITIATE) && gameWorldComponent.isRole(Minecraft.getInstance().player, SERoles.INITIATE)) {
                cir.setReturnValue(SERoles.INITIATE.color());
                cir.cancel();
            }
        }
    }

    @Inject(method = "getInstinctHighlight", at = @At("HEAD"), cancellable = true)
    private static void initiateHighlightToKillers(Entity target, CallbackInfoReturnable<Integer> cir) {
        var player = Minecraft.getInstance().player;
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.level());
        if (!(target instanceof Player targettedPlayer)) {
            return;
        }
        if (!gameWorldComponent.isRole(targettedPlayer, SERoles.INITIATE)) {
            return;
        }
        if (WatheClient.isPlayerSpectatingOrCreative()) {
            return;
        }
        if (!WatheClient.isInstinctEnabled()) {
            return;
        }
        if (!gameWorldComponent.canUseKillerFeatures(player)) {
            return;
        }
        cir.setReturnValue(SERoles.INITIATE.color());
        cir.cancel();
    }

}
