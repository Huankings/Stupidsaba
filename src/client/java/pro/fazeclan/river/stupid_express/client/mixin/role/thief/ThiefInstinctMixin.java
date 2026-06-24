package pro.fazeclan.river.stupid_express.client.mixin.role.thief;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.client.WatheClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pro.fazeclan.river.stupid_express.constants.SERoles;

import java.awt.*;

@Mixin(WatheClient.class)
public class ThiefInstinctMixin {
    
    @Shadow
    public static KeyMapping instinctKeybind;

    @Inject(method = "isInstinctEnabled", at = @At("HEAD"), cancellable = true)
    private static void enableArsonistInstinct(CallbackInfoReturnable<Boolean> cir) {
        Player player = Minecraft.getInstance().player;
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.level());
        if (gameWorldComponent.isRole(player, SERoles.THIEF)) {
            /*
             * 盗贼是“按本能键才获得透视资格”的职业。
             * 这里必须走 WatheClient 的统一输入入口，才能同时支持玩家个人的
             * 开关模式和长按模式，而不是硬编码成 isDown() 长按。
             */
            if (WatheClient.isInstinctInputActive()) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }

    @Inject(
        method = "getInstinctHighlight",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void highlightPlayers(Entity target, CallbackInfoReturnable<Integer> cir) {

        Player player = Minecraft.getInstance().player;
        GameWorldComponent gameWorldComponent = GameWorldComponent.KEY.get(player.level());

        if (!gameWorldComponent.isRole(player, SERoles.THIEF)) return;
        
        if (WatheClient.isPlayerSpectatingOrCreative()) return;
        
        if (!WatheClient.isInstinctEnabled()) return;

        if (!(target instanceof Player) && !(target instanceof ItemEntity)) {
            return;
        }
    
        if (target == player) {
            cir.setReturnValue(SERoles.THIEF.color());
            return;
        }

        cir.setReturnValue(Color.GRAY.getRGB());
    }
}
