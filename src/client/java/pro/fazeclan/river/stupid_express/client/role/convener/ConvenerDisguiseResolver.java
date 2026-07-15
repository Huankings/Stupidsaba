package pro.fazeclan.river.stupid_express.client.role.convener;

import dev.doctor4t.wathe.api.client.appearance.PlayerAppearanceApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import pro.fazeclan.river.stupid_express.client.modifier.dual_personality.DualPersonalityClientState;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerDisguiseComponent;

import java.util.UUID;

/**
 * 召集者客户端伪装解析工具。
 *
 * <p>这里专门承接“根据伪装 UUID 解析皮肤、名字、披风来源”的公共逻辑，
 * 避免其它普通代码去直接引用 mixin 类，导致类加载阶段触发 IllegalClassLoadError。</p>
 */
public final class ConvenerDisguiseResolver {

    private ConvenerDisguiseResolver() {}

    /**
     * 解析当前玩家应当显示成哪张皮肤。
     *
     * <p>返回 {@code null} 表示当前不需要替换皮肤，调用方应继续使用原始皮肤。</p>
     */
    public static @Nullable PlayerSkin resolveDisguiseSkin(AbstractClientPlayer player) {
        ConvenerDisguiseComponent disguiseComponent = ConvenerDisguiseComponent.KEY.get(player);
        if (disguiseComponent.isDisguised()) {
            /*
             * 用户确认的优先级是“已有变形优先于双重人格副人格外观”。
             * 所以召集者等主动伪装存在时，先返回伪装目标皮肤，不再继续查双重人格。
             */
            UUID disguiseUuid = disguiseComponent.getDisguiseUuid();
            if (disguiseUuid == null) {
                return null;
            }
            return resolveSkinForUuid(player, disguiseUuid);
        }

        UUID dualPersonalitySource = DualPersonalityClientState.resolveSubAppearanceSource(player);
        if (dualPersonalitySource != null) {
            return resolveSkinForUuid(player, dualPersonalitySource);
        }

        return null;
    }

    public static @Nullable PlayerSkin resolveSkinForUuid(AbstractClientPlayer player, UUID disguiseUuid) {
        if (disguiseUuid == null) {
            return null;
        }

        /*
         * 这里必须读取“UUID 的原始皮肤”，不能读取目标玩家实体当前 getSkin()。
         * 现在 getSkin() 已经由 Wathe 的外观 API 接管，如果 A 伪装成 B、B 又被其它规则覆盖，
         * 直接读实体会把 B 的当前伪装再次套进 A，造成递归或优先级串线。
         */
        return PlayerAppearanceApi.resolveOriginalSkinTextures(disguiseUuid, true);
    }

    public static @Nullable Component resolveDualPersonalityName(Player viewer) {
        /*
         * 准星中央名牌和皮肤替换必须读同一份外观来源。
         * 副人格哪怕轮换成活跃人格，身份上仍是副人格；
         * 所以只要本局仍处于 ACTIVE 且没有其它变形优先级，就显示主人格名字。
         */
        UUID dualPersonalitySource = DualPersonalityClientState.resolveSubAppearanceSource(viewer);
        return dualPersonalitySource == null ? null : resolveDisguiseName(viewer, dualPersonalitySource);
    }

    /**
     * 解析伪装目标应显示的名字。
     *
     * <p>返回 {@code null} 表示当前没有可用名字，调用方再自行决定显示“等待变形”等占位文本。</p>
     */
    public static @Nullable Component resolveDisguiseName(Player viewer, @Nullable UUID disguiseUuid) {
        if (disguiseUuid == null) {
            return null;
        }

        // 先处理“伪装成自己”的解除状态。
        if (disguiseUuid.equals(viewer.getUUID())) {
            return viewer.getDisplayName();
        }

        // 优先从当前客户端世界里找实体名字，这样旁观、活人、刚被传送来的玩家都能立即显示。
        Player levelPlayer = viewer.level().getPlayerByUUID(disguiseUuid);
        if (levelPlayer != null) {
            return levelPlayer.getDisplayName();
        }

        // 如果目标实体当前不在本地世界里，再从玩家列表缓存里取名字。
        PlayerInfo info = resolvePlayerInfo(disguiseUuid);
        if (info != null && info.getProfile() != null) {
            return Component.literal(info.getProfile().getName());
        }

        return null;
    }

    /**
     * 解析指定 UUID 对应的玩家列表缓存。
     */
    public static @Nullable PlayerInfo resolvePlayerInfo(UUID uuid) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.connection != null) {
            return client.player.connection.getPlayerInfo(uuid);
        }
        return null;
    }

    /**
     * 尝试在当前客户端世界里找到指定 UUID 的实时玩家实体。
     */
    public static @Nullable AbstractClientPlayer resolveLivePlayer(UUID uuid) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return null;
        }

        Player levelPlayer = client.level.getPlayerByUUID(uuid);
        if (levelPlayer instanceof AbstractClientPlayer abstractClientPlayer) {
            return abstractClientPlayer;
        }
        return null;
    }
}
