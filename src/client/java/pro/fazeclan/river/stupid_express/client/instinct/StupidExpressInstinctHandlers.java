package pro.fazeclan.river.stupid_express.client.instinct;

import dev.doctor4t.wathe.api.instinct.InstinctApi;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.client.modifier.dual_personality.DualPersonalityClientState;
import pro.fazeclan.river.stupid_express.client.role.convener.ConvenerColorHelper;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityComponent;
import pro.fazeclan.river.stupid_express.modifier.lovers.LoversPairComponent;
import pro.fazeclan.river.stupid_express.role.arsonist.cca.DousedPlayerComponent;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerDisguiseComponent;

import java.awt.Color;

public final class StupidExpressInstinctHandlers {
    private static final int PRIORITY_CONVENER_SUPPRESSION = 20000;
    private static final int PRIORITY_DUAL_PERSONALITY = 10000;
    private static final int PRIORITY_CONVENER_COLOR = 500;
    private static final int PRIORITY_ROLE_INSTINCT_COLOR = 100;
    private static final int PRIORITY_MARK_COLOR = 100;
    private static final int PRIORITY_FAKE_ARSONIST_GREEN = 50;
    private static final int OTHER_PLAYER_COLOR = 0x8a8a8a;

    private StupidExpressInstinctHandlers() {
    }

    public static void register() {
        registerAvailability();
        registerDualPersonalityHighlight();
        registerConvenerHighlight();
        registerRoleInstinctHighlights();
        registerModifierHighlights();
    }

    private static void registerAvailability() {
        InstinctApi.registerAvailability(StupidExpress.id("instinct/convener_disguise_suppression"), PRIORITY_CONVENER_SUPPRESSION, viewer -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(viewer.level());
            ConvenerDisguiseComponent disguiseComponent = ConvenerDisguiseComponent.KEY.get(viewer);
            if (GameFunctions.isPlayerAliveAndSurvival(viewer)
                    && disguiseComponent.getMorphTicks() > 0
                    && !gameWorldComponent.isRole(viewer, SERoles.CONVENER)) {
                /*
                 * 召集后的变形压制只通过 availability 返回 DISABLE。
                 * 这样所有依赖 WatheClient.isInstinctEnabled() 的本能透视都会被关闭，
                 * 包括 Dual Personality 双活本能；但不依赖本能键的独立职业标记不会被误伤。
                 */
                return InstinctApi.AvailabilityResult.DISABLE;
            }
            return InstinctApi.AvailabilityResult.PASS;
        });

        InstinctApi.registerAvailability(StupidExpress.id("instinct/dual_personality_double_active"), PRIORITY_DUAL_PERSONALITY, viewer -> {
            if (DualPersonalityClientState.isDoubleActive(viewer) && WatheClient.isInstinctInputActive()) {
                return InstinctApi.AvailabilityResult.ENABLE;
            }
            return InstinctApi.AvailabilityResult.PASS;
        });

