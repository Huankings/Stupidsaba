package pro.fazeclan.river.stupid_express.mixin.role;

import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pro.fazeclan.river.stupid_express.cca.CustomWinnerComponent;

import java.util.UUID;

@Mixin(GameRoundEndComponent.class)
public class CustomWinnerCheckMixin {

    @Shadow
    @Final
    private Level world;

    @Inject(
            method = "didWin",
            at = @At(value = "RETURN", ordinal = 1),
            cancellable = true
    )
    private void didWin(UUID uuid, CallbackInfoReturnable<Boolean> cir, @Local(name = "detail") GameRoundEndComponent.RoundEndData detail) {
        var component = CustomWinnerComponent.KEY.get(world);
        if (!component.hasCustomWinner()) {
            return;
        }

        /*
         * 这里必须用 UUID 判断，不再从 CustomWinnerComponent 里取 Player 实体。
         *
         * 结算音效、回放胜负标记、TAB 结算页都可能在胜利者已经退出游戏后继续读取 didWin。
         * 如果这里依赖 getWinners() 返回的在线实体列表，离线胜利者会被过滤掉，
         * 最终被当成失败者或“其他阵营”处理。
         */
        boolean isWinner = component.isWinner(uuid);
        cir.setReturnValue(isWinner);
    }

}
