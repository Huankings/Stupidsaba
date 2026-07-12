package pro.fazeclan.river.stupid_express.record;

import dev.doctor4t.wathe.record.GameRecordEvent;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.replay.DefaultReplayFormatters;
import dev.doctor4t.wathe.record.replay.ReplayGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * stupidexpress 自己的回放文案格式化器。
 *
 * <p>这里遵循和 noellesroles 一样的思路：
 * Wathe 本体只负责统一的事件采集与时间线播放，
 * stupidexpress 专属句式则单独放在自己的 formatter 里维护，
 * 这样不会把扩展模组的文案逻辑污染进 Wathe 本体。</p>
 */
public final class StupidExpressReplayFormatters {
    private StupidExpressReplayFormatters() {
    }

    private static @Nullable Component playerFromKey(GameRecordEvent event, GameRecordManager.MatchRecord match, String key) {
        if (!event.data().contains(key)) {
            return null;
        }
        return ReplayGenerator.formatPlayerName(event.data().getUUID(key), ReplayGenerator.getPlayerInfoCache(match));
    }

    private static @Nullable Component actorText(GameRecordEvent event, GameRecordManager.MatchRecord match) {
        return playerFromKey(event, match, "actor");
    }

    private static @Nullable Component targetText(GameRecordEvent event, GameRecordManager.MatchRecord match) {
        return playerFromKey(event, match, "target");
    }

    @Nullable
    public static Component formatAmnesiacRoleStolen(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerLevel world) {
        Component actor = actorText(event, match);
        Component victim = playerFromKey(event, match, "corpse_owner");
        if (actor == null || victim == null) {
            return null;
        }
        return Component.translatable("replay.global.stupid_express.amnesiac_role_stolen", actor, victim);
    }

    @Nullable
    public static Component formatArsonistDoused(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerLevel world) {
        Component actor = actorText(event, match);
        Component target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Component.translatable("replay.global.stupid_express.arsonist_doused", actor, target);
    }

    @Nullable
    public static Component formatArsonistLighterCooldownStarted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerLevel world) {
        Component actor = actorText(event, match);
        return actor == null ? null : Component.translatable("replay.global.stupid_express.arsonist_lighter_cooldown_started", actor);
    }

    @Nullable
    public static Component formatArsonistLighterCooldownFinished(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerLevel world) {
        Component actor = actorText(event, match);
        return actor == null ? null : Component.translatable("replay.global.stupid_express.arsonist_lighter_cooldown_finished", actor);
    }

    @Nullable
    public static Component formatAvariciousStoleCoins(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerLevel world) {
        Component actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        return Component.translatable("replay.global.stupid_express.avaricious_stole_coins", actor, event.data().getInt("amount"));
    }

    @Nullable
    public static Component formatNecromancerRevived(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerLevel world) {
        Component actor = actorText(event, match);
        Component revived = playerFromKey(event, match, "revived_player");
        if (actor == null || revived == null) {
            return null;
        }
        return Component.translatable("replay.global.stupid_express.necromancer_revived", actor, revived);
    }

    @Nullable
    public static Component formatThiefAttempt(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerLevel world) {
        Component actor = actorText(event, match);
        Component target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Component.translatable("replay.global.stupid_express.thief_attempt", actor, target);
    }

    @Nullable
    public static Component formatThiefSuccess(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerLevel world) {
        Component actor = actorText(event, match);
        Component target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Component.translatable("replay.global.stupid_express.thief_success", actor, target, ReplayGenerator.formatItemName(event.data(), world));
    }

    @Nullable
    public static Component formatThiefFail(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerLevel world) {
        Component actor = actorText(event, match);
        Component target = playerFromKey(event, match, "target_player");
        if (actor == null || target == null) {
            return null;
        }
        return Component.translatable("replay.global.stupid_express.thief_fail", actor, target);
    }

    @Nullable
    public static Component formatConvenerSummon(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerLevel world) {
        Component actor = actorText(event, match);
        Component corpseOwner = playerFromKey(event, match, "corpse_owner");
        if (actor == null || corpseOwner == null) {
            return null;
        }
        return Component.translatable(
                "replay.global.stupid_express.convener_summon",
                actor,
                corpseOwner,
                event.data().getInt("summon_count"),
                event.data().getInt("required_summons")
        );
    }

    @Nullable
    public static Component formatConvenerCounterShieldGained(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerLevel world) {
        Component actor = actorText(event, match);
        if (actor == null) {
            return null;
        }
        return Component.translatable(
                "replay.global.stupid_express.convener_counter_shield_gained",
                actor,
                event.data().getInt("current_layers")
        );
    }

    @Nullable
    public static Component formatConvenerVoodooImmunity(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerLevel world) {
        Component protectedPlayer = actorText(event, match);
        Component voodooCaster = playerFromKey(event, match, "voodoo_player");
        if (protectedPlayer == null || voodooCaster == null) {
            return null;
        }
        return Component.translatable(
                "replay.global.stupid_express.convener_voodoo_immunity",
                protectedPlayer,
                voodooCaster,
                Component.translatable("death_reason.noellesroles.voodoo")
        );
    }

    @Nullable
    public static Component formatConvenerCounterShieldBlocked(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerLevel world) {
        Component victim = targetText(event, match);
        if (victim == null) {
            return null;
        }

        Component damageName = DefaultReplayFormatters.formatBlockedDamageName(event.data(), world);
        Component attacker = actorText(event, match);
        if (attacker != null) {
            return Component.translatable("replay.shield_blocked.stupid_express.convener_counter_shield.by_item", victim, attacker, damageName);
        }
        return Component.translatable("replay.shield_blocked.stupid_express.convener_counter_shield.item", victim, damageName);
    }

    @Nullable
    public static Component formatBrokenHeartDeath(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerLevel world) {
        Component victim = targetText(event, match);
        if (victim == null) {
            return null;
        }

        Component partner = playerFromKey(event, match, "broken_heart_partner");
        if (partner == null) {
            return Component.translatable("replay.death.unknown.died", victim);
        }
        return Component.translatable("replay.death.stupid_express.broken_heart.died", victim, partner);
    }

    @Nullable
    public static Component formatDualActiveStarted(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerLevel world) {
        Component actor = actorText(event, match);
        return actor == null ? null : Component.translatable("replay.global.stupid_express.dual_active_started", actor);
    }

    @Nullable
    public static Component formatDualActiveTimeoutDeath(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerLevel world) {
        Component victim = targetText(event, match);
        return victim == null ? null : Component.translatable("replay.death.stupid_express.dual_active_timeout.died", victim);
    }
}
