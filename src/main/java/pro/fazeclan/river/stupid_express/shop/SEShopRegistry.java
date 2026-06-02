package pro.fazeclan.river.stupid_express.shop;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * StupidExpress 的商店注册中心。
 *
 * <p>这里专门负责“某个职业应该显示/购买哪一套商店条目”。这样做有两个好处：</p>
 * <p>1. 不再需要把职业商店硬塞进 {@code SERoles} 这种角色常量类里；</p>
 * <p>2. 后续新增职业商店时，只需要注册“职业 -> 商店提供器”的映射即可，
 * 不必再复制一套新的客户端/服务端购买逻辑。</p>
 */
public final class SEShopRegistry {

    private static final Map<Role, RoleShopProvider> ROLE_SHOPS = new HashMap<>();

    private SEShopRegistry() {
    }

    /**
     * 注册某个职业的专属商店。
     */
    public static void registerRoleShop(@NotNull Role role, @NotNull RoleShopProvider provider) {
        ROLE_SHOPS.put(role, provider);
    }

    /**
     * 读取玩家当前应当使用的商店条目。
     *
     * <p>如果玩家拥有 StupidExpress 注册的专属商店，则返回对应列表；
     * 否则回退到 Wathe 原版商店，保证不影响其他正常职业。</p>
     */
    public static @NotNull List<ShopEntry> getEntriesForPlayer(@NotNull Player player) {
        RoleShopProvider provider = getProviderForPlayer(player);
        if (provider != null) {
            return provider.getShopEntries(player);
        }
        return GameConstants.SHOP_ENTRIES;
    }

    /**
     * 判断玩家是否命中了 StupidExpress 自定义商店。
     *
     * <p>服务端购买与客户端显示都需要用这个判断，避免只有一边换了列表。</p>
     */
    public static boolean hasCustomShop(@NotNull Player player) {
        return getProviderForPlayer(player) != null;
    }

    private static @Nullable RoleShopProvider getProviderForPlayer(@NotNull Player player) {
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.level());
        Role role = gameWorldComponent.getRole(player);
        if (role == null) {
            return null;
        }
        return ROLE_SHOPS.get(role);
    }
}
