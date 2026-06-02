package pro.fazeclan.river.stupid_express.client.mixin.role.convener;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pro.fazeclan.river.stupid_express.client.role.convener.ConvenerColorHelper;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerDisguiseComponent;

@Mixin(value = WatheClient.class, priority = 2000)
public class ConvenerInstinctMixin {

    @Shadow
    public static KeyMapping instinctKeybind;

    @Inject(method = "isInstinctEnabled", at = @At("HEAD"), cancellable = true)
    private static void stupidexpress$enableConvenerInstinct(CallbackInfoReturnable<Boolean> cir) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        // 被召集而强制变形的活人，在变形持续期间必须彻底失去本能透视。
        // 这里直接从 WatheClient.isInstinctEnabled 的最前面截断，
        // 这样无论是原版 killer instinct，还是 StupidExpress 自己扩展出来的 instinct，
        // 都会一起被压住。
        ConvenerDisguiseComponent disguiseComponent = ConvenerDisguiseComponent.KEY.get(player);
        if (GameFunctions.isPlayerAliveAndSurvival(player)
                && disguiseComponent.getMorphTicks() > 0) {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.level());
            if (!gameWorldComponent.isRole(player, SERoles.CONVENER)) {
                cir.setReturnValue(false);
                cir.cancel();
                return;
            }
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.level());
        if (gameWorldComponent.isRole(player, SERoles.CONVENER) && instinctKeybind.isDown()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "getInstinctHighlight", at = @At("HEAD"), cancellable = true)
    private static void stupidexpress$highlightBodies(Entity target, CallbackInfoReturnable<Integer> cir) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.level());
        if (!gameWorldComponent.isRole(player, SERoles.CONVENER)) {
            return;
        }
        if (WatheClient.isPlayerSpectatingOrCreative()) {
            return;
        }
        if (!WatheClient.isInstinctEnabled()) {
            return;
        }

        // 尸体继续保持固定的召集者职业色，
        // 这样玩家一眼就能把“可召集的尸体”和“可观察的活人”区分开。
        if (target instanceof PlayerBodyEntity) {
            cir.setReturnValue(SERoles.CONVENER.color());
            cir.cancel();
            return;
        }

        // 召集者本能透视下，所有仍然存活的玩家都必须带颜色。
        // 这里直接覆盖 Wathe 原版“平民才是绿色、其他阵营可能不显示”的逻辑，
        // 改为统一使用召集者自己的流动配色。
        if (target instanceof Player targetPlayer && GameFunctions.isPlayerAliveAndSurvival(targetPlayer)) {
            cir.setReturnValue(ConvenerColorHelper.getPlayerFlowColor(targetPlayer.getUUID()));
            cir.cancel();
        }
    }
}
