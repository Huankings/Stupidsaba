package pro.fazeclan.river.stupid_express.role.thief;

import dev.doctor4t.wathe.api.win.VictoryApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.level.ServerPlayer;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.victory.StupidExpressVictoryUtil;

import java.util.List;

/**
 * 小偷的独立胜利和“场上仍有可偷武器时保活”规则。
 */
public final class ThiefVictoryRule {
    private ThiefVictoryRule() {
    }

    public static void init() {
        VictoryApi.registerRule(StupidExpress.id("victory/thief"), VictoryApi.DEFAULT_PRIORITY, context -> {
            List<ServerPlayer> alivePlayers = context.alivePlayers();
            List<ServerPlayer> thieves = alivePlayers.stream()
                    .filter(player -> context.gameWorld().isRole(player, SERoles.THIEF))
                    .toList();
            if (thieves.isEmpty()) {
                return VictoryApi.VictoryResult.pass();
            }

            /*
             * 小偷成为唯一存活者时，结算为小偷自己的独立胜利阵营。
             */
            if (alivePlayers.size() == 1) {
                return StupidExpressVictoryUtil.customWin(SERoles.THIEF.identifier(), SERoles.THIEF.color(), thieves);
            }

            /*
             * 小偷的拖局条件不是“只要活着”，而是“仍有可用武器目标”。
             * ThiefItemTracker 会根据小偷可偷取 / 可追踪的武器状态给出这个判断。
             */
            if (ThiefItemTracker.isWeaponAvailable()
                    && (context.vanillaWinStatus() == GameFunctions.WinStatus.KILLERS
                    || context.vanillaWinStatus() == GameFunctions.WinStatus.PASSENGERS)) {
                return VictoryApi.VictoryResult.keepRunning();
            }
            return VictoryApi.VictoryResult.pass();
        });
    }
}
