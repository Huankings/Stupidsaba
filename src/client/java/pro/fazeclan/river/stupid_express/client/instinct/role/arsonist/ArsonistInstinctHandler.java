package pro.fazeclan.river.stupid_express.client.instinct.role.arsonist;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.world.entity.player.Player;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.client.instinct.StupidExpressInstinctHandlers;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.role.arsonist.cca.DousedPlayerComponent;

import java.awt.Color;

public final class ArsonistInstinctHandler {
    private ArsonistInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerAvailability(StupidExpress.id("instinct/arsonist_availability"), InstinctApi.DEFAULT_PRIORITY, viewer -> {
            if (GameWorldComponent.KEY.get(viewer.level()).isRole(viewer, SERoles.ARSONIST)
                    && WatheClient.isInstinctInputActive()) {
                return InstinctApi.AvailabilityResult.ENABLE;
            }
            return InstinctApi.AvailabilityResult.PASS;
        });

        InstinctApi.registerHighlight(StupidExpress.id("instinct/arsonist_targets"), StupidExpressInstinctHandlers.PRIORITY_ROLE_INSTINCT_COLOR, (viewer, target) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.level());
            if (!(target instanceof Player targetPlayer)
                    || !gameWorld.isRole(viewer, SERoles.ARSONIST)
                    || GameFunctions.isPlayerSpectatingOrCreative(viewer)
                    || !WatheClient.isInstinctEnabled()) {
                return InstinctApi.HighlightResult.pass();
            }

            /*
             * 纵火犯开本能后：已浇油目标显示纵火犯色，未浇油目标显示灰色。
             * 这属于按本能键开启的职业本能，所以必须依赖 WatheClient.isInstinctEnabled()。
             */
            DousedPlayerComponent doused = DousedPlayerComponent.KEY.get(targetPlayer);
            return InstinctApi.HighlightResult.color(doused.isDoused() ? SERoles.ARSONIST.color() : Color.GRAY.getRGB());
        });

        InstinctApi.registerHighlight(StupidExpress.id("instinct/fake_arsonist_green"), StupidExpressInstinctHandlers.PRIORITY_FAKE_ARSONIST_GREEN, (viewer, target) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.level());
            if (!(target instanceof Player targetPlayer)
                    || gameWorld.isRole(viewer, SERoles.CONVENER)
                    || !gameWorld.isRole(targetPlayer, SERoles.ARSONIST)
                    || GameFunctions.isPlayerSpectatingOrCreative(viewer)
                    || !WatheClient.isInstinctEnabled()) {
                return InstinctApi.HighlightResult.pass();
            }
            /*
             * 非召集者的本能看到纵火犯时沿用旧逻辑显示为绿色伪装色。
             * 召集者单独有全员流动色逻辑，所以这里显式避开召集者。
             */
            return InstinctApi.HighlightResult.color(Color.GREEN.getRGB());
        });
    }
}
