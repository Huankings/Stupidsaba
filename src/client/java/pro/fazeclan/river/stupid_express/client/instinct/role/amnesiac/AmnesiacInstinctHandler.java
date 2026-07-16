package pro.fazeclan.river.stupid_express.client.instinct.role.amnesiac;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.world.entity.player.Player;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.client.instinct.StupidExpressInstinctHandlers;
import pro.fazeclan.river.stupid_express.constants.SERoles;

public final class AmnesiacInstinctHandler {
    private AmnesiacInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(StupidExpress.id("instinct/amnesiac_to_killers"), StupidExpressInstinctHandlers.PRIORITY_MARK_COLOR, (viewer, target) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.level());
            if (!StupidExpress.CONFIG.rolesSection.amnesiacSection.amnesiacGlowsDifferently
                    || !(target instanceof Player targetPlayer)
                    || !gameWorld.isRole(targetPlayer, SERoles.AMNESIAC)
                    || GameFunctions.isPlayerSpectatingOrCreative(viewer)
                    || !WatheClient.isInstinctEnabled()
                    || !gameWorld.canUseKillerFeatures(viewer)) {
                return InstinctApi.HighlightResult.pass();
            }

            /*
             * 杀手本能看到失忆者时显示失忆者职业色。
             * 这条依赖 WatheClient.isInstinctEnabled()，所以属于可被 Convener 压制的本能链路。
             */
            return InstinctApi.HighlightResult.color(SERoles.AMNESIAC.color());
        });

        InstinctApi.registerHighlight(StupidExpress.id("instinct/amnesiac_bodies"), StupidExpressInstinctHandlers.PRIORITY_MARK_COLOR, (viewer, target) -> {
            if (!StupidExpress.CONFIG.rolesSection.amnesiacSection.bodiesGlowToAmnesiac
                    || !(target instanceof PlayerBodyEntity)
                    || !GameWorldComponent.KEY.get(viewer.level()).isRole(viewer, SERoles.AMNESIAC)
                    || GameFunctions.isPlayerSpectatingOrCreative(viewer)) {
                return InstinctApi.HighlightResult.pass();
            }

            /*
             * 失忆者看尸体是职业能力提示，不检查 isInstinctEnabled()。
             * 这样它不会被“只压制本能”的规则误关。
             */
            return InstinctApi.HighlightResult.color(SERoles.AMNESIAC.color());
        });
    }
}
