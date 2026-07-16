package pro.fazeclan.river.stupid_express.client.instinct.role.convener;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.world.entity.player.Player;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.client.instinct.StupidExpressInstinctHandlers;
import pro.fazeclan.river.stupid_express.client.role.convener.ConvenerColorHelper;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerDisguiseComponent;

public final class ConvenerInstinctHandler {
    private ConvenerInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerAvailability(StupidExpress.id("instinct/convener_disguise_suppression"), StupidExpressInstinctHandlers.PRIORITY_CONVENER_SUPPRESSION, viewer -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.level());
            ConvenerDisguiseComponent disguise = ConvenerDisguiseComponent.KEY.get(viewer);
            if (GameFunctions.isPlayerAliveAndSurvival(viewer)
                    && disguise.getMorphTicks() > 0
                    && !gameWorld.isRole(viewer, SERoles.CONVENER)) {
                /*
                 * 召集后的强制变形只压制“依赖本能开启”的透视。
                 * 这里通过 availability 返回 DISABLE，所有调用 WatheClient.isInstinctEnabled() 的本能链路都会被关掉；
                 * 但 Cook/Angel/Executioner 这类不依赖本能键的独立职业标记不会被误伤。
                 */
                return InstinctApi.AvailabilityResult.DISABLE;
            }
            return InstinctApi.AvailabilityResult.PASS;
        });

        InstinctApi.registerAvailability(StupidExpress.id("instinct/convener_availability"), InstinctApi.DEFAULT_PRIORITY, viewer -> {
            if (GameWorldComponent.KEY.get(viewer.level()).isRole(viewer, SERoles.CONVENER)
                    && WatheClient.isInstinctInputActive()) {
                /*
                 * 召集者自己的透视仍然是“按本能键才开启”的能力。
                 * 资格放在 0 优先级，和 Wathe 默认杀手本能平级，不额外压过其它职业规则。
                 */
                return InstinctApi.AvailabilityResult.ENABLE;
            }
            return InstinctApi.AvailabilityResult.PASS;
        });

        InstinctApi.registerHighlight(StupidExpress.id("instinct/convener_targets"), StupidExpressInstinctHandlers.PRIORITY_CONVENER_COLOR, (viewer, target) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.level());
            if (!gameWorld.isRole(viewer, SERoles.CONVENER)
                    || GameFunctions.isPlayerSpectatingOrCreative(viewer)
                    || !WatheClient.isInstinctEnabled()) {
                return InstinctApi.HighlightResult.pass();
            }

            if (target instanceof PlayerBodyEntity) {
                /*
                 * 尸体固定显示召集者职业色，和活人的流动色区分开。
                 */
                return InstinctApi.HighlightResult.color(SERoles.CONVENER.color());
            }
            if (target instanceof Player targetPlayer && GameFunctions.isPlayerAliveAndSurvival(targetPlayer)) {
                /*
                 * 活人统一使用召集者的流动配色，避免 Wathe 默认阵营/心情色泄露额外信息。
                 */
                return InstinctApi.HighlightResult.color(ConvenerColorHelper.getPlayerFlowColor(targetPlayer.getUUID()));
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
