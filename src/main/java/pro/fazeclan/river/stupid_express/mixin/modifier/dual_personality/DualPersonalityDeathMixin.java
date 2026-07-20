package pro.fazeclan.river.stupid_express.mixin.modifier.dual_personality;

import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityManager;

@Mixin(GameFunctions.class)
public class DualPersonalityDeathMixin {

    @Inject(
            method = "killPlayer(Lnet/minecraft/world/entity/player/Player;ZLnet/minecraft/world/entity/player/Player;Lnet/minecraft/resources/ResourceLocation;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void stupidexpress$keepDormantPersonalityGameplayAlive(
            Player victim,
            boolean spawnBody,
            Player killer,
            ResourceLocation deathReason,
            CallbackInfo ci
    ) {
        /*
         * 休眠人格靠 PlayerLifeStateApi 的 aliveOverride 被 Wathe 视为“仍在局内”，
         * 所以手雷、静音手雷、狙击枪等范围/贯穿武器会把旁观中的休眠人格也扫进 killPlayer。
         *
         * 这里必须在 Wathe 真正处理死亡之前取消：
         * 1. 不生成尸体和死亡回放；
         * 2. 不给攻击者发击杀金币/任务收益；
         * 3. 不让 Simple Voice Chat 把休眠人格放进死者频道；
         * 4. 不消耗其它职业护盾或 Wathe 的精神护甲。
         *
         * 双活已经稳定开始后不会继续保命；Manager 只额外吞掉启动同一 tick 内、
         * 来自旧范围/贯穿目标快照的残留击杀，保证刚被解离出的休眠人格能真正落地参与双活。
         */
        if (victim instanceof ServerPlayer serverVictim && DualPersonalityManager.tryProtectDormantFatalDeath(serverVictim)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "killPlayer(Lnet/minecraft/world/entity/player/Player;ZLnet/minecraft/world/entity/player/Player;Lnet/minecraft/resources/ResourceLocation;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/api/PlayerLifeStateApi;clearAliveOverride(Lnet/minecraft/world/entity/player/Player;)V"
            ),
            cancellable = true
    )
    private static void stupidexpress$turnFatalDamageIntoDoubleActive(
            Player victim,
            boolean spawnBody,
            Player killer,
            ResourceLocation deathReason,
            CallbackInfo ci
    ) {
        /*
         * Wathe 的 killPlayer 会在这里清除特殊存活状态。
         * 我们赶在清除之前把“活跃人格死亡”改成“双活解离”，并取消本次死亡。
         * 双活超时死亡会被 Manager 的 FORCE_TIMEOUT_DEATHS 标记放行，不会被这里再次拦截。
         */
        if (victim instanceof ServerPlayer serverVictim && DualPersonalityManager.tryInterceptFatalDeath(serverVictim)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "killPlayer(Lnet/minecraft/world/entity/player/Player;ZLnet/minecraft/world/entity/player/Player;Lnet/minecraft/resources/ResourceLocation;)V",
            at = @At("RETURN")
    )
    private static void stupidexpress$rewardDoubleActiveKnifeKill(
            Player victim,
            boolean spawnBody,
            Player killer,
            ResourceLocation deathReason,
            CallbackInfo ci
    ) {
        /*
         * 只在 Wathe 完整处理完击杀之后再奖励时间。
         * 这样尸体、死亡记录、胜利检查等原流程都已经执行，不会因为奖励逻辑改变击杀本身。
         */
        if (killer instanceof ServerPlayer serverKiller && GameConstants.DeathReasons.KNIFE.equals(deathReason)) {
            DualPersonalityManager.onSuccessfulKill(serverKiller, victim, deathReason);
        }

        /*
         * 普通轮换阶段的休眠人格死亡现在会在 HEAD 被取消。
         * 这里保留一个兜底：如果未来某个兼容 mixin 绕过了前置取消、仍让休眠人格走完 Wathe 死亡流程，
         * 至少在 RETURN 处把 Simple Voice Chat 的死者频道副作用撤掉，避免语音隔离继续错位。
         */
        if (victim instanceof ServerPlayer serverVictim) {
            DualPersonalityManager.restoreDormantVoiceChannelAfterDeath(serverVictim);
        }
    }
}
