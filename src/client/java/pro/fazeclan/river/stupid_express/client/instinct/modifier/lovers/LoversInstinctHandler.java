package pro.fazeclan.river.stupid_express.client.instinct.modifier.lovers;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.world.entity.player.Player;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.client.instinct.StupidExpressInstinctHandlers;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;
import pro.fazeclan.river.stupid_express.modifier.lovers.LoversPairComponent;

public final class LoversInstinctHandler {
    private LoversInstinctHandler() {
    }

    public static void register() {
        InstinctApi.registerHighlight(StupidExpress.id("instinct/lovers_partner"), StupidExpressInstinctHandlers.PRIORITY_MARK_COLOR, (viewer, target) -> {
            if (!StupidExpress.CONFIG.modifiersSection.loversSection.loversGlowToEachother
                    || !(target instanceof Player potentialLover)
                    || GameFunctions.isPlayerSpectatingOrCreative(viewer)) {
                return InstinctApi.HighlightResult.pass();
            }

            WorldModifierComponent component = WorldModifierComponent.KEY.get(viewer.level());
            LoversPairComponent pairComponent = LoversPairComponent.KEY.get(viewer.level());
            if (!component.isModifier(viewer, SEModifiers.LOVERS)
                    || !component.isModifier(potentialLover, SEModifiers.LOVERS)
                    || !pairComponent.arePartnersOrFallback(
                    viewer.getUUID(),
                    potentialLover.getUUID(),
                    component.getAllWithModifier(SEModifiers.LOVERS)
            )) {
                return InstinctApi.HighlightResult.pass();
            }

            /*
             * 恋人词条只让“自己的伴侣”发光。
             * 多对恋人同时存在时，其他恋人对不会被暴露给当前玩家。
             */
            return InstinctApi.HighlightResult.color(SEModifiers.LOVERS.color());
        });
    }
}
