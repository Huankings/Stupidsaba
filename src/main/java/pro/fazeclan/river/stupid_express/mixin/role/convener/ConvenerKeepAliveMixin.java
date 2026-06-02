package pro.fazeclan.river.stupid_express.mixin.role.convener;

import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.game.gamemode.MurderGameMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.role.convener.ConvenerWinHelper;

@Mixin(MurderGameMode.class)
public class ConvenerKeepAliveMixin {

    @Inject(
            method = "tickServerGameLoop",
            at = @At(
                    value = "FIELD",
                    target = "Ldev/doctor4t/wathe/game/GameFunctions$WinStatus;NONE:Ldev/doctor4t/wathe/game/GameFunctions$WinStatus;",
                    ordinal = 3,
                    opcode = Opcodes.GETSTATIC
            ),
            cancellable = true
    )
    private void stupidexpress$keepConvenerRoundAlive(
            ServerLevel serverLevel,
            GameWorldComponent gameWorldComponent,
            CallbackInfo ci,
            @Local(name = "winStatus") GameFunctions.WinStatus winStatus
    ) {
        ServerPlayer livingConvener = ConvenerWinHelper.getLivingConvener(serverLevel, gameWorldComponent);
        if (livingConvener == null) {
            return;
        }

        // 召集者成为唯一幸存者时，直接按单独阵营写入结算并终止本 tick 的原版结算。
        if (ConvenerWinHelper.trySingleSurvivorWin(serverLevel, gameWorldComponent)) {
            ci.cancel();
            return;
        }

        // 只要召集者还活着，就继续拦住“普通阵营胜负”的原版结算，
        // 直到召集者自己达成条件或死亡。
        //
        // 但 TIME 必须放行：
        // 当游戏时间已经归零时，原版的超时结算优先级应高于召集者的拖局逻辑，
        // 否则就会出现“时间到了却永远不结算”的极端情况。
        if (winStatus == GameFunctions.WinStatus.KILLERS || winStatus == GameFunctions.WinStatus.PASSENGERS) {
            ci.cancel();
        }
    }
}
