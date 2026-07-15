package pro.fazeclan.river.stupid_express.shop;

import dev.doctor4t.wathe.api.shop.ShopApi;
import dev.doctor4t.wathe.api.shop.ShopPurchaseContext;
import dev.doctor4t.wathe.api.shop.ShopPurchaseResult;
import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.record.ShopPurchaseTracker;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * StupidExpress 通用商店工具类。
 *
 * <p>核心职责：</p>
 * <p>1. 统一处理购买成功/失败逻辑与音效；</p>
 * <p>2. 抽取 Wathe 原版商店中的基础价格，方便扩展职业按原价或偏移价复用；</p>
 * <p>3. 保留 Wathe 原版对 BLACKOUT / PSYCHO_MODE 的“购买即触发事件”行为，
 * 而不是把道具塞进玩家背包。</p>
 */
public final class SEShops {

    private static final Map<Item, Integer> ITEM_PRICES = new HashMap<>();

    static {
        for (ShopEntry entry : GameConstants.SHOP_ENTRIES) {
            ITEM_PRICES.put(entry.stack().getItem(), entry.price());
        }
    }

    private SEShops() {
    }

    /**
     * 读取 Wathe 原版商店里的基础价格。
     *
     * <p>这样后续别的职业要“沿用原价”“在原价基础上加减”时，
     * 就不用把价格写死两份，Wathe 改价后这里也会自动跟着变。</p>
     */
    public static int getBaseItemPrice(@NotNull Item item, int defaultValue) {
        return ShopApi.getDefaultPrice(item, ITEM_PRICES.getOrDefault(item, defaultValue));
    }

    /**
     * 把 StupidExpress 原有的职业商品列表适配成 Wathe ShopApi provider。
     *
     * <p>entriesProvider 仍然只负责“这个职业卖什么”；购买时会回到 {@link #purchase(ShopPurchaseContext)}，
     * 由 Wathe 统一扣钱、同步、播放音效和记录回放。</p>
     */
    public static dev.doctor4t.wathe.api.shop.RoleShopProvider provider(@NotNull RoleShopProvider entriesProvider) {
        return new dev.doctor4t.wathe.api.shop.RoleShopProvider() {
            @Override
            public @NotNull java.util.List<ShopEntry> getShopEntries(@NotNull Player player) {
                return entriesProvider.getShopEntries(player);
            }

            @Override
            public @NotNull ShopPurchaseResult purchase(@NotNull ShopPurchaseContext context) {
                return SEShops.purchase(context);
            }
        };
    }

    /**
     * 提供给 Wathe ShopApi 的购买交付逻辑。
     *
     * <p>这里不再扣钱、不再播放音效、不再写回放记录；这些公共动作已经集中到 Wathe。
     * 本方法只回答一个问题：当前商品有没有真的交付成功。</p>
     */
    public static @NotNull ShopPurchaseResult purchase(@NotNull ShopPurchaseContext context) {
        Player player = context.player();
        ShopEntry entry = context.entry();
        Item item = entry.stack().getItem();
        if (context.balance() < entry.price() || player.getCooldowns().isOnCooldown(item)) {
            return ShopPurchaseResult.FAIL_SHOW_MESSAGE;
        }

        return tryDeliverPurchasedEntry(player, entry)
                ? ShopPurchaseResult.SUCCESS
                : ShopPurchaseResult.FAIL_SHOW_MESSAGE;
    }

    /**
     * 统一处理 StupidExpress 自定义商店购买。
     *
     * <p>这里与 Wathe 原版不同的关键点是：</p>
     * <p>1. 购买失败提示改成可本地化文本；</p>
     * <p>2. BLACKOUT / PSYCHO_MODE 继续保留“立即触发能力”的特殊逻辑；</p>
     * <p>3. 只有真正购买成功时，外层才应该扣钱。</p>
     */
    public static boolean handlePurchase(@NotNull Player player, int balance, @NotNull ShopEntry entry) {
        Item item = entry.stack().getItem();
        if (balance < entry.price() || player.getCooldowns().isOnCooldown(item)) {
            notifyPurchaseFailed(player);
            return false;
        }

        boolean success = tryDeliverPurchasedEntry(player, entry);
        if (!success) {
            notifyPurchaseFailed(player);
            return false;
        }

        /*
         * 自定义商店里同一个格子可能已经被换成了完全不同的道具，
         * 因此购买成功后要主动把真实的 ShopEntry 回填给 Wathe 回放系统，
         * 避免回放继续按原版固定商店索引去误报商品。
            */
        ShopPurchaseTracker.captureSuccessfulPurchase(player, entry, -1, entry.price());
        ShopApi.playBuySound(player);
        return true;
    }

    private static boolean tryDeliverPurchasedEntry(@NotNull Player player, @NotNull ShopEntry entry) {
        Item item = entry.stack().getItem();

        // 这两个道具在 Wathe 原版里就是“购买即生效”的特殊商店能力。
        // 这里保留这层分支，方便以后 StupidExpress 其他职业也能直接复用。
        if (item == WatheItems.BLACKOUT) {
            return PlayerShopComponent.useBlackout(player);
        }
        if (item == WatheItems.PSYCHO_MODE) {
            return PlayerShopComponent.usePsychoMode(player);
        }

        return entry.onBuy(player);
    }

    /**
     * 统一扣钱并同步。
     *
     * <p>单独拆出来是为了让 mixin 更易读：先 handlePurchase，再在成功后调用这里。</p>
     */
    public static void completePurchase(@NotNull PlayerShopComponentAccessor shop, int price) {
        shop.stupid_express$setBalance(shop.stupid_express$getBalance() - price);
        shop.stupid_express$sync();
    }

    public static void notifyPurchaseFailed(@NotNull Player player) {
        ShopApi.sendPurchaseFailedMessage(player);
        ShopApi.playFailSound(player);
    }
}