        InstinctApi.registerAvailability(StupidExpress.id("instinct/role_instinct_availability"), InstinctApi.DEFAULT_PRIORITY, viewer -> {
            if (!WatheClient.isInstinctInputActive()) {
                return InstinctApi.AvailabilityResult.PASS;
            }
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(viewer.level());
            return gameWorldComponent.isRole(viewer, SERoles.CONVENER)
                    || gameWorldComponent.isRole(viewer, SERoles.ARSONIST)
                    || gameWorldComponent.isRole(viewer, SERoles.THIEF)
                    ? InstinctApi.AvailabilityResult.ENABLE
                    : InstinctApi.AvailabilityResult.PASS;
        });
    }

    private static void registerDualPersonalityHighlight() {
        InstinctApi.registerHighlight(StupidExpress.id("instinct/dual_personality_targets"), PRIORITY_DUAL_PERSONALITY, (viewer, target) -> {
            if (!DualPersonalityClientState.isDoubleActive(viewer) || !WatheClient.isInstinctEnabled()) {
                return InstinctApi.HighlightResult.pass();
            }
            if (!(target instanceof Player targetPlayer) || !GameFunctions.isPlayerAliveAndSurvival(targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }

            /*
             * 双活高亮现在检查 WatheClient.isInstinctEnabled()，而不是只检查本能输入。
             * 这正是 Convener 变形压制能压住双活本能的关键：Convener 高优先级 DISABLE 后，
             * 这里会直接 PASS，不再把全场玩家染成灰色。
             */
            DualPersonalityComponent dualComponent = DualPersonalityComponent.KEY.get(viewer.level());
            if (targetPlayer.getUUID().equals(dualComponent.getPartner(viewer.getUUID()))) {
                return InstinctApi.HighlightResult.color(SEModifiers.DUAL_PERSONALITY.color());
            }
            return InstinctApi.HighlightResult.color(OTHER_PLAYER_COLOR);
        });
    }

    private static void registerConvenerHighlight() {
        InstinctApi.registerHighlight(StupidExpress.id("instinct/convener_targets"), PRIORITY_CONVENER_COLOR, (viewer, target) -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(viewer.level());
            if (!gameWorldComponent.isRole(viewer, SERoles.CONVENER)
                    || GameFunctions.isPlayerSpectatingOrCreative(viewer)
                    || !WatheClient.isInstinctEnabled()) {
                return InstinctApi.HighlightResult.pass();
            }

            if (target instanceof PlayerBodyEntity) {
                return InstinctApi.HighlightResult.color(SERoles.CONVENER.color());
            }
            if (target instanceof Player targetPlayer && GameFunctions.isPlayerAliveAndSurvival(targetPlayer)) {
                return InstinctApi.HighlightResult.color(ConvenerColorHelper.getPlayerFlowColor(targetPlayer.getUUID()));
            }
            return InstinctApi.HighlightResult.pass();
        });
    }

    private static void registerRoleInstinctHighlights() {
        InstinctApi.registerHighlight(StupidExpress.id("instinct/arsonist_targets"), PRIORITY_ROLE_INSTINCT_COLOR, (viewer, target) -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(viewer.level());
            if (!(target instanceof Player targetPlayer)
                    || !gameWorldComponent.isRole(viewer, SERoles.ARSONIST)
                    || GameFunctions.isPlayerSpectatingOrCreative(viewer)
                    || !WatheClient.isInstinctEnabled()) {
                return InstinctApi.HighlightResult.pass();
            }

            DousedPlayerComponent doused = DousedPlayerComponent.KEY.get(targetPlayer);
            return InstinctApi.HighlightResult.color(doused.isDoused() ? SERoles.ARSONIST.color() : Color.GRAY.getRGB());
        });

        InstinctApi.registerHighlight(StupidExpress.id("instinct/thief_targets"), PRIORITY_ROLE_INSTINCT_COLOR, (viewer, target) -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(viewer.level());
            if (!gameWorldComponent.isRole(viewer, SERoles.THIEF)
                    || GameFunctions.isPlayerSpectatingOrCreative(viewer)
                    || !WatheClient.isInstinctEnabled()
                    || (!(target instanceof Player) && !(target instanceof ItemEntity))) {
                return InstinctApi.HighlightResult.pass();
            }

            return InstinctApi.HighlightResult.color(target == viewer ? SERoles.THIEF.color() : Color.GRAY.getRGB());
        });

        InstinctApi.registerHighlight(StupidExpress.id("instinct/fake_arsonist_green"), PRIORITY_FAKE_ARSONIST_GREEN, (viewer, target) -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(viewer.level());
            if (!(target instanceof Player targetPlayer)
                    || gameWorldComponent.isRole(viewer, SERoles.CONVENER)
                    || !gameWorldComponent.isRole(targetPlayer, SERoles.ARSONIST)
                    || GameFunctions.isPlayerSpectatingOrCreative(viewer)
                    || !WatheClient.isInstinctEnabled()) {
                return InstinctApi.HighlightResult.pass();
            }
            return InstinctApi.HighlightResult.color(Color.GREEN.getRGB());
        });
    }

    private static void registerModifierHighlights() {
        InstinctApi.registerHighlight(StupidExpress.id("instinct/lovers_partner"), PRIORITY_MARK_COLOR, (viewer, target) -> {
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
            return InstinctApi.HighlightResult.color(SEModifiers.LOVERS.color());
        });

        InstinctApi.registerHighlight(StupidExpress.id("instinct/amnesiac_to_killers"), PRIORITY_MARK_COLOR, (viewer, target) -> {
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(viewer.level());
            if (!StupidExpress.CONFIG.rolesSection.amnesiacSection.amnesiacGlowsDifferently
                    || !(target instanceof Player targetPlayer)
                    || !gameWorldComponent.isRole(targetPlayer, SERoles.AMNESIAC)
                    || GameFunctions.isPlayerSpectatingOrCreative(viewer)
                    || !WatheClient.isInstinctEnabled()
                    || !gameWorldComponent.canUseKillerFeatures(viewer)) {
                return InstinctApi.HighlightResult.pass();
            }
            return InstinctApi.HighlightResult.color(SERoles.AMNESIAC.color());
        });

        InstinctApi.registerHighlight(StupidExpress.id("instinct/amnesiac_bodies"), PRIORITY_MARK_COLOR, (viewer, target) -> {
            if (!StupidExpress.CONFIG.rolesSection.amnesiacSection.bodiesGlowToAmnesiac
                    || !(target instanceof PlayerBodyEntity)
                    || !GameWorldComponent.KEY.get(viewer.level()).isRole(viewer, SERoles.AMNESIAC)
                    || GameFunctions.isPlayerSpectatingOrCreative(viewer)) {
                return InstinctApi.HighlightResult.pass();
            }
            return InstinctApi.HighlightResult.color(SERoles.AMNESIAC.color());
        });

        InstinctApi.registerHighlight(StupidExpress.id("instinct/initiate_targets"), PRIORITY_MARK_COLOR, (viewer, target) -> {
            if (!(target instanceof Player targetPlayer)) {
                return InstinctApi.HighlightResult.pass();
            }
            GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(viewer.level());
            if (gameWorldComponent.isRole(targetPlayer, SERoles.INITIATE)
                    && gameWorldComponent.isRole(viewer, SERoles.INITIATE)) {
                return InstinctApi.HighlightResult.color(SERoles.INITIATE.color());
            }
            if (gameWorldComponent.isRole(targetPlayer, SERoles.INITIATE)
                    && !GameFunctions.isPlayerSpectatingOrCreative(viewer)
                    && WatheClient.isInstinctEnabled()
                    && gameWorldComponent.canUseKillerFeatures(viewer)) {
                return InstinctApi.HighlightResult.color(SERoles.INITIATE.color());
            }
            return InstinctApi.HighlightResult.pass();
        });
    }
}
