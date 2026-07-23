package pro.fazeclan.river.stupid_express.client;

import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.world.entity.player.Player;
import pro.fazeclan.river.stupid_express.client.appearance.StupidExpressAppearanceHandlers;
import pro.fazeclan.river.stupid_express.client.instinct.StupidExpressInstinctHandlers;
import pro.fazeclan.river.stupid_express.client.modifier.dual_personality.DualPersonalityClientState;
import pro.fazeclan.river.stupid_express.client.modifier.dual_personality.DualPersonalityKeybinds;
import pro.fazeclan.river.stupid_express.client.modifier.dual_personality.DualPersonalityTimeHud;

public class StupidExpressClient implements ClientModInitializer {

    public static Player target;
    public static PlayerBodyEntity targetBody;

    @Override
    public void onInitializeClient() {
        StupidExpressInstinctHandlers.register();
        StupidExpressAppearanceHandlers.register();
        DualPersonalityTimeHud.register();

        // 双重人格的客户端瞬时状态只在当前对局内有效，跨局必须清掉，避免旧相机/倒计时残留。
        GameEvents.ON_GAME_START.register(gameMode -> {
            DualPersonalityClientState.resetTransientRenderState();
            DualPersonalityKeybinds.resetSyncedState();
        });
        GameEvents.ON_GAME_STOP.register(gameMode -> {
            /*
             * 顶部时间 HUD 已经通过 Wathe 的 TimeHudApi 接入。
             * 这里仍然主动清一次瞬时动画状态，保证停局/结算边界不会复用双活倒计时的旧滚动数字。
             */
            DualPersonalityClientState.resetTransientRenderState();
            DualPersonalityKeybinds.resetSyncedState();
        });
        GameEvents.ON_FINISH_FINALIZE.register((world, gameComponent) -> {
            DualPersonalityClientState.resetTransientRenderState();
            DualPersonalityKeybinds.resetSyncedState();
        });

        DualPersonalityKeybinds.init();

    }
}
