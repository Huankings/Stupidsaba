package pro.fazeclan.river.stupid_express.mixin.role.initiate;

import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.agmas.harpymodloader.modded_murder.ModdedMurderGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.constants.SERoles;

import java.util.List;

@Mixin(ModdedMurderGameMode.class)
public class InitiateAssignMixin {

    @Inject(
            method = "assignCivilianReplacingRoles",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/ArrayList;removeIf(Ljava/util/function/Predicate;)Z",
                    ordinal = 4
            )
    )
    private void assignSecondInitiate(
            int desiredRoleCount,
            ServerLevel serverWorld,
            GameWorldComponent gameWorldComponent,
            List<ServerPlayer> players,
            CallbackInfo ci
    ) {
        boolean hasInitiate = players.stream().anyMatch(p -> gameWorldComponent.isRole(p, SERoles.INITIATE));

        if (!hasInitiate) {
            return;
        }

        for (ServerPlayer player : players) {
            if (!gameWorldComponent.isInnocent(player)
                    && !gameWorldComponent.canUseKillerFeatures(player)
                    && !gameWorldComponent.isRole(player, SERoles.INITIATE)) {
                gameWorldComponent.addRole(player, SERoles.INITIATE);

                for (ItemStack item : player.getInventory().items) {
                    if (item.is(WatheItems.KEY) || item.is(WatheItems.LETTER)) {
                        continue;
                    }

                    player.getInventory().removeItem(item);
                }

                Log.info(LogCategory.GENERAL, player.getScoreboardName() + " -> " + SERoles.INITIATE.identifier().toString());
                break;
            }
        }
    }

}
