package pro.fazeclan.river.stupid_express.mixin.modifier.dual_personality;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityManager;

@Mixin(GunShootPayload.Receiver.class)
public class DualPersonalityRevolverPenaltyMixin {

    @WrapOperation(
            method = "receive",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/doctor4t/wathe/cca/GameWorldComponent;isInnocent(Lnet/minecraft/world/entity/player/Player;)Z",
                    ordinal = 0
            )
    )
    private boolean stupidexpress$ignorePenaltyWhenShootingInnocentDoubleActiveDualPersonality(
            GameWorldComponent gameWorldComponent,
            Player target,
            Operation<Boolean> original,
            GunShootPayload payload,
            ServerPlayNetworking.Context context
    ) {
        boolean targetNormallyInnocent = original.call(gameWorldComponent, target);
        ServerPlayer shooter = context.player();

        /*
         * 这里参考 NoellesRoles 巫毒师的处理方式：
         * Wathe 原版在左轮命中后，会先调用第一次 isInnocent(target) 判断目标是否好人；
         * 只要这次返回 true，就会继续执行掉枪、清空理智值，或者好人反噬自杀。
         *
         * 双活时刻下，双重人格已经被规则强制转成独立胜利目标。
         * 如果这名双重人格目标本身仍属于好人阵营，那么其他好人用左轮阻止他时，
         * 不应再被当作“好人误伤好人”处罚，所以在这一处把目标临时视为非好人。
         * 这个改判只影响左轮惩罚分支；后面的真正击杀流程仍会照常执行。
         */
        if (DualPersonalityManager.shouldSuppressInnocentRevolverPenalty(shooter, target, targetNormallyInnocent)) {
            return false;
        }

        return targetNormallyInnocent;
    }
}
