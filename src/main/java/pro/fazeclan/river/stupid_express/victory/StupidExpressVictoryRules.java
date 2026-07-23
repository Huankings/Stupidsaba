package pro.fazeclan.river.stupid_express.victory;

import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityVictoryRule;
import pro.fazeclan.river.stupid_express.modifier.lovers.LoversVictoryRule;
import pro.fazeclan.river.stupid_express.role.thief.ThiefVictoryRule;

/**
 * StupidExpress 接入 Wathe 公开胜利 API 的总入口。
 *
 * <p>旧版这些逻辑分散在多个 LoopMixin / KeepAliveMixin / CustomWinnerMixin 里：
 * 1. 先 mixin 到 MurderGameMode 拦截 winStatus；
 * 2. 再写自己的 CustomWinnerComponent；
 * 3. 最后 mixin 客户端结算文字和 didWin。
 *
 * <p>现在具体规则已经拆回各自分类：
 * modifier.lovers、modifier.dual_personality、role.thief。
 * 本类只负责集中初始化，避免 StupidExpress 主类需要知道每个职业 / 词条的内部细节。</p>
 */
public final class StupidExpressVictoryRules {
    private StupidExpressVictoryRules() {
    }

    public static void init() {
        /*
         * 初始化顺序也是兜底优先级的一部分：
         * VictoryApi 在 priority 相同的情况下会让“后注册”的规则先执行。
         * 目前这些默认优先级规则理论上不会同 tick 冲突，但仍保持一个稳定顺序，方便后续排查。
         */
        LoversVictoryRule.init();
        DualPersonalityVictoryRule.init();
        ThiefVictoryRule.init();
    }
}
