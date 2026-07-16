package pro.fazeclan.river.stupid_express.role.convener;

import dev.doctor4t.wathe.api.win.VictoryApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.level.ServerPlayer;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.victory.StupidExpressVictoryUtil;

import java.util.List;

/**
 * 召集者的持续保活与最后存活独立胜利规则。
 *
 * <p>召集者还会在交互逻辑里通过 ConvenerWinHelper.declareConvenerWin(...) 立即胜利；
 * 本类只处理每 tick 的“别让普通阵营提前结束”和“只剩召集者时结算”。</p>
 */
public final class ConvenerVictoryRule {
    private ConvenerVictoryRule() {
    }

    public static void init() {
        VictoryApi.registerRule(StupidExpress.id("victory/convener"), VictoryApi.DEFAULT_PRIORITY, context -> {
            /*
             * getLivingConvener 统一封装了“召集者是否还活着”的判断。
             * 这样交互胜利、保活胜利和旧工具方法都不会各写一套筛选逻辑。
             */
            ServerPlayer livingConvener = ConvenerWinHelper.getLivingConvener(context.world(), context.gameWorld());
            if (livingConvener == null) {
                return VictoryApi.VictoryResult.pass();
            }

            /*
             * 只剩召集者时，按召集者自己的独立阵营胜利展示。
             * 这里不用调用 ConvenerWinHelper.declareConvenerWin(...)，因为 VictoryApi 当前正在仲裁 tick，
             * 直接返回 customWin 让 Wathe 本体统一 stopGame 更清晰。
             */
            if (context.alivePlayers().size() == 1) {
                return StupidExpressVictoryUtil.customWin(
                        SERoles.CONVENER.identifier(),
                        SERoles.CONVENER.color(),
                        List.of(livingConvener)
                );
            }

            /*
             * 召集者还活着时，杀手 / 乘客的普通胜利会被延后；
             * 但 TIME 不拦截，保留 Wathe 的超时结算兜底。
             */
            if (context.vanillaWinStatus() == GameFunctions.WinStatus.KILLERS
                    || context.vanillaWinStatus() == GameFunctions.WinStatus.PASSENGERS) {
                return VictoryApi.VictoryResult.keepRunning();
            }
            return VictoryApi.VictoryResult.pass();
        });
    }
}
