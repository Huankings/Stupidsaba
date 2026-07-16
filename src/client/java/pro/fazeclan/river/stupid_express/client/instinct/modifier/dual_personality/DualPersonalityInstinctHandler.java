package pro.fazeclan.river.stupid_express.client.instinct.modifier.dual_personality;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.world.entity.player.Player;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.client.instinct.StupidExpressInstinctHandlers;
import pro.fazeclan.river.stupid_express.client.modifier.dual_personality.DualPersonalityClientState;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityComponent;

public final class DualPersonalityInstinctHandler {
    private static final int OTHER_PLAYER_COLOR = 0x8a8a8a;

    private DualPersonalityInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerAvailability(StupidExpress.id("instinct/dual_personality_double_active"), StupidExpressInstinctHandlers.PRIORITY_DUAL_PERSONALITY, viewer -> {
            if (DualPersonalityClientState.isDoubleActive(viewer) && WatheClient.isInstinctInputActive()) {
                /*
                 * 双重人格双活期间用本能键进入专属透视。
                 * 这里只给资格，不直接染色；真正颜色由下面的 highlight 规则负责。
                 */
                return InstinctApi.AvailabilityResult.ENABLE;
            }
            return InstinctApi.AvailabilityResult.PASS;
        });

        InstinctApi.registerHighlight(StupidExpress.id("instinct/dual_personality_targets"), StupidExpressInstinctHandlers.PRIORITY_DUAL_PERSONALITY, (viewer, target) -> {
            if (!DualPersonalityClientState.isDoubleActive(viewer) || !WatheClient.isInstinctEnabled()) {
                return InstinctApi.HighlightResult.pass();
            }
            if (!(target instanceof Player targetPlayer) || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }

            /*
             * 这里必须检查 WatheClient.isInstinctEnabled()，而不是只看本能输入。
             * 这样 Convener 召集后的变形压制返回 DISABLE 时，双活本能也会一起被压住。
             */
            DualPersonalityComponent dualComponent = DualPersonalityComponent.KEY.get(viewer.level());
            if (targetPlayer.getUUID().equals(dualComponent.getPartner(viewer.getUUID()))) {
                return InstinctApi.HighlightResult.color(SEModifiers.DUAL_PERSONALITY.color());
            }
            return InstinctApi.HighlightResult.color(OTHER_PLAYER_COLOR);
        });
    }
}
