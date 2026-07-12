package pro.fazeclan.river.stupid_express.client.mixin.modifier.dual_personality;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.client.modifier.dual_personality.DualPersonalityClientState;

@Mixin(KeyboardHandler.class)
public class DualPersonalityDormantInputMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void stupidexpress$blockDormantSpectatorDetachKeys(
            long window,
            int key,
            int scancode,
            int action,
            int modifiers,
            CallbackInfo ci
    ) {
        Minecraft client = Minecraft.getInstance();
        if (!DualPersonalityClientState.isDormant(client.player)) {
            return;
        }
        if (!DualPersonalityClientState.isDormantBlockedKey(client, key, scancode)) {
            return;
        }

        /*
         * 休眠人格本质上是“仍被 Wathe 算作存活”的旁观相机。
         * 原版旁观的 Shift/数字键会尝试脱离附身或切换旁观目标；
         * 如果只靠服务端每 tick setCamera，玩家仍会看到一瞬间跳相机。
         * 因此客户端在按下/长按阶段直接吞掉这些键，释放事件则放行以免键位卡住。
         */
        DualPersonalityClientState.releaseDormantBlockedKeys(client);
        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
            ci.cancel();
        }
    }
}
