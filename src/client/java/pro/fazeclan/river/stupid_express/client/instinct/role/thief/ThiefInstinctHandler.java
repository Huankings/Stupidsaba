package pro.fazeclan.river.stupid_express.client.instinct.role.thief;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.client.instinct.StupidExpressInstinctHandlers;
import pro.fazeclan.river.stupid_express.constants.SERoles;

import java.awt.Color;

public final class ThiefInstinctHandler {
    private ThiefInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerAvailability(StupidExpress.id("instinct/thief_availability"), InstinctApi.DEFAULT_PRIORITY, viewer -> {
            if (GameFunctions.isPlayerAliveAndSurvival(viewer)
                    && GameWorldComponent.KEY.get(viewer.level()).isRole(viewer, SERoles.THIEF)
                    && WatheClient.isInstinctInputActive()) {
                /*
                 * 小偷本能只在存活时开启。
                 * 非存活玩家的物品/玩家描边要走观察者规则，避免被小偷灰色逻辑覆盖。
                 */
                return InstinctApi.AvailabilityResult.ENABLE;
            }
            return InstinctApi.AvailabilityResult.PASS;
        });

        InstinctApi.registerHighlight(StupidExpress.id("instinct/thief_targets"), StupidExpressInstinctHandlers.PRIORITY_ROLE_INSTINCT_COLOR, (viewer, target) -> {
            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.level());
            if (!gameWorld.isRole(viewer, SERoles.THIEF)
                    || GameFunctions.isPlayerSpectatingOrCreative(viewer)
                    || !WatheClient.isInstinctEnabled()
                    || (!(target instanceof Player) && !(target instanceof ItemEntity))) {
                return InstinctApi.HighlightResult.pass();
            }

            /*
             * 小偷本能只关心玩家和物品：自己显示盗贼色，其余目标统一灰色。
             * 这条规则完全依赖本能开启，因此会被 Convener availability DISABLE 正确压住。
             */
            return InstinctApi.HighlightResult.color(target == viewer ? SERoles.THIEF.color() : Color.GRAY.getRGB());
        });
    }
}
