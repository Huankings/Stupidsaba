package pro.fazeclan.river.stupid_express.client.appearance;

import dev.doctor4t.wathe.api.client.appearance.PlayerAppearanceApi;
import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.client.modifier.dual_personality.DualPersonalityClientState;
import pro.fazeclan.river.stupid_express.client.role.convener.ConvenerDisguiseResolver;
import pro.fazeclan.river.stupid_express.role.convener.ConvenerCommunicationHelper;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerDisguiseComponent;

import java.util.UUID;

/**
 * StupidExpress 接入 Wathe 玩家外观 / 准心名字 API 的统一注册处。
 *
 * <p>这里按“职业 / 词条”分组注册：
 * 1. 召集者：高优先级，负责限时全员伪装和召集者自己的主动伪装；
 * 2. 双重人格：低优先级，只在没有其它主动伪装时让副人格显示成主人格。</p>
 */
public final class StupidExpressAppearanceHandlers {
    /**
     * 用户确认：召集者召集尸体后的限时变形高于普通主动变形与双重人格。
     * NoellesRoles 的灵术师本地出窍会用更高优先级，因此仍然能盖过这里。
     */
    private static final int PRIORITY_CONVENER = 1000;
    /**
     * 双重人格低于 Morphling / Controller / Coroner / Convener 等主动变形。
     */
    private static final int PRIORITY_DUAL_PERSONALITY = -100;

    private StupidExpressAppearanceHandlers() {
    }

    public static void register() {
        registerConvenerAppearance();
        registerDualPersonalityAppearance();
    }

    private static void registerConvenerAppearance() {
        PlayerAppearanceApi.registerPlayerSkin(
                StupidExpress.id("appearance/player/convener"),
                PRIORITY_CONVENER,
                player -> resolveConvenerSkin(player)
        );

        RoleNameHudApi.registerName(
                StupidExpress.id("role_name/convener/name"),
                PRIORITY_CONVENER,
                (viewer, target, originalName) -> resolveConvenerName(target, originalName)
        );

        RoleNameHudApi.registerCohortHint(
                StupidExpress.id("role_name/convener/hide_cohort_hint"),
                PRIORITY_CONVENER,
                (viewer, target, vanillaValue) -> {
                    /*
                     * 用户确认：“杀手同伙”提示只在召集者召集后的限时变形期间隐藏。
                     * 召集者自己的永久伪装 morphTicks 为 -1，不会命中这个 helper。
                     */
                    return ConvenerCommunicationHelper.isTemporarilySummonedLivingPlayer(target)
                            ? RoleNameHudApi.VisibilityResult.HIDE
                            : RoleNameHudApi.VisibilityResult.PASS;
                }
        );
    }

    private static void registerDualPersonalityAppearance() {
        PlayerAppearanceApi.registerPlayerSkin(
                StupidExpress.id("appearance/player/dual_personality"),
                PRIORITY_DUAL_PERSONALITY,
                player -> {
                    UUID appearanceSource = DualPersonalityClientState.resolveSubAppearanceSource(player);
                    return appearanceSource == null ? null : ConvenerDisguiseResolver.resolveSkinForUuid(player, appearanceSource);
                }
        );

        RoleNameHudApi.registerName(
                StupidExpress.id("role_name/dual_personality/name"),
                PRIORITY_DUAL_PERSONALITY,
                (viewer, target, originalName) -> {
                    UUID appearanceSource = DualPersonalityClientState.resolveSubAppearanceSource(target);
                    if (appearanceSource == null) {
                        return null;
                    }
                    Component name = ConvenerDisguiseResolver.resolveDisguiseName(target, appearanceSource);
                    return name != null ? name : originalName;
                }
        );

        RoleNameHudApi.registerHudVisibility(
                StupidExpress.id("role_name/dual_personality/dormant_visibility"),
                PRIORITY_CONVENER,
                player -> {
                    /*
                     * 休眠人格的相机被挂到活跃人格身上时，不应该通过准心 HUD 读到额外身份信息。
                     * 这里交给 Wathe 统一清 nametag/note 淡出状态，替代旧 RoleNameRenderer mixin。
                     */
                    return DualPersonalityClientState.isDormant(player)
                            ? RoleNameHudApi.VisibilityResult.HIDE
                            : RoleNameHudApi.VisibilityResult.PASS;
                }
        );
    }

    private static @Nullable PlayerSkin resolveConvenerSkin(AbstractClientPlayer player) {
        ConvenerDisguiseComponent disguiseComponent = ConvenerDisguiseComponent.KEY.get(player);
        if (!disguiseComponent.isDisguised()) {
            return null;
        }

        UUID disguiseUuid = disguiseComponent.getDisguiseUuid();
        if (disguiseUuid == null) {
            return null;
        }

        /*
         * 即使伪装目标就是自己，也要返回“自己的原始皮肤”来截断低优先级规则。
         * 否则副人格处于召集者主动伪装状态时，下面的双重人格 handler 会继续把它改成主人格。
         */
        return ConvenerDisguiseResolver.resolveSkinForUuid(player, disguiseUuid);
    }

    private static @Nullable Component resolveConvenerName(Player target, Component originalName) {
        ConvenerDisguiseComponent disguiseComponent = ConvenerDisguiseComponent.KEY.get(target);
        if (!disguiseComponent.isDisguised()) {
            return null;
        }

        UUID disguiseUuid = disguiseComponent.getDisguiseUuid();
        if (disguiseUuid == null) {
            return null;
        }
        if (disguiseUuid.equals(target.getUUID())) {
            return originalName;
        }

        /*
         * 准星名字必须和最终显示皮肤一致。
         * 玩家看到“脸是尸体原主，名字却是真人”的瞬间会直接暴露召集者机制。
         */
        Component disguiseName = ConvenerDisguiseResolver.resolveDisguiseName(target, disguiseUuid);
        return disguiseName != null ? disguiseName : originalName;
    }
}
