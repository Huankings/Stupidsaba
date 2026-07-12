package pro.fazeclan.river.stupid_express.client.modifier.dual_personality;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.packet.DualPersonalitySwitchC2SPacket;

/**
 * 双重人格客户端按键注册。
 *
 * <p>U 用于请求提前切换人格。
 * 双活阶段的透视不再注册 StupidExpress 自己的独立按键，
 * 而是直接复用 Wathe 原生本能键，避免玩家需要额外配置一套“临时本能”键位。</p>
 */
public final class DualPersonalityKeybinds {

    public static KeyMapping switchKey;

    private DualPersonalityKeybinds() {
    }

    public static void init() {
        // U 是主动切换键，普通轮换阶段只有活跃人格按下才会被服务端接受。
        switchKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.stupid_express.dual_personality_switch",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                "category.stupid_express"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (DualPersonalityClientState.isDormant(Minecraft.getInstance().player)) {
                /*
                 * 如果玩家在切入休眠人格前已经按住 Shift 或数字键，
                 * 后续不一定会立刻产生新的 keyPress 事件。
                 * 因此每 tick 主动释放一次，避免原版旁观相机用“已按下”状态脱离附身。
                 */
                DualPersonalityClientState.releaseDormantBlockedKeys(client);
            }

            while (switchKey.consumeClick()) {
                if (client.getConnection() != null) {
                    // 空包只表达“我按了切换键”，服务端会再次校验是否真能切换。
                    ClientPlayNetworking.send(new DualPersonalitySwitchC2SPacket());
                }
            }
        });
    }
}
