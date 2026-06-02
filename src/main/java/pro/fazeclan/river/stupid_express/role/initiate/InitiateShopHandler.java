package pro.fazeclan.river.stupid_express.role.initiate;

import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import pro.fazeclan.river.stupid_express.shop.DirectGiveShopEntry;
import pro.fazeclan.river.stupid_express.shop.SEShops;

import java.util.ArrayList;
import java.util.List;

/**
 * 初学者专属商店。
 *
 * <p>目前只放初学者自己的商品；
 * 后续如果要给其他职业定制商店，也建议按这个文件的结构单独拆分类。</p>
 */
public final class InitiateShopHandler {

    /**
     * 这里先做成静态列表，是因为当前初学者商店不依赖玩家实时状态。
     * 以后如果要做动态商品，也可以把 {@link #getShopEntries(Player)} 改成现场生成。
     */
    private static final List<ShopEntry> SHOP_ENTRIES = Util.make(new ArrayList<>(), entries -> {
        // 初学者当前只有一把售价更高的匕首。
        // 价格跟着 Wathe 原价浮动，再额外 +100；
        // 同时这里必须使用“直接发放型条目”，否则非杀手职业会在 onBuy 时被 Wathe 原版拦下。
        entries.add(new DirectGiveShopEntry(
                WatheItems.KNIFE.getDefaultInstance(),
                SEShops.getBaseItemPrice(WatheItems.KNIFE, 100) + 100,
                ShopEntry.Type.WEAPON
        ));
    });

    private InitiateShopHandler() {
    }

    public static @NotNull List<ShopEntry> getShopEntries(@NotNull Player player) {
        return SHOP_ENTRIES;
    }
}
