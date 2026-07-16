package pro.fazeclan.river.stupid_express.modifier.lovers;

import dev.doctor4t.wathe.api.win.VictoryApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;
import pro.fazeclan.river.stupid_express.victory.StupidExpressVictoryUtil;

import java.util.List;
import java.util.UUID;

/**
 * 恋人词条的独立胜利 / 共胜 / 保活规则。
 *
 * <p>拆到 modifier.lovers 包里，是为了让“恋人自己的胜利规则”跟配对组件、
 * HUD、强制分配等恋人相关源码放在同一个功能分类下，后续调平衡时不需要再去总注册器里翻。</p>
 */
public final class LoversVictoryRule {
    /*
     * 用户确认：恋人和双重人格同时在一组玩家身上时，恋人的独立胜利展示优先。
     * Wathe VictoryApi 会先执行高 priority 规则，因此恋人给 100，双重人格给 90。
     */
    private static final int PRIORITY = 100;

    private LoversVictoryRule() {
    }

    public static void init() {
        VictoryApi.registerRule(StupidExpress.id("victory/lovers"), PRIORITY, context -> {
            var config = StupidExpress.CONFIG.modifiersSection.loversSection;
            var world = context.world();
            var gameWorld = context.gameWorld();
            var modifierComponent = WorldModifierComponent.KEY.get(world);
            var pairComponent = LoversPairComponent.KEY.get(world);

            /*
             * VictoryApi 已经把“当前仍存活且处于生存模式的玩家”算好放进 context。
             * 这里先筛出还活着的恋人；如果没有恋人活着，本规则完全不接管本 tick。
             */
            List<ServerPlayer> remainingPlayers = context.alivePlayers();
            List<ServerPlayer> remainingLovers = remainingPlayers.stream()
                    .filter(player -> modifierComponent.isModifier(player, SEModifiers.LOVERS))
                    .toList();
            if (remainingLovers.isEmpty()) {
                return VictoryApi.VictoryResult.pass();
            }

            /*
             * 后面的殉情和伴侣共胜都只关心“仍活着的恋人 UUID”。
             * 先做成列表可以避免每次循环都重新 stream，同时也能传给 LoversPairComponent 的 fallback 逻辑。
             */
            List<UUID> remainingLoverUuids = remainingLovers.stream().map(ServerPlayer::getUUID).toList();

            VictoryApi.VictoryResult heartbreakResult = handleHeartbreakDeaths(remainingLovers, remainingLoverUuids, pairComponent);
            if (heartbreakResult.action() != VictoryApi.VictoryAction.PASS) {
                return heartbreakResult;
            }

            /*
             * 场上存活玩家全部都是恋人时，恋人不再依附杀手 / 乘客阵营，
             * 而是作为自己的独立胜利阵营写入 Wathe 自定义结算。
             */
            if (remainingPlayers.size() == remainingLovers.size()) {
                return StupidExpressVictoryUtil.customWin(
                        SEModifiers.LOVERS.identifier(),
                        SEModifiers.LOVERS.color(),
                        remainingLovers
                );
            }

            VictoryApi.VictoryResult killerCoWinResult = tryKillerCoWin(
                    remainingPlayers,
                    remainingLovers,
                    remainingLoverUuids,
                    pairComponent,
                    gameWorld,
                    config.loversWinWithKillers
            );
            if (killerCoWinResult.action() != VictoryApi.VictoryAction.PASS) {
                return killerCoWinResult;
            }

            /*
             * 如果原版本 tick 还没有任何阵营胜利，恋人规则不需要做保活。
             * 保活只应该在“Wathe 准备结束游戏”时介入，否则会无意义地拦住正常对局推进。
             */
            if (!context.hasVanillaWinner()) {
                return VictoryApi.VictoryResult.pass();
            }

            /*
             * 配置不允许恋人与乘客 / 杀手普通共胜时，只要还有恋人活着，
             * 就拦下原版 KILLERS 或 PASSENGERS 结算，让游戏继续等到：
             * 1. 恋人自己独立胜利；
             * 2. 恋人死亡；
             * 3. 配置允许的杀手恋人共胜条件成立。
             */
            if (!config.loversWinWithCivilians
                    && (context.vanillaWinStatus() == GameFunctions.WinStatus.KILLERS
                    || context.vanillaWinStatus() == GameFunctions.WinStatus.PASSENGERS)) {
                return VictoryApi.VictoryResult.keepRunning();
            }

            return VictoryApi.VictoryResult.pass();
        });
    }

