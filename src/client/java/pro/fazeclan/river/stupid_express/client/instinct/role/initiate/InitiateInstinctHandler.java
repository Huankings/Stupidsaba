package pro.fazeclan.river.stupid_express.client.instinct.role.initiate;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.world.entity.player.Player;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.client.instinct.StupidExpressInstinctHandlers;
import pro.fazeclan.river.stupid_express.constants.SERoles;

public final class InitiateInstinctHandler {
    private InitiateInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(StupidExpress.id("instinct/initiate_targets"), StupidExpressInstinctHandlers.PRIORITY_MARK_COLOR, (viewer, target) -> {
            if (!(target instanceof Player targetPlayer)
                    || !GameFunctions.isPlayerAliveAndSurvival(viewer)
                    || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }

            GameWorldComponent gameWorld = GameWorldComponent.KEY.get(viewer.level());
            if (gameWorld.isRole(targetPlayer, SERoles.INITIATE)
                    && gameWorld.isRole(viewer, SERoles.INITIATE)) {
                /*
                 * 入会者互相识别不依赖本能键，是词条/职业关系提示。
                 * 但仍然只对存活入会者开放；死亡后不能用这条高优先级标记覆盖观察者职业色。
                 */
                return InstinctApi.HighlightResult.color(SERoles.INITIATE.color());
            }
            if (gameWorld.isRole(targetPlayer, SERoles.INITIATE)
                    && WatheClient.isInstinctEnabled()
                    && gameWorld.canUseKillerFeatures(viewer)) {
                /*
                 * 杀手通过本能识别入会者时，仍必须依赖 isInstinctEnabled()。
                 */
                return InstinctApi.HighlightResult.color(SERoles.INITIATE.color());
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
