package pro.fazeclan.river.stupid_express.client.modifier.dual_personality;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.TimeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityComponent;

import java.util.UUID;

/**
 * 双重人格客户端状态工具。
 *
 * <p>客户端 HUD、皮肤、名字和输入拦截都会读取世界组件。
 * 这里统一使用 GameStatus.ACTIVE，而不是 Wathe 的 isRunning()，
 * 因为 isRunning() 在 STOPPING 结算阶段也会返回 true；如果继续按 isRunning 判定，
 * 双活倒计时和副人格外观就会残留到游戏结束画面。</p>
 */
public final class DualPersonalityClientState {

    private DualPersonalityClientState() {
    }

    public static boolean isActiveRound(@Nullable Player player) {
        return player != null
                && player.level() != null
                && isActiveRound(player.level());
    }

    public static boolean isActiveRound(@Nullable Level level) {
        return level != null
                && GameWorldComponent.KEY.get(level).getGameStatus() == GameWorldComponent.GameStatus.ACTIVE;
    }

    public static boolean hasRoundRenderState(@Nullable Level level) {
        if (level == null) {
            return false;
        }

        GameWorldComponent.GameStatus status = GameWorldComponent.KEY.get(level).getGameStatus();
        /*
         * 皮肤/准星名字属于“本局身份展示”，不等同于技能和倒计时。
         * ACTIVE 时当然要显示；STOPPING 结算黑幕期间也要继续显示，
         * 等 Wathe finalizeGame 把玩家传回准备大厅并清组件后再消失。
         */
        return status == GameWorldComponent.GameStatus.ACTIVE
                || status == GameWorldComponent.GameStatus.STOPPING;
    }

    public static boolean isDormant(@Nullable Player player) {
        return isActiveRound(player)
                && DualPersonalityComponent.KEY.get(player.level()).isDormant(player.getUUID());
    }

    public static boolean isDoubleActive(@Nullable Player player) {
        return isActiveRound(player)
                && DualPersonalityComponent.KEY.get(player.level()).isDoubleActive(player.getUUID());
    }

    public static int getDoubleActiveTicks(@Nullable Player player) {
        return isDoubleActive(player)
                ? DualPersonalityComponent.KEY.get(player.level()).getDoubleActiveTicks(player.getUUID())
                : 0;
    }

    public static @Nullable UUID resolveSubAppearanceSource(@Nullable Player player) {
        if (player == null) {
            return null;
        }

        return resolveSubAppearanceSource(player.level(), player.getUUID());
    }

    public static @Nullable UUID resolveSubAppearanceSource(@Nullable Level level, @Nullable UUID playerUuid) {
        if (!hasRoundRenderState(level) || playerUuid == null) {
            return null;
        }

        DualPersonalityComponent.PairState pair = DualPersonalityComponent.KEY.get(level).getPair(playerUuid);
        if (pair == null || !pair.isSub(playerUuid)) {
            return null;
        }
        return pair.main;
    }

    public static boolean isDormantBlockedKey(Minecraft client, int key, int scancode) {
        if (client == null || client.options == null) {
            return false;
        }

        /*
         * 休眠人格处于旁观模式时，Shift 会尝试脱离当前附身相机，
         * 数字键会尝试打开/选择旁观目标。这里按玩家实际键位配置判断，
         * 所以即使玩家改过蹲键或快捷栏键，也能一起拦住。
         */
        if (client.options.keyShift.matches(key, scancode)) {
            return true;
        }
        for (KeyMapping hotbarKey : client.options.keyHotbarSlots) {
            if (hotbarKey.matches(key, scancode)) {
                return true;
            }
        }
        return false;
    }

    public static void releaseDormantBlockedKeys(Minecraft client) {
        if (client == null || client.options == null) {
            return;
        }

        /*
         * 玩家可能在变成休眠人格之前就已经按住 Shift。
         * 单纯拦截后续 keyPress 不会释放这个“已按下”状态，所以每 tick 主动清掉。
         */
        client.options.keyShift.setDown(false);
        for (KeyMapping hotbarKey : client.options.keyHotbarSlots) {
            hotbarKey.setDown(false);
        }
    }

    public static void resetTransientRenderState() {
        /*
         * 双活倒计时现在通过 Wathe 的 TimeHudApi 接管顶部时间。
         * 停局/结算边界仍需要清理 TimeRenderer 的滚动数字状态，
         * 但这里不再直接碰 view/offsetDelta，而是调用 Wathe 暴露的重置入口。
         */
        TimeRenderer.resetTransientState();
    }
}
