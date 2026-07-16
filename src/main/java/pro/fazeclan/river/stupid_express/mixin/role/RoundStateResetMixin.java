package pro.fazeclan.river.stupid_express.mixin.role;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityComponent;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityManager;
import pro.fazeclan.river.stupid_express.modifier.lovers.LoversPairComponent;

@Mixin(GameFunctions.class)
public class RoundStateResetMixin {

    @Inject(method = "initializeGame", at = @At("HEAD"))
    private static void initializeGame(ServerLevel serverWorld, CallbackInfo ci) {
        /*
         * 这里仍然保留一个很小的 GameFunctions mixin，但它只负责“每局状态清理”，
         * 不再参与胜利判定。
         *
         * 旧版 StupidExpress 会在这里顺手清理 CustomWinnerComponent；
         * 现在独立胜利已经统一写进 Wathe 的 GameRoundEndComponent，
         * initializeGame 时 Wathe 自己会刷新结算快照，所以扩展侧不再需要额外的
         * custom_winner 世界组件。
         *
         * 恋人配对是本局数据，不能跨局沿用。否则上一局 a+b 的配对会残留到下一局，
         * 即使下一局根本没有恋人词条，客户端 HUD 或 VictoryApi 胜利规则仍可能读到
         * 过期伴侣关系。
         */
        LoversPairComponent.KEY.get(serverWorld).clear();
        /*
         * 双重人格的配对与倒计时同样是本局共享状态。
         * 新局开始前先清掉，避免旧局的“副人格已激活 / 倒计时中”影响下一局的保活判断。
         */
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
