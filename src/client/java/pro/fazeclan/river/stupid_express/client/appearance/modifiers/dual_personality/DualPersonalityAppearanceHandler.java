package pro.fazeclan.river.stupid_express.client.appearance.modifiers.dual_personality;

import dev.doctor4t.wathe.api.client.appearance.PlayerAppearanceApi;
import dev.doctor4t.wathe.api.client.gui.RoleNameHudApi;
import net.minecraft.network.chat.Component;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.client.appearance.StupidExpressAppearancePriorities;
import pro.fazeclan.river.stupid_express.client.modifier.dual_personality.DualPersonalityClientState;
import pro.fazeclan.river.stupid_express.client.role.convener.ConvenerDisguiseResolver;

import java.util.UUID;

/**
 * 双重人格词条接入 Wathe 外观与准心名字 API 的规则。
 *
 * <p>副人格在没有更高优先级伪装时显示为主人格；休眠人格视角不显示 Wathe 准心 HUD，
 * 防止通过挂载相机读取到当前不该获取的身份信息。</p>
 */
public final class DualPersonalityAppearanceHandler {
    private DualPersonalityAppearanceHandler() {
    }

    public static void register() {
        PlayerAppearanceApi.registerPlayerSkin(
                StupidExpress.id("appearance/player/dual_personality"),
                StupidExpressAppearancePriorities.DUAL_PERSONALITY,
                player -> {
                    UUID appearanceSource = DualPersonalityClientState.resolveSubAppearanceSource(player);
                    /*
                     * appearanceSource 为空代表该玩家不是“需要显示成主人格”的副人格；
                     * 非空时按主人格 UUID 解析原皮，作为低优先级外观兜底。
                     */
                    return appearanceSource == null
                            ? null
                            : ConvenerDisguiseResolver.resolveSkinForUuid(player, appearanceSource);
                }
        );

        RoleNameHudApi.registerName(
                StupidExpress.id("role_name/dual_personality/name"),
                StupidExpressAppearancePriorities.DUAL_PERSONALITY,
                (viewer, target, originalName) -> {
                    UUID appearanceSource = DualPersonalityClientState.resolveSubAppearanceSource(target);
                    if (appearanceSource == null) {
                        return null;
                    }

                    /*
                     * 副人格显示为主人格时，准心名字也要解析到主人格；
                     * 缓存暂时拿不到名字时保留原名，避免 HUD 空白或崩溃。
                     */
                    Component name = ConvenerDisguiseResolver.resolveDisguiseName(target, appearanceSource);
                    return name != null ? name : originalName;
                }
        );

        RoleNameHudApi.registerHudVisibility(
                StupidExpress.id("role_name/dual_personality/dormant_visibility"),
                StupidExpressAppearancePriorities.CONVENER,
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
}
