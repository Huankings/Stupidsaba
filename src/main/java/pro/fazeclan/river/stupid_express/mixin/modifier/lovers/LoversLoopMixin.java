package pro.fazeclan.river.stupid_express.mixin.modifier.lovers;

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

@Mixin(value = MurderGameMode.class, priority = 900)
public class LoversLoopMixin {

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
    private void loversWinCheck(
            ServerLevel serverWorld,
            GameWorldComponent gameWorldComponent,
            CallbackInfo ci,
            @Local(name = "winStatus") GameFunctions.WinStatus winStatus
    ) {

        var config = StupidExpress.CONFIG;
        var loversAlive = false;

        var modifierComponent = WorldModifierComponent.KEY.get(serverWorld);

        var remainingPlayers = serverWorld.getPlayers(GameFunctions::isPlayerAliveAndSurvival);
        var remainingLovers = remainingPlayers.stream().filter(p -> modifierComponent.isModifier(p, SEModifiers.LOVERS)).toList();

        if (remainingLovers.size() == 1) {
            // 如果你的恋人不在了...很抱歉
            GameFunctions.killPlayer(remainingLovers.getFirst(), true, null, StupidExpress.id("broken_heart"));
            return;
        }

        for (ServerPlayer player : remainingLovers) {
            loversAlive = true;

            // 检查是否满足“只有恋人获胜”的条件
            if (remainingPlayers.size() == remainingLovers.size()) {
                var ce = CustomWinnerComponent.KEY.get(serverWorld);
                ce.setWinningTextId(SEModifiers.LOVERS.identifier().getPath());
                ce.setWinners(remainingLovers.stream().map(sp -> serverWorld.getPlayerByUUID(sp.getUUID())).toList());
                ce.setColor(SEModifiers.LOVERS.color());
                ce.sync();

                GameRoundEndComponent.KEY.get(serverWorld)
                        .setRoundEndData(serverWorld.players(), GameFunctions.WinStatus.KILLERS);

                GameFunctions.stopGame(serverWorld);
                ci.cancel();
                return;
            }

            // 检查是否满足“与杀手恋人”获胜条件
            if (config.modifiersSection.loversSection.loversWinWithKillers) {
                var lover = remainingLovers.stream().filter(p -> !p.equals(player)).toList().getFirst();
                if (gameWorldComponent.isInnocent(player) && gameWorldComponent.isInnocent(lover)) {
                    continue;
                }
                var remainingNonInnocent = remainingPlayers.stream().filter(hostile -> !gameWorldComponent.isInnocent(hostile)).toList();
                if (remainingPlayers.size() - 1 != remainingNonInnocent.size()) {
                    continue;
                }
                GameRoundEndComponent.KEY.get(serverWorld)
                        .setRoundEndData(serverWorld.players(), GameFunctions.WinStatus.KILLERS);
                GameFunctions.stopGame(serverWorld);
                ci.cancel();
                return;
            }
        }

        // 检查如果恋人不能和乘客阵营一起获胜的时候，则让游戏继续进行下去
        if (loversAlive
                && !config.modifiersSection.loversSection.loversWinWithCivilians
                && (winStatus == GameFunctions.WinStatus.KILLERS || winStatus == GameFunctions.WinStatus.PASSENGERS)) {
            ci.cancel();
        }

    }

}
