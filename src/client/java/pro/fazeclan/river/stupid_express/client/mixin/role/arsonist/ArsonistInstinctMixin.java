package pro.fazeclan.river.stupid_express.client.mixin.role.arsonist;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.role.arsonist.cca.DousedPlayerComponent;

import java.awt.*;

@Mixin(WatheClient.class)
public class ArsonistInstinctMixin {

    @Shadow
    public static KeyMapping instinctKeybind;

    @Inject(method = "isInstinctEnabled", at = @At("HEAD"), cancellable = true)
    private static void enableArsonistInstinct(CallbackInfoReturnable<Boolean> cir) {
        var player = Minecraft.getInstance().player;
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.level());
        if (gameWorldComponent.isRole(player, SERoles.ARSONIST)) {
            if (instinctKeybind.isDown()) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }

    @Inject(method = "getInstinctHighlight", at = @At("HEAD"), cancellable = true)
    private static void dousedPlayersInInstinct(Entity target, CallbackInfoReturnable<Integer> cir) {
        var player = Minecraft.getInstance().player;
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.level());
        if (!(target instanceof Player targettedPlayer)) {
            return;
        }
        if (!gameWorldComponent.isRole(player, SERoles.ARSONIST)) {
            return;
        }
        if (WatheClient.isPlayerSpectatingOrCreative()) {
            return;
        }
        if (!WatheClient.isInstinctEnabled()) {
            return;
        }
        var douse = DousedPlayerComponent.KEY.get(targettedPlayer);
        if (douse.isDoused()) {
            cir.setReturnValue(SERoles.ARSONIST.color());
            cir.cancel();
        } else {
            cir.setReturnValue(Color.GRAY.getRGB());
            cir.cancel();
        }
    }

    @Inject(method = "getInstinctHighlight", at = @At("HEAD"), cancellable = true)
    private static void fakeArsonistGreenGlow(Entity target, CallbackInfoReturnable<Integer> cir) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.level());
        if (!(target instanceof Player targettedPlayer)) {
            return;
        }

        // 召集者的本能透视对所有活人都应该统一走自己的流动色，
        // 不能被纵火犯这个“伪装成绿色”的专用逻辑覆盖。
        if (gameWorldComponent.isRole(player, SERoles.CONVENER)) {
            return;
        }

        if (!gameWorldComponent.isRole(targettedPlayer, SERoles.ARSONIST)) {
            return;
        }
        if (WatheClient.isPlayerSpectatingOrCreative()) {
            return;
        }
        if (!WatheClient.isInstinctEnabled()) {
            return;
        }
        cir.setReturnValue(Color.GREEN.getRGB());
    }

}
