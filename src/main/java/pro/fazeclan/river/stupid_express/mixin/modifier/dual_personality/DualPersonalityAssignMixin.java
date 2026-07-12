package pro.fazeclan.river.stupid_express.mixin.modifier.dual_personality;

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
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityManager;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.ForcedDualPersonalityManager;

import java.util.List;

@Mixin(ModdedMurderGameMode.class)
public class DualPersonalityAssignMixin {

    /*
     * assignModifiers 内部可能多次经过我们选择的注入点。
     * 这个标记保证一局只消费一次强制双重人格队列。
     */
    @Unique
    private boolean stupidexpress$forcedDualPersonalityApplied;

    @Inject(method = "assignModifiers", at = @At("HEAD"))
    private void stupidexpress$prepareDualPersonalityModifierLimit(
            int desiredRoleCount,
            ServerLevel serverLevel,
            GameWorldComponent gameWorldComponent,
            List<ServerPlayer> players,
            CallbackInfo ci
    ) {
        /*
         * Harpy 在分配词条前读取 MODIFIER_MAX。
         * 这里按本局参局人数刷新 dual_personality 是否进入随机池，
         * 同时重置“强制队列是否已消费”的本局标记。
         */
        this.stupidexpress$forcedDualPersonalityApplied = false;
        DualPersonalityManager.refreshModifierMaximum(serverLevel, players);
    }

    @Inject(
            method = "assignModifiers",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/ArrayList;isEmpty()Z",
                    ordinal = 0
            )
    )
    private void stupidexpress$applyForcedDualPersonalityBeforeModifierAnnouncement(
            int desiredRoleCount,
            ServerLevel serverLevel,
            GameWorldComponent gameWorldComponent,
            List<ServerPlayer> players,
            CallbackInfo ci
    ) {
        if (this.stupidexpress$forcedDualPersonalityApplied) {
            return;
        }
        this.stupidexpress$forcedDualPersonalityApplied = true;

        /*
         * 这个注入点位于词条列表已基本确定、但还没向玩家播报之前。
         * 因此强制配对可以覆盖随机结果，并且播报/HUD 会看到最终正确的双重人格词条。
         */
        ForcedDualPersonalityManager.ApplyResult result = ForcedDualPersonalityManager.consumeAndApplyPendingPairs(serverLevel, players);
        if (result.changedAnything()) {
            StupidExpress.LOGGER.info(
                    "已应用强制双重人格配对：成功 {} 对，因玩家未参与而作废 {} 对。",
                    result.appliedPairs(),
                    result.skippedPairs()
            );
        }
    }
}
