package pro.fazeclan.river.stupid_express.mixin.role.thief;

import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.role.thief.ThiefItemTracker;

@Mixin(GameFunctions.class)
public class ThiefTrackKillPlayerMixin {

    @Inject(
        method = "killPlayer(Lnet/minecraft/world/entity/player/Player;ZLnet/minecraft/world/entity/player/Player;Lnet/minecraft/resources/ResourceLocation;)V",
        at = @At("RETURN")
    )
    private static void afterKillPlayer(
        Player victim, 
        boolean spawnBody, 
        @Nullable Player killer, 
        ResourceLocation deathReason, 
        CallbackInfo ci
    ) {
        if (victim instanceof ServerPlayer serverVictim) {
            ThiefItemTracker.onKillPlayer(serverVictim);
        }
    }
}