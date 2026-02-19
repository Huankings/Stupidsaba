package pro.fazeclan.river.stupid_express.mixin.role.thief;

import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pro.fazeclan.river.stupid_express.role.thief.ThiefItemTracker;

@Mixin(ShopEntry.class)
public class ThiefTrackShopMixin {

    @Inject(
        method = "insertStackInFreeSlot(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Z",
        at = @At("RETURN")
    )
    private static void onItemInserted(Player player, ItemStack stackToInsert, 
                                        CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayer serverPlayer && cir.getReturnValue()) {
            ThiefItemTracker.onBuyItem(serverPlayer);
        }
    }
}