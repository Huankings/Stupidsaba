package pro.fazeclan.river.stupid_express.mixin.modifier.dual_personality;

import com.llamalad7.mixinextras.sugar.Local;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.game.gamemode.MurderGameMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.agmas.harpymodloader.component.WorldModifierComponent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.StupidExpress;
import pro.fazeclan.river.stupid_express.cca.CustomWinnerComponent;
import pro.fazeclan.river.stupid_express.constants.SEModifiers;
import pro.fazeclan.river.stupid_express.modifier.dual_personality.DualPersonalityComponent;

import java.util.List;

@Mixin(value = MurderGameMode.class, priority = 1100)
public class DualPersonalityLoopMixin {

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
    private void stupidexpress$dualPersonalityWinCheck(
            ServerLevel serverWorld,
            GameWorldComponent gameWorldComponent,
            CallbackInfo ci,
            @Local(name = "winStatus") GameFunctions.WinStatus winStatus
    ) {
        /*
         * 这里插在 Wathe 原版胜利状态已经算出来之后。
         * 好处是不用重写整套杀手/乘客胜利判断，只在“本局还有双重人格”时追加独立胜利和共胜拦截。
         */
        var config = StupidExpress.CONFIG.modifiersSection.dualPersonalitySection;
        WorldModifierComponent modifierComponent = WorldModifierComponent.KEY.get(serverWorld);
        DualPersonalityComponent dualPersonalityComponent = DualPersonalityComponent.KEY.get(serverWorld);

        List<ServerPlayer> remainingPlayers = serverWorld.getPlayers(GameFunctions::isPlayerAliveAndSurvival);
        List<ServerPlayer> remainingDualPersonalities = remainingPlayers.stream()
                .filter(player -> modifierComponent.isModifier(player, SEModifiers.DUAL_PERSONALITY))
                .toList();

        if (remainingDualPersonalities.isEmpty()) {
            return;
        }

        if (remainingPlayers.size() == remainingDualPersonalities.size()) {
            /*
             * 用户确认：只要剩余存活玩家全部带双重人格词条，就判定双重人格胜利。
             * 这天然支持多对双重人格，不要求只剩同一对。
             */
            CustomWinnerComponent customWinnerComponent = CustomWinnerComponent.KEY.get(serverWorld);
            customWinnerComponent.setWinningTextId(SEModifiers.DUAL_PERSONALITY.identifier().getPath());
            customWinnerComponent.setWinners(remainingDualPersonalities);
            customWinnerComponent.setColor(SEModifiers.DUAL_PERSONALITY.color());
            customWinnerComponent.sync();

            GameRoundEndComponent.KEY.get(serverWorld)
                    .setRoundEndData(serverWorld.players(), GameFunctions.WinStatus.KILLERS);
            GameFunctions.stopGame(serverWorld);
            ci.cancel();
            return;
        }

        boolean anyDoubleActiveDualPersonality = remainingDualPersonalities.stream()
                .anyMatch(player -> dualPersonalityComponent.isDoubleActive(player.getUUID()));
        if (anyDoubleActiveDualPersonality
                && (winStatus == GameFunctions.WinStatus.KILLERS || winStatus == GameFunctions.WinStatus.PASSENGERS)) {
            /*
             * 双活时刻是双重人格被迫解离后的最终独胜窗口。
             * 即使 config 允许和杀手/乘客共胜，也不能让普通阵营胜利把双活中的双重人格一起带走；
             * 只有上面的“剩余存活者全部都是双重人格”条件满足时，才会结算双重人格自己的独立胜利。
             */
            ci.cancel();
            return;
        }

        /*
         * 和恋人一样，双重人格默认不允许被普通乘客/杀手胜利一起带走。
         * 如果配置允许共胜，就放行 Wathe 原本的胜利流程；否则只要还有双重人格活着，
         * 默认胜利会被延后，直到满足双重人格自己的独立胜利或他们被淘汰。
         */
        boolean anyDualPersonalityKiller = remainingDualPersonalities.stream()
                .anyMatch(player -> !gameWorldComponent.isInnocent(player));
        if (winStatus == GameFunctions.WinStatus.KILLERS
                && (!config.dualPersonalityWinWithKillers || !anyDualPersonalityKiller)) {
            // 杀手胜利时，如果配置不允许共胜，或剩余双重人格都不是杀手阵营，就延后结束。
            ci.cancel();
            return;
        }

        if (winStatus == GameFunctions.WinStatus.PASSENGERS && !config.dualPersonalityWinWithCivilians) {
            // 乘客胜利同理：默认双重人格是独立胜利阵营，不会被普通乘客胜利顺带结算。
            ci.cancel();
        }
    }
}
