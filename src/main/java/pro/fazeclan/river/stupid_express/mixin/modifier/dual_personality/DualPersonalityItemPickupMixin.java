package pro.fazeclan.river.stupid_express.mixin.modifier.dual_personality;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityManager;

@Mixin(ItemEntity.class)
public class DualPersonalityItemPickupMixin {

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void stupidexpress$blockInnocentDoubleActiveRevolverPickup(Player player, CallbackInfo ci) {
        /*
         * 左轮是实体拾取触发，不一定经过“使用物品”类回调。
         * 直接拦 ItemEntity#playerTouch 可以覆盖地上捡枪、死亡掉落再捡回等情况。
         */
        if (player instanceof ServerPlayer serverPlayer
                && DualPersonalityManager.shouldBlockRevolverPickup(serverPlayer, (ItemEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
