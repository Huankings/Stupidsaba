package pro.fazeclan.river.stupid_express.client.appearance.roles.convener;

import dev.doctor4t.wathe.api.client.appearance.PlayerAppearanceApi;
import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.client.appearance.StupidExpressAppearancePriorities;
import pro.fazeclan.river.stupid_express.client.role.convener.ConvenerDisguiseResolver;
import pro.fazeclan.river.stupid_express.role.convener.ConvenerCommunicationHelper;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerDisguiseComponent;

import java.util.UUID;

/**
 * 召集者接入 Wathe 外观与准心名字 API 的规则。
 *
 * <p>召集者负责两种显示：召集尸体后的限时全员伪装，以及召集者自己的主动伪装。
 * 两者都写入 ConvenerDisguiseComponent，因此皮肤和准心名字统一从该组件解析。</p>
 */
public final class ConvenerAppearanceHandler {
    private ConvenerAppearanceHandler() {
    }

    public static void register() {
        PlayerAppearanceApi.registerPlayerSkin(
                StupidExpress.id("appearance/player/convener"),
                StupidExpressAppearancePriorities.CONVENER,
                ConvenerAppearanceHandler::resolveConvenerSkin
        );

        RoleNameHudApi.registerName(
                StupidExpress.id("role_name/convener/name"),
                StupidExpressAppearancePriorities.CONVENER,
                (viewer, target, originalName) -> resolveConvenerName(target, originalName)
        );

        RoleNameHudApi.registerCohortHint(
                StupidExpress.id("role_name/convener/hide_cohort_hint"),
                StupidExpressAppearancePriorities.CONVENER,
                (viewer, target, vanillaValue) -> {
                    /*
                     * “杀手同伙”提示只在召集者召集尸体后的限时变形期间隐藏。
                     * 召集者自己的永久伪装 morphTicks 为 -1，不会命中这个 helper，
                     * 因此不会把正常同伙提示长期吞掉。
                     */
                    return ConvenerCommunicationHelper.isTemporarilySummonedLivingPlayer(target)
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
         * 准心名字必须和最终显示皮肤一致。
         * 玩家看到“脸是尸体原主，名字却是真人”的瞬间会直接暴露召集者机制。
         */
        Component disguiseName = ConvenerDisguiseResolver.resolveDisguiseName(target, disguiseUuid);
        return disguiseName != null ? disguiseName : originalName;
    }
}
