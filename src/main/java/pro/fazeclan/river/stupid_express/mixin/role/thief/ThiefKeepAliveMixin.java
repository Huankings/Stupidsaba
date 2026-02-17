package pro.fazeclan.river.stupid_express.mixin.role.thief;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.game.gamemode.MurderGameMode;
import dev.doctor4t.wathe.cca.GameRoundEndComponent;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.role.thief.ThiefItemTracker;
import pro.fazeclan.river.stupid_express.cca.CustomWinnerComponent;
import java.util.List;

@Mixin(MurderGameMode.class)
public class ThiefKeepAliveMixin {

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
    private void keepAlive(
            ServerLevel serverLevel, 
            GameWorldComponent gameWorldComponent, 
            CallbackInfo ci, 
            @Local(name = "winStatus") GameFunctions.WinStatus winStatus
    ) {
        List<ServerPlayer> alivePlayers = serverLevel.getPlayers(GameFunctions::isPlayerAliveAndSurvival);
        
        boolean thiefAlive = false;

        for (ServerPlayer player : alivePlayers) {
            if (gameWorldComponent.isRole(player, SERoles.THIEF)) {
                thiefAlive = true;
                break;
            }
        }
        
        // Check if Thief is the last one standing
        if (alivePlayers.size() == 1 && thiefAlive) {
            CustomWinnerComponent nrwc = CustomWinnerComponent.KEY.get(serverLevel);
            nrwc.setWinningTextId(SERoles.THIEF.identifier().getPath());
            nrwc.setWinners(List.of((Player) alivePlayers.getFirst()));
            nrwc.setColor(SERoles.THIEF.color());
            nrwc.sync();
            GameRoundEndComponent.KEY.get(serverLevel).setRoundEndData(serverLevel.players(), GameFunctions.WinStatus.KILLERS);
            
            GameFunctions.stopGame(serverLevel);
        }

        // Keep game going - check for if thief is alive and there are available items from ThiefItemRules.isKeepGameGoing()
        if (thiefAlive && ThiefItemTracker.isWeaponAvailable() && 
            (winStatus == GameFunctions.WinStatus.KILLERS || winStatus == GameFunctions.WinStatus.PASSENGERS)) {
            ci.cancel();
        }
    }
}