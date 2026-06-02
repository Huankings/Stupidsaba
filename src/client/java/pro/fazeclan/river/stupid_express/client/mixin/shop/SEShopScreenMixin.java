package pro.fazeclan.river.stupid_express.client.mixin.shop;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedHandledScreen;
import dev.doctor4t.wathe.client.gui.screen.ingame.LimitedInventoryScreen;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.shop.SEShopRegistry;

import java.util.List;

/**
 * 客户端统一替换商店显示列表。
 *
 * <p>服务端购买和客户端展示必须使用同一套条目来源，
 * 否则会出现“界面能看到 A，但实际买到的是 B”的错位问题。</p>
 */
@Mixin(LimitedInventoryScreen.class)
public abstract class SEShopScreenMixin extends LimitedHandledScreen<InventoryMenu> {

    @Shadow @Final public LocalPlayer player;

    protected SEShopScreenMixin(InventoryMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @ModifyVariable(method = "init", at = @At(value = "STORE"), name = "entries")
    private List<ShopEntry> stupid_express$replaceShopEntries(List<ShopEntry> originalEntries) {
        if (SEShopRegistry.hasCustomShop(this.player)) {
            return SEShopRegistry.getEntriesForPlayer(this.player);
        }
        return originalEntries;
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void stupid_express$appendNonKillerCustomShop(CallbackInfo ci) {
        if (!SEShopRegistry.hasCustomShop(this.player)) {
            return;
        }

        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(this.player.level());
        if (gameWorldComponent.canUseKillerFeatures(this.player)) {
            // 杀手职业的自定义商店已经在上面的 ModifyVariable 中替换过原版 entries，
            // 这里不再重复添加按钮。
            return;
        }

        List<ShopEntry> entries = SEShopRegistry.getEntriesForPlayer(this.player);
        int apart = 36;
        int x = this.width / 2 - entries.size() * apart / 2 + 9;
        int shouldBeY = (this.height - 32) / 2;
        int y = shouldBeY - 46;

        for (int i = 0; i < entries.size(); i++) {
            this.addRenderableWidget(new LimitedInventoryScreen.StoreItemWidget(
                    (LimitedInventoryScreen) (Object) this,
                    x + apart * i,
                    y,
                    entries.get(i),
                    i
            ));
        }
    }
}
