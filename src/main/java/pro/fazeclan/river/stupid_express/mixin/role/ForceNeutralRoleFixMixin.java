package pro.fazeclan.river.stupid_express.mixin.role;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.agmas.harpymodloader.Harpymodloader;
import org.agmas.harpymodloader.config.HarpyModLoaderConfig;
import org.agmas.harpymodloader.modded_murder.ModdedMurderGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(ModdedMurderGameMode.class)
public class ForceNeutralRoleFixMixin {

    @Inject(
        method = "assignCivilianReplacingRoles",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Collections;shuffle(Ljava/util/List;)V",
            ordinal = 1,
            shift = At.Shift.AFTER
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void assignForcedNeutralRoles(
            int desiredRoleCount,
            ServerLevel serverLevel,
            GameWorldComponent gameWorldComponent,
            List<ServerPlayer> players,
            CallbackInfo ci,
            ArrayList<Role> shuffledCivilianRoles,
            ArrayList<Role> shuffledNeutralRoles,
            ArrayList<ServerPlayer> playersForCivilianRoles) {
        
        List<Role> neutralRolesToProcess = new ArrayList<>(shuffledNeutralRoles);
        
        for (Role role : neutralRolesToProcess) {
            if (Harpymodloader.FORCED_MODDED_ROLE.containsKey(role) && 
                !((HarpyModLoaderConfig)HarpyModLoaderConfig.HANDLER.instance()).disabled.contains(role.identifier().toString())) {
                
                List<UUID> forcedUUIDs = Harpymodloader.FORCED_MODDED_ROLE.get(role);
                
                List<ServerPlayer> forcedPlayers = new ArrayList<>();
                for (ServerPlayer player : playersForCivilianRoles) {
                    if (forcedUUIDs.contains(player.getUUID()) && 
                        Harpymodloader.OVERWRITE_ROLES.contains(gameWorldComponent.getRole(player))) {
                        forcedPlayers.add(player);
                    }
                }
                
                if (!forcedPlayers.isEmpty()) {
                    invokeFindAndAssignPlayers(
                        forcedPlayers.size(),
                        role,
                        forcedPlayers,
                        gameWorldComponent,
                        serverLevel
                    );
                    
                    playersForCivilianRoles.removeAll(forcedPlayers);
                    
                    shuffledNeutralRoles.remove(role);
                }
            }
        }
    }

    @Invoker("findAndAssignPlayers")
    static int invokeFindAndAssignPlayers(int desiredRoleCount, Role role, List<ServerPlayer> players, 
                                          GameWorldComponent gameWorldComponent, Level level) {
        throw new AssertionError();
    }
}