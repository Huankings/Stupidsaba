package pro.fazeclan.river.stupid_express.appearance.modifiers.dual_personality;

import dev.doctor4t.wathe.api.appearance.BodyAppearanceApi;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityComponent;

/**
 * 双重人格词条接入 Wathe 尸体外观 API 的规则。
 *
 * <p>副人格死亡时，尸体肉眼显示为主人格外观；这里只返回外观来源 UUID，
 * Wathe 尸体真实 owner 仍然是死亡的副人格本人，验尸、复活、回放等逻辑不会被改写。</p>
 */
public final class DualPersonalityBodyAppearanceHandler {
    /**
     * 双重人格只是低优先级的身份外观伪装。
     * 如果以后有更强的死亡瞬间外观规则，可以使用更高 priority 覆盖它。
     */
    private static final int PRIORITY_DUAL_PERSONALITY = -100;

    private DualPersonalityBodyAppearanceHandler() {
    }

    public static void register() {
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
                     * 只在“死者确实是副人格”时返回主人格 UUID。
                     * 主人格死亡、没有配对、或组件还没同步完成时 PASS，交给 Wathe 使用真实死者外观。
                     */
                    return pair.main;
                }
        );
    }
}
