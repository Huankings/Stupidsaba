package pro.fazeclan.river.stupid_express.client.modifier.dual_personality;

import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.model.WatheModelLayers;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 双重人格尸体外观解析。
 *
 * <p>这里选择 NoellesRoles 的 PlayerBodySkinRenderer 思路：只在客户端渲染层替换皮肤/模型，
 * 不改服务端 PlayerBodyEntity 里保存的尸体 owner UUID。这样尸体在验尸、回放、尸袋、
 * 其它职业读取 owner 时仍然属于真正死亡的副人格，只是玩家肉眼看到的是主人格外形。</p>
 */
public final class DualPersonalityBodyRenderHelper {

    private static PlayerModel<PlayerBodyEntity> classicBodyModel;
    private static PlayerModel<PlayerBodyEntity> slimBodyModel;

    private DualPersonalityBodyRenderHelper() {
    }

    public static void initializeBodyModels(EntityRendererProvider.Context context) {
        if (classicBodyModel == null) {
            classicBodyModel = new PlayerModel<>(context.bakeLayer(WatheModelLayers.PLAYER_BODY), false);
        }
        if (slimBodyModel == null) {
            slimBodyModel = new PlayerModel<>(context.bakeLayer(WatheModelLayers.PLAYER_BODY_SLIM), true);
        }
    }

    public static @Nullable PlayerSkin resolveDualPersonalityBodySkin(PlayerBodyEntity body) {
        if (body == null) {
            return null;
        }

        UUID appearanceSource = DualPersonalityClientState.resolveSubAppearanceSource(
                body.level(),
                body.getPlayerUuid()
        );
        if (appearanceSource == null) {
            return null;
        }

        return resolveSkinForUuid(appearanceSource);
    }

    public static @Nullable ResourceLocation resolveDualPersonalityBodyTexture(PlayerBodyEntity body) {
        PlayerSkin skin = resolveDualPersonalityBodySkin(body);
        return skin == null ? null : skin.texture();
    }

    public static @Nullable PlayerModel<PlayerBodyEntity> resolveBodyModel(@Nullable PlayerSkin skin) {
        if (skin == null) {
            return null;
        }

        return skin.model() == PlayerSkin.Model.SLIM ? slimBodyModel : classicBodyModel;
    }

    private static @Nullable PlayerSkin resolveSkinForUuid(UUID uuid) {
        Minecraft client = Minecraft.getInstance();

        /*
         * 优先从本地实体和当前世界实体取皮肤，能覆盖玩家刚死亡但仍在客户端世界里的瞬间。
         * 找不到实体时再退回玩家列表和 Wathe 的跨回合缓存。
         */
        if (client.player != null && uuid.equals(client.player.getUUID())) {
            return client.player.getSkin();
        }

        if (client.level != null) {
            Player levelPlayer = client.level.getPlayerByUUID(uuid);
            if (levelPlayer instanceof AbstractClientPlayer abstractClientPlayer) {
                return abstractClientPlayer.getSkin();
            }
        }

        if (client.player != null && client.player.connection != null) {
            PlayerInfo info = client.player.connection.getPlayerInfo(uuid);
            if (info != null && info.getSkin() != null) {
                return info.getSkin();
            }
        }

        if (WatheClient.PLAYER_ENTRIES_CACHE != null && WatheClient.PLAYER_ENTRIES_CACHE.containsKey(uuid)) {
            return WatheClient.PLAYER_ENTRIES_CACHE.get(uuid).getSkin();
        }

        return null;
    }
}
