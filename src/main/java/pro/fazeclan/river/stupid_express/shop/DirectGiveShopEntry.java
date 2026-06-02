package pro.fazeclan.river.stupid_express.shop;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 直接发放到背包的商店条目。
 *
 * <p>Wathe 原版的 {@link ShopEntry} 默认会检查玩家是否“可使用杀手功能”，
 * 因此像初学者这种非杀手职业如果直接复用原版 {@code ShopEntry}，
 * 即便商店界面和扣钱逻辑都写好了，也会在真正发物品时失败。</p>
 *
 * <p>这个条目专门绕开那层限制，只做“找空栏并塞入物品”这件事。
 * 后续如果 StupidExpress 还有其他非杀手职业想拥有自定义商店，
 * 可以优先复用这个类。</p>
 */
public class DirectGiveShopEntry extends ShopEntry {

    public DirectGiveShopEntry(@NotNull ItemStack stack, int price, @NotNull Type type) {
        super(stack, price, type);
    }

    @Override
    public boolean onBuy(@NotNull Player player) {
        return insertStackInFreeSlot(player, stack().copy());
    }
}
