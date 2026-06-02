package pro.fazeclan.river.stupid_express.client.role.convener;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.PlayerSkin;
import org.jetbrains.annotations.Nullable;

/**
 * 召集者伪装渲染辅助类。
 *
 * <p>当前 Convener 已经会替换 PlayerSkin 纹理，
 * 但如果目标是 Alex 纤细模型，而当前渲染器仍然拿着 Steve 经典模型，
 * 或者反过来，就会出现“皮肤对了、模型身形不对”的破绽。
 *
 * <p>这里单独缓存 slim/classic 两套玩家模型，供渲染 mixin 在每次真正渲染前
 * 按目标皮肤的 {@link PlayerSkin#model()} 动态切换。</p>
 */
public final class ConvenerDisguiseRenderHelper {

    private static PlayerModel<AbstractClientPlayer> classicModel;
    private static PlayerModel<AbstractClientPlayer> slimModel;

    private ConvenerDisguiseRenderHelper() {
    }

    /**
     * 缓存原版玩家渲染器使用的两套标准模型：
     * - ModelLayers.PLAYER：Steve 经典模型；
     * - ModelLayers.PLAYER_SLIM：Alex 纤细模型。
     */
    public static void initializePlayerModels(EntityRendererProvider.Context context) {
        if (classicModel == null) {
            classicModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        }
        if (slimModel == null) {
            slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        }
    }

    /**
     * 根据当前真正显示出来的皮肤，返回对应的玩家模型。
     *
     * <p>返回 null 时说明没有拿到皮肤数据，调用方保持原模型即可。</p>
     */
    public static @Nullable PlayerModel<AbstractClientPlayer> resolvePlayerModel(@Nullable PlayerSkin playerSkin) {
        if (playerSkin == null) {
            return null;
        }

        return playerSkin.model() == PlayerSkin.Model.SLIM ? slimModel : classicModel;
    }
}
