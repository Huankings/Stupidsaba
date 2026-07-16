package pro.fazeclan.river.stupid_express.role.arsonist;

import dev.doctor4t.wathe.api.win.VictoryApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.level.ServerPlayer;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.victory.StupidExpressVictoryUtil;

import java.util.List;

/**
 * 纵火犯的“活着时拖住普通结算 / 最后存活时独立胜利”规则。
 */
public final class ArsonistVictoryRule {
    private ArsonistVictoryRule() {
    }

    public static void init() {
        VictoryApi.registerRule(StupidExpress.id("victory/arsonist"), VictoryApi.DEFAULT_PRIORITY, context -> {
            /*
             * 这个开关来自 StupidExpress 配置。
             * 关闭时纵火犯不再承担保活逻辑，只保留道具自身的即时胜利行为。
             */
            if (!StupidExpress.CONFIG.rolesSection.arsonistSection.arsonistKeepsGameGoing) {
                return VictoryApi.VictoryResult.pass();
            }

            List<ServerPlayer> alivePlayers = context.alivePlayers();
            List<ServerPlayer> arsonists = alivePlayers.stream()
                    .filter(player -> context.gameWorld().isRole(player, SERoles.ARSONIST))
                    .toList();
            if (arsonists.isEmpty()) {
                return VictoryApi.VictoryResult.pass();
            }

            /*
             * 只剩纵火犯存活时，右侧“独立胜利阵营”只显示纵火犯玩家。
             * 这里支持多个纵火犯一起被写入 winners，虽然正常玩法通常只剩一名。
             */
            if (alivePlayers.size() == 1) {
                return StupidExpressVictoryUtil.customWin(SERoles.ARSONIST.identifier(), SERoles.ARSONIST.color(), arsonists);
            }

            /*
             * 纵火犯还活着时，杀手 / 乘客不应提前结算。
             * TIME 不在这里拦截，避免时间归零后出现永远不结束的局。
             */
            if (context.vanillaWinStatus() == GameFunctions.WinStatus.KILLERS
                    || context.vanillaWinStatus() == GameFunctions.WinStatus.PASSENGERS) {
                return VictoryApi.VictoryResult.keepRunning();
            }
            return VictoryApi.VictoryResult.pass();
        });
    }
}
