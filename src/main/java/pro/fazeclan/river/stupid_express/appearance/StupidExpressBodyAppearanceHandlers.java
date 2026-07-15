package pro.fazeclan.river.stupid_express.appearance;

import dev.doctor4t.wathe.api.appearance.BodyAppearanceApi;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityComponent;

/**
 * StupidExpress 接入 Wathe 尸体外观 API 的统一注册处。
 *
 * <p>这里放 common/server 侧规则，因为尸体生成时要把“看起来像谁”的 UUID
 * 写进 Wathe 的 PlayerBodyEntity 同步数据里；真正的尸体 owner 仍然是死亡玩家本人。</p>
 */
public final class StupidExpressBodyAppearanceHandlers {
    /**
     * 双重人格只是低优先级的“身份外观伪装”。
     * 如果以后有更强的死亡瞬间外观规则，可以用更高 priority 覆盖它。
     */
    private static final int PRIORITY_DUAL_PERSONALITY = -100;

    private StupidExpressBodyAppearanceHandlers() {
    }

    public static void register() {
        registerDualPersonalityBodyAppearance();
    }

    private static void registerDualPersonalityBodyAppearance() {
        BodyAppearanceApi.register(
                StupidExpress.id("appearance/body/dual_personality"),
                PRIORITY_DUAL_PERSONALITY,
                (victim, killer, deathReason) -> {
                    DualPersonalityComponent.PairState pair =
                            DualPersonalityComponent.KEY.get(victim.level()).getPair(victim.getUUID());
                    if (pair == null || !pair.isSub(victim.getUUID())) {
                        return null;
                    }

                    /*
                     * 副人格死亡时，尸体肉眼显示为主人格外观。
                     * 注意这里只返回“外观来源 UUID”，Wathe 的尸体真实 owner 不会改变，
                     * 因此验尸、复活、回放等读取 body.getPlayerUuid() 的逻辑仍然能拿到真正死者。
                     */
                    return pair.main;
                }
        );
    }
}
