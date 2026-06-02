package pro.fazeclan.river.stupid_express.shop;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 角色商店提供器。
 *
 * <p>之所以保留 {@link Player} 参数，而不是简单返回静态常量列表，
 * 是为了给后续扩展留口子：某些职业未来可能需要根据玩家状态、回合进度、
 * 组件数据等动态生成商店内容。</p>
 */
@FunctionalInterface
public interface RoleShopProvider {

    @NotNull List<ShopEntry> getShopEntries(@NotNull Player player);
}
