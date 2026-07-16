package pro.fazeclan.river.stupid_express.modifier.dual_personality;

import dev.doctor4t.wathe.api.win.VictoryApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;
import pro.fazeclan.river.stupid_express.victory.StupidExpressVictoryUtil;

import java.util.List;

/**
 * 双重人格词条的独立胜利 / 共胜 / 保活规则。
 *
 * <p>旧版逻辑在 DualPersonalityLoopMixin 里直接拦截 MurderGameMode 的局部 winStatus。
 * 现在规则本身放回 dual_personality 包，入口改为 Wathe VictoryApi，避免再依赖 mixin 注入点。</p>
 */
public final class DualPersonalityVictoryRule {
    /*
     * 恋人需要压过双重人格，所以这里低于 LoversVictoryRule 的 100。
     * 如果同一批玩家同时满足“恋人独胜”和“双重人格独胜”，Wathe 会优先展示恋人结算。
     */
    private static final int PRIORITY = 90;

    private DualPersonalityVictoryRule() {
    }

    public static void init() {
        VictoryApi.registerRule(StupidExpress.id("victory/dual_personality"), PRIORITY, context -> {
            var config = StupidExpress.CONFIG.modifiersSection.dualPersonalitySection;
            var world = context.world();
            var gameWorld = context.gameWorld();
            var modifierComponent = WorldModifierComponent.KEY.get(world);
            var dualPersonalityComponent = DualPersonalityComponent.KEY.get(world);

            /*
             * 先筛出还活着的双重人格玩家。
             * 只要列表为空，本规则就完全 pass，不影响杀手 / 乘客 / 其他扩展规则。
             */
            List<ServerPlayer> remainingPlayers = context.alivePlayers();
            List<ServerPlayer> remainingDualPersonalities = remainingPlayers.stream()
                    .filter(player -> modifierComponent.isModifier(player, SEModifiers.DUAL_PERSONALITY))
                    .toList();
            if (remainingDualPersonalities.isEmpty()) {
                return VictoryApi.VictoryResult.pass();
            }

            /*
             * 用户确认过的独立胜利条件：
             * 只要剩余存活玩家全部带双重人格词条，就结算为“双重人格”独立胜利。
             * 这里不要求他们属于同一对，也不要求只剩一个人；多个双重人格可以一起显示在右侧独立胜利阵营。
             */
            if (remainingPlayers.size() == remainingDualPersonalities.size()) {
                return StupidExpressVictoryUtil.customWin(
                        SEModifiers.DUAL_PERSONALITY.identifier(),
                        SEModifiers.DUAL_PERSONALITY.color(),
                        remainingDualPersonalities
                );
            }

            /*
             * 没有原版胜利时，不需要保活或共胜。
             * 双重人格的拦截只发生在 Wathe 准备把游戏交给杀手 / 乘客结算的时候。
             */
            if (!context.hasVanillaWinner()) {
                return VictoryApi.VictoryResult.pass();
            }

            boolean anyDoubleActiveDualPersonality = remainingDualPersonalities.stream()
                    .anyMatch(player -> dualPersonalityComponent.isDoubleActive(player.getUUID()));
            if (anyDoubleActiveDualPersonality
                    && (context.vanillaWinStatus() == GameFunctions.WinStatus.KILLERS
                    || context.vanillaWinStatus() == GameFunctions.WinStatus.PASSENGERS)) {
                /*
                 * “双活”是双重人格被迫解离后的最终独胜窗口。
                 * 即使配置允许和杀手 / 乘客共胜，只要还有双活中的双重人格，就不能让普通阵营先结算；
                 * 必须继续游戏，直到他们自己独立胜利或被淘汰。
                 */
                return VictoryApi.VictoryResult.keepRunning();
            }

            /*
             * 杀手胜利时，只有“双重人格配置允许与杀手共胜”并且至少一名存活双重人格属于非无辜阵营，
             * 才能把双重人格玩家追加成真正赢家。否则保活，避免他们被杀手胜利顺带结算掉。
             */
            boolean anyDualPersonalityKiller = remainingDualPersonalities.stream()
                    .anyMatch(player -> !gameWorld.isInnocent(player));
            if (context.vanillaWinStatus() == GameFunctions.WinStatus.KILLERS) {
                if (config.dualPersonalityWinWithKillers && anyDualPersonalityKiller) {
                    return VictoryApi.VictoryResult.vanillaWin(
                            GameFunctions.WinStatus.KILLERS,
                            VictoryApi.uuidsFromPlayers(remainingDualPersonalities)
                    );
                }
                return VictoryApi.VictoryResult.keepRunning();
            }

            /*
             * 乘客胜利同理：
             * 配置允许时，把所有存活双重人格追加为 extraWinnerUuids；
             * 配置不允许时，继续拖住普通乘客结算。
             */
            if (context.vanillaWinStatus() == GameFunctions.WinStatus.PASSENGERS) {
                if (config.dualPersonalityWinWithCivilians) {
                    return VictoryApi.VictoryResult.vanillaWin(
                            GameFunctions.WinStatus.PASSENGERS,
                            VictoryApi.uuidsFromPlayers(remainingDualPersonalities)
                    );
                }
                return VictoryApi.VictoryResult.keepRunning();
            }

            return VictoryApi.VictoryResult.pass();
        });
    }
}
