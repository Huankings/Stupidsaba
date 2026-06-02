package pro.fazeclan.river.stupid_express.mixin.role.convener;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerDisguiseComponent;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerMomentumComponent;
import pro.fazeclan.river.stupid_express.role.convener.cca.ConvenerPlayerComponent;

@Mixin(GameFunctions.class)
public abstract class ConvenerResetMixin {

    @Inject(method = "resetPlayer", at = @At("TAIL"))
    private static void stupidexpress$resetConvenerComponents(ServerPlayer player, CallbackInfo ci) {
        // 召集者有多份回合态数据：解锁头像列表、召集次数、任务护盾进度、当前伪装状态。
        // 这里统一在每次 resetPlayer 时清空，确保下一把开局绝不会继承上一把的尸体面孔。
        ConvenerPlayerComponent.KEY.get(player).reset();
        ConvenerDisguiseComponent.KEY.get(player).clearDisguise();
        ConvenerMomentumComponent.KEY.get(player).reset();
    }
}
