package pro.fazeclan.river.stupid_express.client.modifier.dual_personality;

import dev.doctor4t.wathe.api.time.TimeHudApi;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityManager;

/**
 * 双重人格词条的顶部时间 HUD 接入。
 *
 * <p>旧实现通过 mixin 直接取消 Wathe 的 TimeRenderer.renderHud，
 * 这会把“谁能接管顶部时间”写死在 mixin 优先级里。现在改为注册 Wathe 的 TimeHudApi provider：
 * 双活阶段返回 SHOW，普通阶段返回 PASS，让 Wathe 默认回合时间继续显示。</p>
 */
public final class DualPersonalityTimeHud {
    private static final int PRIORITY = TimeHudApi.DEFAULT_PRIORITY + 100;

    private DualPersonalityTimeHud() {
    }

    public static void register() {
        TimeHudApi.registerProvider(StupidExpress.id("dual_personality/double_active_time"), PRIORITY, player -> {
            /*
             * 只有“双活阶段”才接管屏幕正上方时间。
             * 这里不再关心 Wathe 的普通回合时间，也不直接操作 TimeRenderer.view，
             * 因为滚动数字动画、来源切换重置和最终绘制都交给 Wathe 统一处理。
             */
            int time = DualPersonalityClientState.getDoubleActiveTicks(player);
            if (time <= 0) {
                return TimeHudApi.TimeDisplay.pass();
            }

            /*
             * 使用双重人格词条色固定渲染，明确告诉玩家：
             * 现在看到的是双活剩余时间，而不是普通回合倒计时。
             */
            return TimeHudApi.TimeDisplay.showFixedColor(time, DualPersonalityManager.COLOR);
        });
    }
}
