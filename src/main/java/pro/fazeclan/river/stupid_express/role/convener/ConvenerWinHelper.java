package pro.fazeclan.river.stupid_express.role.convener;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.api.win.CustomVictory;
import dev.doctor4t.wathe.api.win.VictoryApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerPlayerComponent;

import java.util.List;

/**
 * 统一封装召集者的胜利判定与回合结束写入。
 * 这样服务端交互逻辑和 KeepAlive Mixin 都能复用一套出口，
 * 避免不同位置写出不一致的结算数据。
 */
public final class ConvenerWinHelper {

    private ConvenerWinHelper() {}

    /**
     * 根据本局参与人数计算召集者需要完成的召集次数。
     * 规则为：总人数的 1/3 向下取整，再额外 +1。
     */
    public static int getRequiredSummons(net.minecraft.world.level.Level level) {
        return Math.max(1, (getRoundPlayerCount(level) / 3) + 1);
    }

    /**
     * 统计本局实际参局人数。
     *
     * <p>这里不能单纯依赖 {@link GameFunctions#getReadyPlayerCount}：
     * 游戏开始后玩家已经离开 ready 区，那个值会掉到 0，
     * 从而把召集需求错误地算成 1。
     *
     * <p>因此这里改为取三种来源里的最大值：
     * 1. 已分配角色的玩家数量；
     * 2. 当前仍然存活的参局玩家数量；
     * 3. 开局前 ready 区人数。
     *
     * <p>这样无论是在身份刚分配、游戏刚开始，还是中后期有人死亡之后，
     * 都不会把“总参局人数”错误地下掉成 1。</p>
     */
    public static int getRoundPlayerCount(net.minecraft.world.level.Level level) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(level);
        int assignedRoleCount = gameWorldComponent.getRoles().size();
        int alivePlayerCount = Math.toIntExact(level.players().stream().filter(GameFunctions::isPlayerAliveAndSurvival).count());
        int readyPlayerCount = GameFunctions.getReadyPlayerCount(level);
        return Math.max(1, Math.max(assignedRoleCount, Math.max(alivePlayerCount, readyPlayerCount)));
    }

    /**
     * 将召集者组件里的“所需召集次数”同步刷新为本局真实值。
     *
     * <p>只在数值发生变化时才同步，避免无意义的网络包刷屏。</p>
     */
    public static void refreshRequiredSummons(ServerPlayer convener) {
        ConvenerPlayerComponent convenerComponent = ConvenerPlayerComponent.KEY.get(convener);
        int requiredSummons = getRequiredSummons(convener.level());
        if (convenerComponent.getRequiredSummons() == requiredSummons) {
            return;
        }

        convenerComponent.setRequiredSummons(requiredSummons);
        convenerComponent.sync();
    }

    /**
     * 判断当前场上是否还有活着的召集者。
     */
    public static ServerPlayer getLivingConvener(ServerLevel serverLevel, GameWorldComponent gameWorldComponent) {
        for (ServerPlayer player : serverLevel.getPlayers(GameFunctions::isPlayerAliveAndSurvival)) {
            if (gameWorldComponent.isRole(player, SERoles.CONVENER)) {
                return player;
            }
        }
        return null;
    }

    /**
     * 当召集者成为唯一存活者时立即触发单独阵营胜利。
     */
    public static boolean trySingleSurvivorWin(ServerLevel serverLevel, GameWorldComponent gameWorldComponent) {
        List<ServerPlayer> alivePlayers = serverLevel.getPlayers(GameFunctions::isPlayerAliveAndSurvival);
        if (alivePlayers.size() != 1) {
            return false;
        }

        ServerPlayer winner = alivePlayers.getFirst();
        if (!gameWorldComponent.isRole(winner, SERoles.CONVENER)) {
            return false;
        }

        declareConvenerWin(serverLevel, winner);
        return true;
    }

    /**
     * 将结算面板、胜负文本和胜者列表全部切到召集者自己的单独阵营胜利文案。
     */
    public static void declareConvenerWin(ServerLevel serverLevel, ServerPlayer winner) {
        /*
         * 召集者可能从交互逻辑中立即达成胜利，也可能由 VictoryApi 的每 tick 规则判定胜利。
         * 两条路径都走同一个 Wathe 公开出口，避免再次出现服务端赢家、客户端公告和结算分组不同步。
         */
        VictoryApi.endGameWithCustomVictory(
                serverLevel,
                CustomVictory.of(SERoles.CONVENER.identifier(), SERoles.CONVENER.color(), List.of((Player) winner))
        );
    }
}
