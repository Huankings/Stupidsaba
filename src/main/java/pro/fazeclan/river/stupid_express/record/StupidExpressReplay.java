package pro.fazeclan.river.stupid_express.record;

import dev.doctor4t.wathe.record.replay.ReplayRegistry;
import pro.fazeclan.river.stupid_express.StupidExpress;

/**
 * stupidexpress 的回放总入口。
 *
 * <p>这里统一负责两件事：</p>
 * <p>1. 注册 stupidexpress 专属的回放格式化器；</p>
 * <p>2. 让各个职业 / 词条逻辑文件只需要在“事件真正发生”的地方记一笔，
 * 不需要分散地关心回放注册和长期状态追踪。</p>
 */
public final class StupidExpressReplay {

    public static final net.minecraft.resources.ResourceLocation THIEF_ATTEMPT_EVENT = StupidExpress.id("thief_attempt");
    public static final net.minecraft.resources.ResourceLocation THIEF_SUCCESS_EVENT = StupidExpress.id("thief_success");
    public static final net.minecraft.resources.ResourceLocation THIEF_FAIL_EVENT = StupidExpress.id("thief_fail");
    public static final net.minecraft.resources.ResourceLocation DUAL_ACTIVE_STARTED_EVENT = StupidExpress.id("dual_active_started");
    public static final net.minecraft.resources.ResourceLocation BROKEN_HEART_DEATH_REASON = StupidExpress.id("broken_heart");

    private StupidExpressReplay() {
    }

    public static void init() {
        registerReplayFormatters();
    }

    private static void registerReplayFormatters() {
        ReplayRegistry.registerGlobalEventFormatter(THIEF_ATTEMPT_EVENT, StupidExpressReplayFormatters::formatThiefAttempt);
        ReplayRegistry.registerGlobalEventFormatter(THIEF_SUCCESS_EVENT, StupidExpressReplayFormatters::formatThiefSuccess);
        ReplayRegistry.registerGlobalEventFormatter(THIEF_FAIL_EVENT, StupidExpressReplayFormatters::formatThiefFail);
        ReplayRegistry.registerGlobalEventFormatter(DUAL_ACTIVE_STARTED_EVENT, StupidExpressReplayFormatters::formatDualActiveStarted);
        ReplayRegistry.registerDeathReasonFormatter(BROKEN_HEART_DEATH_REASON, StupidExpressReplayFormatters::formatBrokenHeartDeath);
        ReplayRegistry.registerDeathReasonFormatter(pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityManager.DOUBLE_ACTIVE_TIMEOUT_DEATH_REASON, StupidExpressReplayFormatters::formatDualActiveTimeoutDeath);
    }
}
