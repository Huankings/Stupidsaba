package pro.fazeclan.river.stupid_express.mixin.role.thief;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.game.gamemode.MurderGameMode;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;

import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.role.thief.ThiefItemRules;
import pro.fazeclan.river.stupid_express.cca.CustomWinnerComponent;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(MurderGameMode.class)
public class ThiefKeepAliveMixin {
    @Inject(
        method = "tickServerGameLoop",
        at = @At(
            value = "INVOKE",
            target = "Ldev/doctor4t/wathe/game/GameFunctions;stopGame(Lnet/minecraft/server/level/ServerLevel;)V",
            ordinal = 0
        ),
        cancellable = true
    )
    private void keepAlive(ServerLevel serverLevel, GameWorldComponent gameWorldComponent, CallbackInfo ci) {
        List<ServerPlayer> alivePlayers = serverLevel.players().stream()
            .filter(GameFunctions::isPlayerAliveAndSurvival)
            .collect(Collectors.toList());
        
        boolean thiefAlive = false;
        boolean hasKeepGameGoingItem = false;
        
        for (ServerPlayer player : alivePlayers) {
            if (gameWorldComponent.isRole(player, SERoles.THIEF)) {
                thiefAlive = true;
            }

            if (!hasKeepGameGoingItem) {
                hasKeepGameGoingItem = player.getInventory().items.stream()
                    .anyMatch(stack -> !stack.isEmpty() && 
                        ThiefItemRules.isKeepGameGoing(stack.getItem()));
            }

            if (thiefAlive && hasKeepGameGoingItem) {
                break;
            }
        }
        
        // Single thief alive with no other players - thief wins
        if (alivePlayers.size() == 1 && thiefAlive) {
            CustomWinnerComponent nrwc = CustomWinnerComponent.KEY.get(serverLevel);
            nrwc.setWinningTextId(SERoles.THIEF.identifier().getPath());
            nrwc.setWinners(List.of((Player) alivePlayers.getFirst()));
            nrwc.setColor(SERoles.THIEF.color());
            nrwc.sync();
            
            GameRoundEndComponent.KEY.get(serverLevel)
                .setRoundEndData(serverLevel.players(), GameFunctions.WinStatus.KILLERS);
            GameFunctions.stopGame(serverLevel);
            ci.cancel();
            return;
        }

        // Thief alive with keep-game-going item - prevent game from ending
        if (thiefAlive && hasKeepGameGoingItem) {
            ci.cancel();
        }
    }
}