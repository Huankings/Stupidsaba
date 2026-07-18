package pro.fazeclan.river.stupid_express.client.appearance;

import pro.fazeclan.river.stupid_express.client.appearance.modifiers.dual_personality.DualPersonalityAppearanceHandler;
import pro.fazeclan.river.stupid_express.client.appearance.roles.convener.ConvenerAppearanceHandler;

/**
 * StupidExpress 接入 Wathe 玩家外观 / 准心名字 API 的总入口。
 *
 * <p>这里只保留注册顺序；召集者和双重人格的具体逻辑分别放到自己的 handler，
 * 避免后续新增职业时继续堆到同一个大类里。</p>
 */
public final class StupidExpressAppearanceHandlers {
    private StupidExpressAppearanceHandlers() {
    }

    public static void register() {
        ConvenerAppearanceHandler.register();
        DualPersonalityAppearanceHandler.register();
    }
}
