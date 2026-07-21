package pro.fazeclan.river.stupid_express.record;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.replay.ReplayRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.constants.SEItems;
import pro.fazeclan.river.stupid_express.constants.SERoles;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * stupidexpress 的回放总入口。
 *
 * <p>这里统一负责两件事：</p>
 * <p>1. 注册 stupidexpress 专属的回放格式化器；</p>
 * <p>2. 维护那些不是“瞬时事件”的回放状态，例如纵火犯打火机冷却结束。</p>
 *
 * <p>这样各个职业逻辑文件里只需要在“事件真正发生”的地方记一笔，
 * 不需要分散地关心回放注册和长期状态追踪。</p>
 */
public final class StupidExpressReplay {

    public static final net.minecraft.resources.ResourceLocation AMNESIAC_ROLE_STOLEN_EVENT = StupidExpress.id("amnesiac_role_stolen");
    public static final net.minecraft.resources.ResourceLocation ARSONIST_DOUSED_EVENT = StupidExpress.id("arsonist_doused");
    public static final net.minecraft.resources.ResourceLocation ARSONIST_LIGHTER_COOLDOWN_STARTED_EVENT = StupidExpress.id("arsonist_lighter_cooldown_started");
    public static final net.minecraft.resources.ResourceLocation ARSONIST_LIGHTER_COOLDOWN_FINISHED_EVENT = StupidExpress.id("arsonist_lighter_cooldown_finished");
    public static final net.minecraft.resources.ResourceLocation THIEF_ATTEMPT_EVENT = StupidExpress.id("thief_attempt");
    public static final net.minecraft.resources.ResourceLocation THIEF_SUCCESS_EVENT = StupidExpress.id("thief_success");
    public static final net.minecraft.resources.ResourceLocation THIEF_FAIL_EVENT = StupidExpress.id("thief_fail");
    public static final net.minecraft.resources.ResourceLocation CONVENER_SUMMON_EVENT = StupidExpress.id("convener_summon");
    public static final net.minecraft.resources.ResourceLocation CONVENER_COUNTER_SHIELD_GAINED_EVENT = StupidExpress.id("convener_counter_shield_gained");
    public static final net.minecraft.resources.ResourceLocation CONVENER_VOODOO_IMMUNITY_EVENT = StupidExpress.id("convener_voodoo_immunity");
    public static final net.minecraft.resources.ResourceLocation DUAL_ACTIVE_STARTED_EVENT = StupidExpress.id("dual_active_started");
    public static final net.minecraft.resources.ResourceLocation CONVENER_COUNTER_SHIELD_SOURCE = StupidExpress.id("convener_counter_shield");
    public static final net.minecraft.resources.ResourceLocation BROKEN_HEART_DEATH_REASON = StupidExpress.id("broken_heart");

    /**
     * 正在等待“打火机冷却结束”回放的纵火犯集合。
     *
     * <p>开始冷却的瞬间会把玩家 UUID 放进来，
     * 后续每 tick 轮询，一旦冷却真的结束再补发“冷却完毕”事件。</p>
     */
    private static final Set<UUID> TRACKED_LIGHTER_COOLDOWNS = new HashSet<>();

    private StupidExpressReplay() {
    }

    public static void init() {
        registerReplayFormatters();
        registerCooldownTracker();
    }

    /**
     * 由职业逻辑在“打火机刚进入冷却”时调用。
     *
     * <p>这里只负责把玩家加入后续追踪队列，
     * 真正的“冷却结束”回放会在服务端 tick 中统一发出。</p>
     */
    public static void trackLighterCooldown(ServerPlayer player) {
        TRACKED_LIGHTER_COOLDOWNS.add(player.getUUID());
    }

    private static void registerReplayFormatters() {
        ReplayRegistry.registerGlobalEventFormatter(AMNESIAC_ROLE_STOLEN_EVENT, StupidExpressReplayFormatters::formatAmnesiacRoleStolen);
        ReplayRegistry.registerGlobalEventFormatter(ARSONIST_DOUSED_EVENT, StupidExpressReplayFormatters::formatArsonistDoused);
        ReplayRegistry.registerGlobalEventFormatter(ARSONIST_LIGHTER_COOLDOWN_STARTED_EVENT, StupidExpressReplayFormatters::formatArsonistLighterCooldownStarted);
        ReplayRegistry.registerGlobalEventFormatter(ARSONIST_LIGHTER_COOLDOWN_FINISHED_EVENT, StupidExpressReplayFormatters::formatArsonistLighterCooldownFinished);
        ReplayRegistry.registerGlobalEventFormatter(THIEF_ATTEMPT_EVENT, StupidExpressReplayFormatters::formatThiefAttempt);
        ReplayRegistry.registerGlobalEventFormatter(THIEF_SUCCESS_EVENT, StupidExpressReplayFormatters::formatThiefSuccess);
        ReplayRegistry.registerGlobalEventFormatter(THIEF_FAIL_EVENT, StupidExpressReplayFormatters::formatThiefFail);
        ReplayRegistry.registerGlobalEventFormatter(CONVENER_SUMMON_EVENT, StupidExpressReplayFormatters::formatConvenerSummon);
        ReplayRegistry.registerGlobalEventFormatter(CONVENER_COUNTER_SHIELD_GAINED_EVENT, StupidExpressReplayFormatters::formatConvenerCounterShieldGained);
        ReplayRegistry.registerGlobalEventFormatter(CONVENER_VOODOO_IMMUNITY_EVENT, StupidExpressReplayFormatters::formatConvenerVoodooImmunity);
        ReplayRegistry.registerGlobalEventFormatter(DUAL_ACTIVE_STARTED_EVENT, StupidExpressReplayFormatters::formatDualActiveStarted);
        ReplayRegistry.registerShieldSourceFormatter(CONVENER_COUNTER_SHIELD_SOURCE, StupidExpressReplayFormatters::formatConvenerCounterShieldBlocked);
        ReplayRegistry.registerDeathReasonFormatter(BROKEN_HEART_DEATH_REASON, StupidExpressReplayFormatters::formatBrokenHeartDeath);
        ReplayRegistry.registerDeathReasonFormatter(pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityManager.DOUBLE_ACTIVE_TIMEOUT_DEATH_REASON, StupidExpressReplayFormatters::formatDualActiveTimeoutDeath);
    }

    private static void registerCooldownTracker() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (TRACKED_LIGHTER_COOLDOWNS.isEmpty()) {
                return;
            }

            Iterator<UUID> iterator = TRACKED_LIGHTER_COOLDOWNS.iterator();
            while (iterator.hasNext()) {
                UUID playerUuid = iterator.next();
                ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);

                // 玩家离线、不是纵火犯、对局已结束时都直接停止追踪，避免脏数据残留。
                if (player == null) {
                    iterator.remove();
                    continue;
                }
                if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR || player.gameMode.getGameModeForPlayer() == GameType.CREATIVE) {
                    iterator.remove();
                    continue;
                }

                GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.level());
                if (!gameWorldComponent.isRunning() || !gameWorldComponent.isRole(player, SERoles.ARSONIST) || !GameFunctions.isPlayerAliveAndSurvival(player)) {
                    iterator.remove();
                    continue;
                }

                if (player.getCooldowns().isOnCooldown(SEItems.LIGHTER)) {
                    continue;
                }

                GameRecordManager.recordGlobalEvent(player.serverLevel(), ARSONIST_LIGHTER_COOLDOWN_FINISHED_EVENT, player, null);
                iterator.remove();
            }
        });
    }
}
