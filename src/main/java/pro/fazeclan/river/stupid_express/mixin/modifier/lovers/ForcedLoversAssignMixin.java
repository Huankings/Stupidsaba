package pro.fazeclan.river.stupid_express.mixin.modifier.lovers;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.harpymodloader.modded_murder.ModdedMurderGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.modifier.lovers.ForcedLoversManager;

import java.util.List;

@Mixin(ModdedMurderGameMode.class)
public class ForcedLoversAssignMixin {

    @Unique
    private boolean stupidexpress$forcedLoversApplied;

    @Inject(method = "assignModifiers", at = @At("HEAD"))
    private void stupidexpress$resetForcedLoversApplyFlag(
            int desiredRoleCount,
            ServerLevel serverLevel,
            GameWorldComponent gameWorldComponent,
            List<ServerPlayer> players,
            CallbackInfo ci
    ) {
        this.stupidexpress$forcedLoversApplied = false;
    }

    @Inject(
            method = "assignModifiers",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/ArrayList;isEmpty()Z",
                    ordinal = 0
            )
    )
    private void stupidexpress$applyForcedLoversBeforeModifierAnnouncement(
            int desiredRoleCount,
            ServerLevel serverLevel,
            GameWorldComponent gameWorldComponent,
            List<ServerPlayer> players,
            CallbackInfo ci
    ) {
        if (this.stupidexpress$forcedLoversApplied) {
            return;
        }
        this.stupidexpress$forcedLoversApplied = true;

        /*
         * 这个注入点位于 HarpyModLoader 完成随机/forceModifier 分配之后、
         * 给玩家发送“你获得了哪些词条”的 actionbar 公告之前。
         *
         * 因此 setlovers 队列在这里消费最合适：
         * 1. 能覆盖已经随机生成的恋人，避免多出随机配对；
         * 2. 能让 Harpy 原本的词条公告显示最终的 LOVERS 结果；
         * 3. players 参数就是 Wathe 本局真正参与游戏的玩家列表，
         *    正好满足“指定玩家未参与时本次指定作废”的需求。
         */
        ForcedLoversManager.ApplyResult result = ForcedLoversManager.consumeAndApplyPendingPairs(serverLevel, players);
        if (result.changedAnything()) {
            StupidExpress.LOGGER.info(
                    "已应用强制恋人配对：成功 {} 对，因玩家未参与而作废 {} 对。",
                    result.appliedPairs(),
                    result.skippedPairs()
            );
        }
    }
}
