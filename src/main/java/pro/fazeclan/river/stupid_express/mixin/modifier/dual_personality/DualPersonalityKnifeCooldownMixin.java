package pro.fazeclan.river.stupid_express.mixin.modifier.dual_personality;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.KnifeStabPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityComponent;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityManager;

@Mixin(KnifeStabPayload.Receiver.class)
public class DualPersonalityKnifeCooldownMixin {

    @Inject(method = "receive", at = @At("TAIL"))
    private void stupidexpress$shortenDoubleActiveKnifeCooldown(
            @NotNull KnifeStabPayload payload,
            ServerPlayNetworking.@NotNull Context context,
            CallbackInfo ci
    ) {
        ServerPlayer player = context.player();
        if (DualPersonalityComponent.KEY.get(player.level()).isDoubleActive(player.getUUID())) {
            /*
             * Wathe 原本会在匕首命中后设置自己的冷却。
             * 注入 TAIL 后再写一次较短冷却，相当于只在双活阶段覆盖成 1 秒。
             */
            player.getCooldowns().addCooldown(WatheItems.KNIFE, DualPersonalityManager.DOUBLE_ACTIVE_KNIFE_COOLDOWN_TICKS);
        }
    }
}
