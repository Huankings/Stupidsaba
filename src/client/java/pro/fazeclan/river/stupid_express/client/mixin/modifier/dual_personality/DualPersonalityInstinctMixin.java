package pro.fazeclan.river.stupid_express.client.mixin.modifier.dual_personality;

import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pro.fazeclan.river.stupid_express.client.modifier.dual_personality.DualPersonalityClientState;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityComponent;

@Mixin(value = WatheClient.class, priority = 5000)
public class DualPersonalityInstinctMixin {

    // 双活透视中，另一人格用词条紫色显示，其他存活玩家统一灰色显示。
    private static final int OTHER_PLAYER_COLOR = 0x8a8a8a;

    @Inject(method = "isInstinctEnabled", at = @At("HEAD"), cancellable = true)
    private static void stupidexpress$enableDualActiveInstinct(CallbackInfoReturnable<Boolean> cir) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (DualPersonalityClientState.isDoubleActive(player)
                && WatheClient.isInstinctInputActive()) {
            /*
             * 双活透视直接复用 Wathe 原生本能键。
             * 这里调用 isInstinctInputActive()，而不是直接读取 KeyMapping，
             * 所以玩家通过 /instinct key 选择的“长按/开关”模式会原样生效。
             *
             * mixin 优先级高于 StupidExpress、NoellesRoles、kinssaba、StarryExpress
             * 这类常规职业本能 mixin；双活期间一旦本能键激活，就先返回 true 并取消，
             * 让好人阵营、杀手阵营和中立职业都统一进入双重人格的双活本能规则。
             */
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "getInstinctHighlight", at = @At("HEAD"), cancellable = true)
    private static void stupidexpress$highlightDualActiveTargets(Entity target, CallbackInfoReturnable<Integer> cir) {
        var player = Minecraft.getInstance().player;
        if (player == null || !WatheClient.isInstinctInputActive()) {
            return;
        }

        if (!DualPersonalityClientState.isDoubleActive(player)) {
            return;
        }
        if (!(target instanceof Player targetPlayer) || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)) {
            // 只高亮存活玩家，尸体/旁观/非玩家实体保持 Wathe 原逻辑。
            return;
        }

        DualPersonalityComponent dualComponent = DualPersonalityComponent.KEY.get(player.level());
        if (targetPlayer.getUUID().equals(dualComponent.getPartner(player.getUUID()))) {
            // 另一人格用双重人格紫色，方便双活混战时快速辨认队友。
            cir.setReturnValue(SEModifiers.DUAL_PERSONALITY.color());
        } else {
            /*
             * 这里必须 cancel，即使目标本身也会被杀手、纵火犯、召集者或其它扩展职业本能高亮。
             * 双活时刻的玩法目标是“两个双重人格单独求胜”，所以它的本能颜色优先级最高：
             * 除另一人格外，所有仍存活玩家都统一灰色，避免其它职业本能泄露额外阵营或职业信息。
             */
            cir.setReturnValue(OTHER_PLAYER_COLOR);
        }
        cir.cancel();
    }
}