    private static VictoryApi.VictoryResult handleHeartbreakDeaths(
            List<ServerPlayer> remainingLovers,
            List<UUID> remainingLoverUuids,
            LoversPairComponent pairComponent
    ) {
        /*
         * 恋人殉情必须放在所有胜利判断前面。
         * 如果本 tick 伴侣刚死亡，而原版同时算出了杀手 / 乘客胜利，直接结算会把“失去伴侣后应死亡”
         * 的状态漏掉。这里先处理死亡，再返回 keepRunning，让下一 tick 用最新存活人数重新仲裁。
         */
        for (ServerPlayer lover : remainingLovers) {
            UUID partnerUuid = pairComponent.getPartnerOrFallback(lover.getUUID(), remainingLoverUuids);
            if (partnerUuid == null || !remainingLoverUuids.contains(partnerUuid)) {
                CompoundTag extraDeathData = new CompoundTag();
                if (partnerUuid != null) {
                    /*
                     * 把伴侣 UUID 写进额外死亡数据，方便回放 / 死因文本知道这次死亡来自哪段恋人关系。
                     */
                    extraDeathData.putUUID("broken_heart_partner", partnerUuid);
                }
                GameFunctions.killPlayer(lover, true, null, StupidExpress.id("broken_heart"), extraDeathData);
                return VictoryApi.VictoryResult.keepRunning();
            }
        }
        return VictoryApi.VictoryResult.pass();
    }

    private static VictoryApi.VictoryResult tryKillerCoWin(
            List<ServerPlayer> remainingPlayers,
            List<ServerPlayer> remainingLovers,
            List<UUID> remainingLoverUuids,
            LoversPairComponent pairComponent,
            dev.doctor4t.wathe.cca.GameWorldComponent gameWorld,
            boolean loversWinWithKillers
    ) {
        if (!loversWinWithKillers) {
            return VictoryApi.VictoryResult.pass();
        }

        /*
         * 这是旧 LoversLoopMixin 的“杀手恋人共胜”语义：
         * 它不要求 Wathe 原版已经判定 KILLERS，因为只要无辜恋人还活着，
         * 原版就可能仍然认为 civilianAlive=true。
         *
         * 因此这里主动检查 StupidExpress 自己的条件，满足时直接返回 vanillaWin(KILLERS, extraWinners)。
         */
        for (ServerPlayer player : remainingLovers) {
            ServerPlayer partner = getLivingPartner(player, pairComponent, remainingLoverUuids, remainingLovers);
            if (partner == null) {
                continue;
            }
            /*
             * 两个恋人都是无辜阵营时，不能触发“杀手恋人共胜”。
             * 至少要有一名恋人属于非无辜阵营，才说明这对恋人和杀手胜利有关。
             */
            if (gameWorld.isInnocent(player) && gameWorld.isInnocent(partner)) {
                continue;
            }

            /*
             * 旧逻辑用 remainingPlayers.size() - 1 == remainingNonInnocent.size() 判断：
             * 场上除了一个无辜恋人以外，其余存活者都不是无辜阵营。
             * 这种局面下，普通杀手胜利会被无辜恋人阻挡，但 StupidExpress 允许这对恋人和杀手一起结算。
             */
            List<ServerPlayer> remainingNonInnocent = remainingPlayers.stream()
                    .filter(hostile -> !gameWorld.isInnocent(hostile))
                    .toList();
            if (remainingPlayers.size() - 1 != remainingNonInnocent.size()) {
                continue;
            }

            /*
             * 顶部和结算文案仍显示“杀手胜利”，但所有存活恋人都会写入 extraWinnerUuids。
             * Wathe 的 GameRoundEndComponent.didWin(...) 会先读取这份额外赢家列表，
             * 所以这些恋人会获得真正赢家音效、胜负记录和 TAB 赢家标记。
             */
            return VictoryApi.VictoryResult.vanillaWin(
                    GameFunctions.WinStatus.KILLERS,
                    VictoryApi.uuidsFromPlayers(remainingLovers)
            );
        }
        return VictoryApi.VictoryResult.pass();
    }

    private static ServerPlayer getLivingPartner(
            ServerPlayer player,
            LoversPairComponent pairComponent,
            List<UUID> remainingLoverUuids,
            List<ServerPlayer> remainingLovers
    ) {
        UUID partnerUuid = pairComponent.getPartnerOrFallback(player.getUUID(), remainingLoverUuids);
        if (partnerUuid == null) {
            return null;
        }
        for (ServerPlayer lover : remainingLovers) {
            if (partnerUuid.equals(lover.getUUID())) {
                return lover;
            }
        }
        return null;
    }
}
