package pro.fazeclan.river.stupid_express.client.role.convener;

import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
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
        if (!disguiseComponent.isDisguised()) {
            return null;
        }

        UUID disguiseUuid = disguiseComponent.getDisguiseUuid();
        if (disguiseUuid == null) {
            return null;
        }

        Minecraft client = Minecraft.getInstance();

        // 优先处理“伪装成本地玩家自己”的情况，避免 UUID 命中后又继续往外找导致错皮。
        if (client.player != null && disguiseUuid.equals(client.player.getUUID())) {
            return client.player.getSkin();
        }

        // 目标玩家如果当前就在客户端世界里，直接取实时实体皮肤最准确。
        Player levelPlayer = player.level().getPlayerByUUID(disguiseUuid);
        if (levelPlayer instanceof AbstractClientPlayer abstractClientPlayer) {
            return abstractClientPlayer.getSkin();
        }

        // 否则退回玩家列表缓存，这样死亡、掉线或暂未加载进本地世界时也能继续显示。
        PlayerInfo info = resolvePlayerInfo(disguiseUuid);
        if (info != null && info.getSkin() != null) {
            return info.getSkin();
        }

        if (WatheClient.PLAYER_ENTRIES_CACHE != null && WatheClient.PLAYER_ENTRIES_CACHE.containsKey(disguiseUuid)) {
            return WatheClient.PLAYER_ENTRIES_CACHE.get(disguiseUuid).getSkin();
        }

        return null;
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
