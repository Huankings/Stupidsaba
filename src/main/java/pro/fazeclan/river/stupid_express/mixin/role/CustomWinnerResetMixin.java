package pro.fazeclan.river.stupid_express.mixin.role;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.cca.CustomWinnerComponent;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityComponent;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityManager;
import pro.fazeclan.river.stupid_express.modifier.lovers.LoversPairComponent;

@Mixin(GameFunctions.class)
public class CustomWinnerResetMixin {

    @Inject(method = "initializeGame", at = @At("HEAD"))
    private static void initializeGame(ServerLevel serverWorld, CallbackInfo ci) {
        CustomWinnerComponent component = CustomWinnerComponent.KEY.get(serverWorld);
        component.reset();
        /*
         * 恋人配对是“本局数据”，不能跨局沿用。
         * 否则上一局 a+b 的配对会残留到下一局，即使下一局根本没有恋人词条，
         * 客户端 HUD 或胜负逻辑仍可能读到过期伴侣关系。
         */
        LoversPairComponent.KEY.get(serverWorld).clear();
        DualPersonalityComponent.KEY.get(serverWorld).clear();
    }

    @Inject(method = "finalizeGame", at = @At("TAIL"))
    private static void stupidexpress$clearDualPersonalityAfterFinalize(ServerLevel serverWorld, CallbackInfo ci) {
        /*
         * initializeGame 只能保证“下一局开始前”清理。
         * 但也不能在 stopGame 刚进入 STOPPING 时马上清：
         * 结算到黑幕传回准备大厅前，玩家仍可能看到副人格尸体/准星名字。
         * 所以参考 NoellesRoles 变形怪 resetPlayer 的思路，把清理延后到 Wathe finalizeGame 末尾。
         */
        DualPersonalityManager.clearRoundState(serverWorld);
    }

}
