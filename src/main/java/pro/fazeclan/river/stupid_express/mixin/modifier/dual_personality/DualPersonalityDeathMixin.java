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
    }
}
