package pro.fazeclan.river.stupid_express.appearance;

import pro.fazeclan.river.stupid_express.appearance.modifiers.dual_personality.DualPersonalityBodyAppearanceHandler;

/**
 * StupidExpress 接入 Wathe 尸体外观 API 的服务端总入口。
 *
 * <p>尸体外观是在 common/server 侧写入 Wathe 尸体同步数据的，因此不能放到 client 源集。
 * 具体词条规则拆成独立 handler，后续新增死亡瞬间外观规则时从这里聚合即可。</p>
 */
public final class StupidExpressBodyAppearanceHandlers {
    private StupidExpressBodyAppearanceHandlers() {
    }

    public static void register() {
        DualPersonalityBodyAppearanceHandler.register();
    }
}
