package pro.fazeclan.river.stupid_express.mixin.role.avaricious;

import dev.doctor4t.wathe.cca.GameTimeComponent;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.gamemode.MurderGameMode;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pro.fazeclan.river.stupid_express.constants.SERoles;
import pro.fazeclan.river.stupid_express.role.avaricious.AvariciousGoldHandler;

@Mixin(MurderGameMode.class)
public class AvariciousTimer {

    @Inject(
            method = "tickServerGameLoop",
            at = @At("TAIL")
    )
    private void actionbar(
            ServerLevel serverWorld, GameWorldComponent gameWorldComponent, CallbackInfo ci
    ) {

        // Optional: stop when game ends
        if (gameWorldComponent.getGameStatus() != GameWorldComponent.GameStatus.ACTIVE)
            return;

        GameTimeComponent timeComponent = GameTimeComponent.KEY.get(serverWorld);
        long time = timeComponent.time;

        int remainder = (int) (time % AvariciousGoldHandler.TIMER_TICKS);
        int ticksRemaining = AvariciousGoldHandler.TIMER_TICKS - remainder;

        // Update once per second
        if (time % 20 != 0) return;

        for (ServerPlayer player : serverWorld.players()) {

            if (!gameWorldComponent.isRole(player, SERoles.AVARICIOUS)) continue;

            player.sendSystemMessage(
                    Component.translatable(
                            "hud.stupid_express.avaricious.payout_timer",
                            (60 - (ticksRemaining / 20))
                    ).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    true
            );
        }
    }

}
