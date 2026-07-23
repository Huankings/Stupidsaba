package pro.fazeclan.river.stupid_express.client.modifier.dual_personality;

import dev.doctor4t.wathe.api.client.appearance.PlayerAppearanceApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 双重人格外观解析工具。
 *
 * <p>这里专门放“根据 UUID 取皮肤 / 取名字”的纯工具逻辑，
 * 不再依赖召集者的组件或字段，避免双活外观在类加载阶段间接碰到已删除的职业代码。</p>
 */
public final class DualPersonalityAppearanceResolver {

    private DualPersonalityAppearanceResolver() {
    }

    public static @Nullable PlayerSkin resolveSkinForUuid(AbstractClientPlayer player, UUID disguiseUuid) {
        if (disguiseUuid == null) {
            return null;
        }

        /*
         * 必须按 UUID 回查“原始皮肤”，不能直接读当前实体的 getSkin()。
         * 否则如果某个外观又被更高优先级规则覆盖，就会把当前伪装层再套一层。
         */
        return PlayerAppearanceApi.resolveOriginalSkinTextures(disguiseUuid, true);
    }

    public static @Nullable Component resolveDisguiseName(Player viewer, @Nullable UUID disguiseUuid) {
        if (disguiseUuid == null) {
            return null;
        }

        // 自己伪装成自己时，直接显示本名。
        if (disguiseUuid.equals(viewer.getUUID())) {
            return viewer.getDisplayName();
        }

        // 优先从当前世界找实体名字，避免刚进局时玩家列表还没更新。
        Player levelPlayer = viewer.level().getPlayerByUUID(disguiseUuid);
        if (levelPlayer != null) {
            return levelPlayer.getDisplayName();
        }

        // 再从玩家列表缓存里找名字。
        PlayerInfo info = resolvePlayerInfo(disguiseUuid);
        if (info != null && info.getProfile() != null) {
            return Component.literal(info.getProfile().getName());
        }

        return null;
    }

    public static @Nullable PlayerInfo resolvePlayerInfo(UUID uuid) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.player.connection != null) {
            return client.player.connection.getPlayerInfo(uuid);
        }
        return null;
    }

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
