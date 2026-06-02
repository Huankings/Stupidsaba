package pro.fazeclan.river.stupid_express.mixin.shop;

import dev.doctor4t.wathe.cca.PlayerShopComponent;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.shop.PlayerShopComponentAccessor;
import pro.fazeclan.river.stupid_express.shop.SEShopRegistry;
import pro.fazeclan.river.stupid_express.shop.SEShops;

import java.util.List;

/**
 * 服务端统一接管 StupidExpress 自定义商店购买。
 *
 * <p>只要玩家命中了我们注册的专属商店，就不再走 Wathe 原版固定的 SHOP_ENTRIES。
 * 这样后续新增职业商店时，不需要再为每个职业单独写一个新的 tryBuy mixin。</p>
 */
@Mixin(PlayerShopComponent.class)
public abstract class SEPlayerShopMixin implements PlayerShopComponentAccessor {

    @Shadow @Final private Player player;
    @Shadow public int balance;
    @Shadow public abstract void sync();

    @Inject(method = "tryBuy", at = @At("HEAD"), cancellable = true)
    private void stupid_express$tryCustomShopBuy(int index, CallbackInfo ci) {
        if (!SEShopRegistry.hasCustomShop(this.player)) {
            return;
        }

        List<ShopEntry> entries = SEShopRegistry.getEntriesForPlayer(this.player);
        if (index < 0 || index >= entries.size()) {
            ci.cancel();
            return;
        }

        ShopEntry entry = entries.get(index);
        if (SEShops.handlePurchase(this.player, this.balance, entry)) {
            SEShops.completePurchase(this, entry.price());
        }

        ci.cancel();
    }

    @Override
    public int stupid_express$getBalance() {
        return this.balance;
    }

    @Override
    public void stupid_express$setBalance(int balance) {
        this.balance = balance;
    }

    @Override
    public void stupid_express$sync() {
        this.sync();
    }
}
