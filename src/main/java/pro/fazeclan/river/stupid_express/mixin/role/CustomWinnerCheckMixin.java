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

        // 旧逻辑是拿“结算分类文本”去对比自定义获胜 id，
        // 对中立单独阵营来说并不可靠，因为服务端原始分类里他们通常还是被记成平民。
        // 这里改成直接以 CustomWinnerComponent 写入的获胜 UUID 列表为准。
        boolean isWinner = component.getWinners().stream().anyMatch(player -> player.getUUID().equals(uuid));
        cir.setReturnValue(isWinner);
    }

}
